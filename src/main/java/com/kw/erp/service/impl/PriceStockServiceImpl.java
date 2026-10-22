package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.price.PriceImportReq;
import com.kw.erp.client.dto.price.PriceListReq;
import com.kw.erp.client.dto.stock.StockInfoReq;
import com.kw.erp.client.dto.stock.StocksReq;
import com.kw.erp.entity.PriceStock;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.PriceStockMapper;
import com.kw.erp.service.PriceStockService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 价格与库存实现：游标翻页同步 + 本地分页 + 批量更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceStockServiceImpl implements PriceStockService {

    private static final int SYNC_LIMIT = 100;

    /** 同步翻页上限，避免异常游标导致死循环 */
    private static final int MAX_PAGES = 100;

    private final OzonApiClient client;
    private final PriceStockMapper priceStockMapper;

    @Override
    public int sync() {
        Map<Long, PriceStock> bySku = new HashMap<>();
        Map<String, PriceStock> byOffer = new HashMap<>();
        loadExisting(bySku, byOffer);

        int affected = syncPrices(bySku, byOffer);
        affected += syncStocks(bySku, byOffer);
        log.info("价格库存同步完成，影响 {} 行", affected);
        return affected;
    }

    private int syncPrices(Map<Long, PriceStock> bySku, Map<String, PriceStock> byOffer) {
        int affected = 0;
        String cursor = "";
        for (int page = 0; page < MAX_PAGES; page++) {
            PriceListReq req = new PriceListReq();
            req.setFilter(new PriceListReq.Filter());
            req.setLimit(SYNC_LIMIT);
            req.setCursor(cursor);
            JsonNode resp = client.priceList(req);
            var items = JsonUtils.elements(resp == null ? null : resp.path("items"));
            if (items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                PriceStock entity = parsePrice(item);
                if (entity == null) {
                    continue;
                }
                save(entity, bySku, byOffer);
                affected++;
            }
            String next = JsonUtils.text(resp, "cursor");
            if (!StringUtils.hasText(next) || next.equals(cursor)) {
                break;
            }
            cursor = next;
        }
        return affected;
    }

    private int syncStocks(Map<Long, PriceStock> bySku, Map<String, PriceStock> byOffer) {
        int affected = 0;
        String cursor = "";
        for (int page = 0; page < MAX_PAGES; page++) {
            StockInfoReq req = new StockInfoReq();
            req.setFilter(new StockInfoReq.Filter());
            req.setLimit(SYNC_LIMIT);
            req.setCursor(cursor);
            JsonNode resp;
            try {
                resp = client.stocksInfo(req);
            } catch (RuntimeException e) {
                log.warn("库存拉取失败，跳过：{}", e.getMessage());
                break;
            }
            JsonNode itemsNode = JsonUtils.path(resp, "result", "items");
            if (!itemsNode.isArray() && resp != null) {
                itemsNode = resp.path("items");
            }
            var items = JsonUtils.elements(itemsNode);
            if (items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                affected += parseStocks(item, bySku, byOffer);
            }
            String next = JsonUtils.text(JsonUtils.path(resp, "result"), "cursor");
            if (next == null && resp != null) {
                next = JsonUtils.text(resp, "cursor");
            }
            if (!StringUtils.hasText(next) || next.equals(cursor)) {
                break;
            }
            cursor = next;
        }
        return affected;
    }

    private PriceStock parsePrice(JsonNode item) {
        Long sku = JsonUtils.longVal(item, "sku", "product_id");
        String offerId = JsonUtils.text(item, "offer_id");
        if (sku == null && offerId == null) {
            return null;
        }
        PriceStock entity = new PriceStock();
        entity.setSku(sku);
        entity.setOfferId(offerId == null ? JsonUtils.text(JsonUtils.path(item, "stocks"), "offer_id") : offerId);
        JsonNode price = JsonUtils.path(item, "price");
        entity.setPrice(JsonUtils.text(price, "price"));
        entity.setOldPrice(JsonUtils.text(price, "old_price"));
        entity.setMinPrice(JsonUtils.text(price, "min_price"));
        entity.setMarketPrice(JsonUtils.text(price, "market_price"));
        entity.setCurrencyCode(JsonUtils.text(price, "currency_code"));
        entity.setPriceIndex(JsonUtils.text(JsonUtils.path(item, "price_indexes"), "current_index"));
        entity.setPricesUpdatedAt(LocalDateTime.now());
        entity.setSyncedAt(LocalDateTime.now());
        return entity;
    }

    /** 一个 SKU 可能对应多个仓库，按 sku+warehouse 组合行保存 */
    private int parseStocks(JsonNode item, Map<Long, PriceStock> bySku, Map<String, PriceStock> byOffer) {
        Long sku = JsonUtils.longVal(item, "sku", "product_id");
        String offerId = JsonUtils.text(item, "offer_id");
        int count = 0;
        var stocks = JsonUtils.elements(item.path("stocks"));
        if (stocks.isEmpty()) {
            PriceStock entity = newPriceStockRow(sku, offerId, item);
            save(entity, bySku, byOffer);
            return 1;
        }
        for (JsonNode stock : stocks) {
            PriceStock entity = newPriceStockRow(sku, offerId, item);
            entity.setWarehouseId(JsonUtils.longVal(stock, "warehouse_id", "office_id"));
            entity.setWarehouseName(JsonUtils.text(stock, "warehouse_name", "name"));
            entity.setStock(JsonUtils.intVal(stock, "quantity", "stock"));
            entity.setReserved(JsonUtils.intVal(stock, "reserve", "reserved"));
            entity.setIncoming(JsonUtils.intVal(stock, "incoming", "come_when"));
            entity.setStocksUpdatedAt(LocalDateTime.now());
            save(entity, bySku, byOffer);
            count++;
        }
        return count;
    }

    private PriceStock newPriceStockRow(Long sku, String offerId, JsonNode item) {
        PriceStock entity = new PriceStock();
        entity.setSku(sku);
        entity.setOfferId(offerId);
        entity.setSyncedAt(LocalDateTime.now());
        return entity;
    }

    /** 优先按 sku 匹配已有行；无 sku 时按 offer_id */
    private void save(PriceStock entity, Map<Long, PriceStock> bySku, Map<String, PriceStock> byOffer) {
        PriceStock existing = entity.getSku() != null ? bySku.get(entity.getSku()) : null;
        if (existing == null && entity.getOfferId() != null) {
            existing = byOffer.get(entity.getOfferId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            if (entity.getStock() == null) {
                entity.setStock(0);
            }
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            priceStockMapper.insert(entity);
        } else {
            entity.setId(existing.getId());
            // 价格与库存来自两次不同的拉取，缺省值用已有数据补齐
            if (entity.getPrice() == null) {
                entity.setPrice(existing.getPrice());
                entity.setOldPrice(existing.getOldPrice());
                entity.setMinPrice(existing.getMinPrice());
                entity.setMarketPrice(existing.getMarketPrice());
                entity.setCurrencyCode(existing.getCurrencyCode());
                entity.setPriceIndex(existing.getPriceIndex());
                entity.setPricesUpdatedAt(existing.getPricesUpdatedAt());
            }
            if (entity.getStock() == null) {
                entity.setStock(existing.getStock());
                entity.setReserved(existing.getReserved());
                entity.setIncoming(existing.getIncoming());
                entity.setWarehouseId(existing.getWarehouseId());
                entity.setWarehouseName(existing.getWarehouseName());
                entity.setStocksUpdatedAt(existing.getStocksUpdatedAt());
            }
            if (entity.getOfferId() == null) {
                entity.setOfferId(existing.getOfferId());
            }
            entity.setUpdatedAt(now);
            priceStockMapper.updateById(entity);
        }
        if (entity.getSku() != null) {
            bySku.put(entity.getSku(), entity);
        }
        if (entity.getOfferId() != null) {
            byOffer.put(entity.getOfferId(), entity);
        }
    }

    private void loadExisting(Map<Long, PriceStock> bySku, Map<String, PriceStock> byOffer) {
        for (PriceStock ps : priceStockMapper.selectList(null)) {
            if (ps.getSku() != null) {
                bySku.putIfAbsent(ps.getSku(), ps);
            }
            if (ps.getOfferId() != null) {
                byOffer.putIfAbsent(ps.getOfferId(), ps);
            }
        }
    }

    @Override
    public IPage<PriceStock> page(long current, long size, String keyword) {
        LambdaQueryWrapper<PriceStock> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            Long skuKeyword = parseLongOrNull(keyword);
            wrapper.and(w -> {
                w.like(PriceStock::getOfferId, keyword)
                        .or().like(PriceStock::getWarehouseName, keyword);
                if (skuKeyword != null) {
                    w.or().eq(PriceStock::getSku, skuKeyword);
                }
            });
        }
        wrapper.orderByDesc(PriceStock::getId);
        return priceStockMapper.selectPage(new Page<>(current, size), wrapper);
    }

    private static Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public JsonNode updatePrices(PriceImportReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getPrices())) {
            throw new DomainException("请至少填写一条价格");
        }
        req.getPrices().forEach(p -> {
            if (!StringUtils.hasText(p.getPrice())) {
                throw new DomainException("商品 \"" + p.getOfferId() + "\" 的价格不能为空");
            }
            if (p.getOfferId() == null && p.getProductId() == null) {
                throw new DomainException("价格记录必须包含 offer_id 或 product_id");
            }
        });
        JsonNode resp = client.pricesImport(req);
        sync();
        return resp;
    }

    @Override
    public JsonNode updateStocks(StocksReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getStocks())) {
            throw new DomainException("请至少填写一条库存");
        }
        req.getStocks().forEach(s -> {
            if (s.getStock() == null || s.getStock() < 0) {
                throw new DomainException("商品 \"" + s.getOfferId() + "\" 的库存数量不合法");
            }
            if (s.getSku() == null && s.getOfferId() == null) {
                throw new DomainException("库存记录必须包含 sku 或 offer_id");
            }
        });
        JsonNode resp = client.stocksUpdate(req);
        sync();
        return resp;
    }
}
