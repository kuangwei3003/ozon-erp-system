package com.kw.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.product.ImportProductReq;
import com.kw.erp.client.dto.product.ProductInfoReq;
import com.kw.erp.client.dto.product.ProductListReq;
import com.kw.erp.client.dto.product.ProductListResp;
import com.kw.erp.entity.Product;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品服务逻辑测试：同步落库、详情补全、上传参数校验
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private OzonApiClient client;
    @Mock
    private ProductMapper productMapper;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(client, productMapper);
    }

    private ProductListResp listResp(String lastId, ProductListResp.Item... items) {
        ProductListResp resp = new ProductListResp();
        ProductListResp.Result result = new ProductListResp.Result();
        result.setItems(List.of(items));
        result.setLastId(lastId);
        resp.setResult(result);
        return resp;
    }

    private ProductListResp.Item item(long productId, String offerId, String status) {
        ProductListResp.Item item = new ProductListResp.Item();
        item.setId(productId);
        item.setOfferId(offerId);
        item.setName("SKU-" + offerId);
        ProductListResp.ProductStatus st = new ProductListResp.ProductStatus();
        st.setStatus(status);
        item.setStatus(st);
        return item;
    }

    @Test
    void syncInsertsNewProductsAndEnrichesFromInfo() throws Exception {
        when(client.productList(any(ProductListReq.class)))
                .thenReturn(listResp("", item(100L, "A-1", "purchase_available")));
        when(productMapper.selectList(any())).thenReturn(List.of());
        when(client.productInfoList(any(ProductInfoReq.class))).thenReturn(MAPPER.readTree("""
                {"items":[
                  {"id":100,"offer_id":"A-1","sku":999,"name":"SKU-A-1",
                   "brand":{"name":"BrandX"},
                   "category":{"type_id":91951,"parent_id":17028920},
                   "visibility_details":{"status":"visible","has_stock":true},
                   "content":{"images":["https://img/1.jpg"]}}
                ]}
                """));

        assertEquals(1, service.sync());

        ArgumentCaptor<Product> inserted = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(inserted.capture());
        assertEquals("A-1", inserted.getValue().getOfferId());
        assertEquals("purchase_available", inserted.getValue().getStatus());

        ArgumentCaptor<Product> updated = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(updated.capture());
        // 同一次同步里列表与详情操作同一个实例，最终落库的 sku 以详情接口为准
        assertEquals(inserted.getValue(), updated.getValue());
        Product enriched = updated.getValue();
        assertEquals(999L, enriched.getSku().longValue());
        assertEquals("BrandX", enriched.getBrand());
        assertEquals(91951L, enriched.getTypeId().longValue());
        assertEquals(17028920L, enriched.getCategoryId().longValue());
        assertEquals("visible", enriched.getVisibility());
        assertEquals(1, enriched.getHasStock().intValue());
        assertEquals("https://img/1.jpg", enriched.getPrimaryImage());
    }

    @Test
    void syncUpdatesExistingProductInsteadOfInsert() throws Exception {
        Product existing = new Product();
        existing.setId(5L);
        existing.setSku(100L);
        existing.setOfferId("A-1");
        existing.setPrimaryImage("https://old/1.jpg");
        existing.setBrand("BrandX");

        when(client.productList(any(ProductListReq.class)))
                .thenReturn(listResp("", item(100L, "A-1", "expired")));
        when(productMapper.selectList(any())).thenReturn(List.of(existing));
        when(client.productInfoList(any(ProductInfoReq.class))).thenReturn(MAPPER.readTree("{\"items\":[]}"));

        assertEquals(1, service.sync());

        verify(productMapper, never()).insert(any(Product.class));
        ArgumentCaptor<Product> updated = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(updated.capture());
        assertEquals(5L, updated.getValue().getId().longValue());
        assertEquals("expired", updated.getValue().getStatus());
        // 详情接口没返回时保留已有的图片与品牌
        assertEquals("https://old/1.jpg", updated.getValue().getPrimaryImage());
        assertEquals("BrandX", updated.getValue().getBrand());
    }

    @Test
    void syncSkipsEnrichmentWhenDetailCallFails() {
        when(client.productList(any(ProductListReq.class)))
                .thenReturn(listResp("", item(100L, "A-1", "purchase_available")));
        when(productMapper.selectList(any())).thenReturn(List.of());
        when(client.productInfoList(any(ProductInfoReq.class))).thenThrow(new RuntimeException("503"));

        // 详情接口失败不能让整次同步失败，基础数据仍要落库
        assertEquals(1, service.sync());
        verify(productMapper).insert(any(Product.class));
        verify(productMapper, never()).updateById(any(Product.class));
    }

    @Test
    void syncReturnsZeroForEmptyShop() {
        when(client.productList(any(ProductListReq.class))).thenReturn(listResp("", new ProductListResp.Item[0]));

        assertEquals(0, service.sync());
        verify(productMapper, never()).insert(any(Product.class));
        verify(client, never()).productInfoList(any());
    }

    /** 游标不前进时必须终止翻页，否则同步会死循环 */
    @Test
    void syncStopsWhenCursorDoesNotAdvance() {
        ProductListResp page = listResp("100", item(1L, "A", "purchase_available"));
        when(client.productList(any(ProductListReq.class))).thenReturn(page, page);
        when(productMapper.selectList(any())).thenReturn(List.of());

        assertEquals(2, service.sync());

        ArgumentCaptor<ProductListReq> req = ArgumentCaptor.forClass(ProductListReq.class);
        verify(client, times(2)).productList(req.capture());
        assertEquals("", req.getAllValues().get(0).getLastId());
        assertEquals("100", req.getAllValues().get(1).getLastId());
    }

    @Test
    void uploadRejectsInvalidItems() {
        assertThrows(DomainException.class, () -> service.upload(null));

        ImportProductReq empty = new ImportProductReq();
        empty.setItems(List.of());
        assertThrows(DomainException.class, () -> service.upload(empty));

        ImportProductReq noOffer = new ImportProductReq();
        ImportProductReq.ImportItem item = new ImportProductReq.ImportItem();
        item.setName("name");
        noOffer.setItems(List.of(item));
        DomainException ex = assertThrows(DomainException.class, () -> service.upload(noOffer));
        assertEquals("商品的 offer_id（卖家货号）不能为空", ex.getMessage());

        ImportProductReq noName = new ImportProductReq();
        ImportProductReq.ImportItem item2 = new ImportProductReq.ImportItem();
        item2.setOfferId("A-1");
        noName.setItems(List.of(item2));
        DomainException ex2 = assertThrows(DomainException.class, () -> service.upload(noName));
        assertEquals("商品 \"A-1\" 的名称不能为空", ex2.getMessage());

        verify(client, never()).productImport(any());
    }

    @Test
    void uploadPassesValidRequestToClient() {
        ImportProductReq req = new ImportProductReq();
        ImportProductReq.ImportItem item = new ImportProductReq.ImportItem();
        item.setOfferId("A-1");
        item.setName("name");
        req.setItems(List.of(item));
        JsonNode resp = mock(JsonNode.class);
        when(client.productImport(req)).thenReturn(resp);

        assertEquals(resp, service.upload(req));
    }

    @Test
    void detailFailsWhenProductMissing() {
        when(productMapper.selectById(1L)).thenReturn(null);
        DomainException ex = assertThrows(DomainException.class, () -> service.detail(1L));
        assertEquals("商品不存在，请先同步商品数据", ex.getMessage());
    }

    @Test
    void uploadStatusRejectsNullTask() {
        assertThrows(DomainException.class, () -> service.uploadStatus(null));
        verify(client, never()).productImportInfo(any());
    }
}
