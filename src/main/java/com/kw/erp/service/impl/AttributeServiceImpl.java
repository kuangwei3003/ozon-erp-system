package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.attr.AttributeReq;
import com.kw.erp.client.dto.attr.AttributeValuesReq;
import com.kw.erp.client.dto.attr.ProductAttributesReq;
import com.kw.erp.entity.ProductAttribute;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ProductAttributeMapper;
import com.kw.erp.service.AttributeService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 属性与特征数据实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private static final int ATTR_PAGE_LIMIT = 100;

    private static final int MAX_PAGES = 50;

    private final OzonApiClient client;
    private final ProductAttributeMapper attributeMapper;

    @Override
    public JsonNode categoryTree() {
        return client.categoryTree();
    }

    @Override
    public JsonNode categoryAttributes(AttributeReq req) {
        if (req == null || req.getDescriptionCategoryId() == null || req.getTypeId() == null) {
            throw new DomainException("请先选择类目（description_category_id 与 type_id）");
        }
        return client.categoryAttribute(req);
    }

    @Override
    public JsonNode attributeValues(AttributeValuesReq req) {
        if (req == null || req.getAttrId() == null) {
            throw new DomainException("属性 ID 不能为空");
        }
        return client.attributeValues(req);
    }

    @Override
    public int syncProductAttributes(List<Long> skus) {
        ProductAttributesReq req = new ProductAttributesReq();
        ProductAttributesReq.Filter filter = new ProductAttributesReq.Filter();
        if (!CollectionUtils.isEmpty(skus)) {
            filter.setSku(skus);
        }
        req.setFilter(filter);
        req.setLimit(ATTR_PAGE_LIMIT);

        int saved = 0;
        int lastId = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            req.setLastId(lastId);
            JsonNode resp = client.productAttributes(req);
            JsonNode items = JsonUtils.path(resp, "result", "items");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                saved += saveOne(item);
            }
            Integer next = JsonUtils.intVal(JsonUtils.path(resp, "result"), "last_id");
            if (next == null || next == lastId) {
                break;
            }
            lastId = next;
        }
        log.info("商品属性同步完成，写入 {} 条", saved);
        return saved;
    }

    /** 一个商品一条记录，重复同步时先删后插 */
    private int saveOne(JsonNode item) {
        Long productId = JsonUtils.longVal(item, "product_id", "id");
        Long sku = JsonUtils.longVal(item, "sku");
        String offerId = JsonUtils.text(item, "offer_id");

        LambdaQueryWrapper<ProductAttribute> delete = new LambdaQueryWrapper<>();
        if (productId != null) {
            delete.eq(ProductAttribute::getProductId, productId);
        } else if (sku != null) {
            delete.eq(ProductAttribute::getSku, sku);
        } else {
            return 0;
        }
        attributeMapper.delete(delete);

        int count = 0;
        for (JsonNode attr : JsonUtils.elements(item.path("attributes"))) {
            ProductAttribute entity = new ProductAttribute();
            entity.setProductId(productId);
            entity.setSku(sku);
            entity.setOfferId(offerId);
            entity.setAttributeId(JsonUtils.longVal(attr, "id", "attribute_id"));
            entity.setAttrName(JsonUtils.text(attr, "name", "attribute_name"));
            entity.setComplexId(JsonUtils.longVal(attr, "complex_id"));
            entity.setValueJson(attr.path("values").isArray() ? attr.path("values").toString() : attr.toString());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            attributeMapper.insert(entity);
            count++;
        }
        return count;
    }

    @Override
    public List<ProductAttribute> attributesOfProduct(Long productIdOrSku) {
        if (productIdOrSku == null) {
            return List.of();
        }
        LambdaQueryWrapper<ProductAttribute> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ProductAttribute::getProductId, productIdOrSku)
                .or().eq(ProductAttribute::getSku, productIdOrSku))
                .orderByAsc(ProductAttribute::getAttributeId);
        return attributeMapper.selectList(wrapper);
    }
}
