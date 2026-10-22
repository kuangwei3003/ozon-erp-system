package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.product.ImportProductReq;
import com.kw.erp.client.dto.product.ImportStatusReq;
import com.kw.erp.client.dto.product.ProductInfoReq;
import com.kw.erp.client.dto.product.ProductListReq;
import com.kw.erp.client.dto.product.ProductListResp;
import com.kw.erp.entity.Product;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ProductMapper;
import com.kw.erp.service.ProductService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品管理实现：列表同步 + 详情补全 + 上传/更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** 单页拉取数量 */
    private static final int LIST_LIMIT = 1000;

    /** 最多拉取页数，防止误配密钥时无限翻页 */
    private static final int MAX_PAGES = 20;

    /** 详情批量查询分片大小 */
    private static final int INFO_CHUNK = 100;

    private final OzonApiClient client;
    private final ProductMapper productMapper;

    @Override
    public int sync() {
        List<ProductListResp.Item> listed = fetchAllFromList();
        if (listed.isEmpty()) {
            return 0;
        }
        Map<String, Product> byOffer = new HashMap<>();
        Map<Long, Product> bySku = new HashMap<>();
        loadExisting(byOffer, bySku);

        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        // 先用列表结果落一份基础数据
        for (ProductListResp.Item item : listed) {
            Product entity = fromListItem(item, now);
            upsert(entity, byOffer, bySku);
            count++;
        }
        // 再用详情接口补全 sku / 品牌 / 类目 / 图片
        enrichFromInfo(listed, byOffer, bySku, now);
        log.info("商品同步完成，共 {} 条", count);
        return count;
    }

    private List<ProductListResp.Item> fetchAllFromList() {
        List<ProductListResp.Item> all = new ArrayList<>();
        String lastId = "";
        for (int i = 0; i < MAX_PAGES; i++) {
            ProductListReq req = new ProductListReq();
            req.setFilter(new ProductListReq.Filter());
            req.setLimit(LIST_LIMIT);
            req.setLastId(lastId);
            ProductListResp resp = client.productList(req);
            if (resp == null || resp.getResult() == null || CollectionUtils.isEmpty(resp.getResult().getItems())) {
                break;
            }
            all.addAll(resp.getResult().getItems());
            String next = resp.getResult().getLastId();
            if (!StringUtils.hasText(next) || next.equals(lastId)) {
                break;
            }
            lastId = next;
        }
        return all;
    }

    private void enrichFromInfo(List<ProductListResp.Item> listed, Map<String, Product> byOffer,
                                Map<Long, Product> bySku, LocalDateTime now) {
        List<Long> ids = listed.stream().map(ProductListResp.Item::getId).filter(id -> id > 0).distinct().toList();
        for (int i = 0; i < ids.size(); i += INFO_CHUNK) {
            List<Long> chunk = ids.subList(i, Math.min(i + INFO_CHUNK, ids.size()));
            ProductInfoReq req = new ProductInfoReq();
            req.setProductId(chunk);
            JsonNode resp;
            try {
                resp = client.productInfoList(req);
            } catch (RuntimeException e) {
                log.warn("商品详情拉取失败，跳过本批次（{} 条）：{}", chunk.size(), e.getMessage());
                continue;
            }
            for (JsonNode item : JsonUtils.elements(resp == null ? null : resp.path("items"))) {
                applyInfo(item, byOffer, bySku, now);
            }
        }
    }

    private void applyInfo(JsonNode item, Map<String, Product> byOffer, Map<Long, Product> bySku, LocalDateTime now) {
        Long productId = JsonUtils.longVal(item, "id", "product_id");
        String offerId = JsonUtils.text(item, "offer_id");
        Product entity = findExisting(productId, offerId, byOffer, bySku);
        if (entity == null) {
            return;
        }
        Long sku = JsonUtils.longVal(item, "sku");
        if (sku != null) {
            entity.setSku(sku);
            bySku.put(sku, entity);
        }
        entity.setName(firstNonBlank(JsonUtils.text(item, "name"), entity.getName()));
        entity.setBrand(JsonUtils.text(JsonUtils.path(item, "brand"), "name"));
        JsonNode category = JsonUtils.path(item, "category");
        entity.setTypeId(JsonUtils.longVal(category, "type_id"));
        entity.setCategoryId(JsonUtils.longVal(category, "parent_id"));
        entity.setVisibility(firstNonBlank(
                JsonUtils.text(JsonUtils.path(item, "visibility_details"), "status"),
                JsonUtils.text(item, "visibility")));
        entity.setHasStock(boolToInt(JsonUtils.path(item, "visibility_details").path("has_stock").asBoolean(false)));
        entity.setPrimaryImage(extractImage(item));
        entity.setRawJson(item.toString());
        entity.setSyncedAt(now);
        entity.setUpdatedAt(now);
        productMapper.updateById(entity);
    }

    private Product findExisting(Long productId, String offerId, Map<String, Product> byOffer, Map<Long, Product> bySku) {
        Product found = offerId == null ? null : byOffer.get(offerId);
        if (found == null && productId != null) {
            found = bySku.get(productId);
        }
        return found;
    }

    private Product fromListItem(ProductListResp.Item item, LocalDateTime now) {
        Product entity = new Product();
        entity.setSku(item.getId());
        entity.setOfferId(item.getOfferId());
        entity.setName(item.getName());
        entity.setStatus(item.getStatus() == null ? null : statusOf(item));
        entity.setSyncedAt(now);
        return entity;
    }

    private String statusOf(ProductListResp.Item item) {
        if (item.getStatus() == null) {
            return null;
        }
        return item.getStatus().getStatus();
    }

    private void upsert(Product entity, Map<String, Product> byOffer, Map<Long, Product> bySku) {
        Product existing = findExisting(entity.getSku(), entity.getOfferId(), byOffer, bySku);
        if (existing == null) {
            entity.setCreatedAt(LocalDateTime.now());
            productMapper.insert(entity);
            track(entity, byOffer, bySku);
        } else {
            entity.setId(existing.getId());
            // 保留详情接口已填充的字段
            entity.setName(firstNonBlank(entity.getName(), existing.getName()));
            entity.setPrimaryImage(existing.getPrimaryImage());
            entity.setBrand(existing.getBrand());
            entity.setTypeId(existing.getTypeId());
            entity.setCategoryId(existing.getCategoryId());
            entity.setVisibility(existing.getVisibility());
            productMapper.updateById(entity);
            track(entity, byOffer, bySku);
        }
    }

    private void track(Product entity, Map<String, Product> byOffer, Map<Long, Product> bySku) {
        if (entity.getOfferId() != null) {
            byOffer.put(entity.getOfferId(), entity);
        }
        if (entity.getSku() != null) {
            bySku.put(entity.getSku(), entity);
        }
    }

    private void loadExisting(Map<String, Product> byOffer, Map<Long, Product> bySku) {
        for (Product p : productMapper.selectList(null)) {
            if (p.getOfferId() != null) {
                byOffer.put(p.getOfferId(), p);
            }
            if (p.getSku() != null) {
                bySku.put(p.getSku(), p);
            }
        }
    }

    /** 图片：兼容 primary_image / content.images[] / images[] */
    private String extractImage(JsonNode item) {
        String direct = JsonUtils.text(item, "primary_image", "image");
        if (direct != null) {
            return direct;
        }
        JsonNode images = JsonUtils.path(item, "content", "images");
        if (images.isArray() && images.size() > 0) {
            return images.get(0).asText();
        }
        images = item.path("images");
        if (images.isArray() && images.size() > 0) {
            JsonNode first = images.get(0);
            return first.isObject() ? JsonUtils.text(first, "file_name", "url", "value") : first.asText();
        }
        return null;
    }

    @Override
    public IPage<Product> page(long current, long size, String keyword, String status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getName, keyword).or().like(Product::getOfferId, keyword));
        }
        wrapper.eq(StringUtils.hasText(status), Product::getStatus, status);
        wrapper.orderByDesc(Product::getId);
        return productMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public Product detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new DomainException("商品不存在，请先同步商品数据");
        }
        return product;
    }

    @Override
    public JsonNode upload(ImportProductReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getItems())) {
            throw new DomainException("请至少填写一个商品");
        }
        req.getItems().forEach(item -> {
            if (!StringUtils.hasText(item.getOfferId())) {
                throw new DomainException("商品的 offer_id（卖家货号）不能为空");
            }
            if (!StringUtils.hasText(item.getName())) {
                throw new DomainException("商品 \"" + item.getOfferId() + "\" 的名称不能为空");
            }
        });
        return client.productImport(req);
    }

    @Override
    public JsonNode uploadStatus(Long taskId) {
        if (taskId == null) {
            throw new DomainException("taskId 不能为空");
        }
        ImportStatusReq req = new ImportStatusReq();
        req.setTaskId(taskId);
        return client.productImportInfo(req);
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    private static Integer boolToInt(boolean value) {
        return value ? 1 : 0;
    }
}
