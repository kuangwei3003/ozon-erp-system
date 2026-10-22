package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.returnn.ReturnListReq;
import com.kw.erp.entity.ReturnItem;
import com.kw.erp.mapper.ReturnItemMapper;
import com.kw.erp.service.ReturnService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 退货信息实现：分页拉取 Ozon 退货数据并落地本地表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private static final int PAGE_SIZE = 100;

    private static final int MAX_PAGES = 50;

    private final OzonApiClient client;
    private final ReturnItemMapper returnMapper;

    @Override
    public int sync() {
        Map<String, ReturnItem> existing = loadExisting();
        int count = 0;
        int page = 1;
        for (int i = 0; i < MAX_PAGES; i++, page++) {
            ReturnListReq req = new ReturnListReq();
            req.setPage(page);
            req.setPageSize(PAGE_SIZE);
            JsonNode resp = client.returnList(req);
            List<JsonNode> items = extractReturns(resp);
            if (items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                ReturnItem entity = parse(item);
                if (entity == null) {
                    continue;
                }
                save(entity, existing);
                count++;
            }
            if (items.size() < PAGE_SIZE) {
                break;
            }
        }
        log.info("退货同步完成，共 {} 条", count);
        return count;
    }

    /** 兼容 {returns:[...]} / {result:{returns:[...]}} / [...] 三种报文结构 */
    private List<JsonNode> extractReturns(JsonNode resp) {
        if (resp == null) {
            return List.of();
        }
        JsonNode node = resp.path("returns");
        if (!node.isArray()) {
            node = JsonUtils.path(resp, "result", "returns");
        }
        if (!node.isArray()) {
            node = JsonUtils.path(resp, "result", "items");
        }
        if (!node.isArray() && resp.isArray()) {
            node = resp;
        }
        return JsonUtils.elements(node);
    }

    private ReturnItem parse(JsonNode item) {
        Long returnId = JsonUtils.longVal(item, "id", "return_id");
        String postingNumber = JsonUtils.text(item, "posting_number", "posting");
        if (returnId == null && postingNumber == null) {
            return null;
        }
        ReturnItem entity = new ReturnItem();
        entity.setReturnId(returnId);
        entity.setPostingNumber(postingNumber);
        entity.setSku(JsonUtils.longVal(JsonUtils.path(item, "product"), "sku", "origin_id"));
        entity.setOfferId(JsonUtils.text(JsonUtils.path(item, "product"), "offer_id", "sku_code"));
        entity.setProductName(JsonUtils.text(JsonUtils.path(item, "product"), "name", "product_name"));
        entity.setStatus(JsonUtils.text(item, "status", "return_status"));
        entity.setReturnMode(JsonUtils.text(item, "return_mode", "scheme"));
        entity.setReasonType(JsonUtils.text(JsonUtils.path(item, "reason"), "reason_type", "id"));
        entity.setCustomerName(JsonUtils.text(JsonUtils.path(item, "customer"), "name", "fio"));
        JsonNode dates = JsonUtils.path(item, "dates");
        entity.setAwaitingDelivDeadline(JsonUtils.parseDate(item, "awaiting_deliv_deadline"));
        entity.setDecisionDate(JsonUtils.parseDate(dates, "decision_date"));
        entity.setReturnDate(JsonUtils.parseDate(dates, "return_date", "last_modified"));
        entity.setRebateDate(JsonUtils.parseDate(dates, "rebate_date"));
        entity.setRawJson(item.toString());
        entity.setSyncedAt(LocalDateTime.now());
        return entity;
    }

    private void save(ReturnItem entity, Map<String, ReturnItem> existing) {
        String key = keyOf(entity);
        ReturnItem old = existing.get(key);
        LocalDateTime now = LocalDateTime.now();
        if (old == null) {
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            returnMapper.insert(entity);
            existing.put(key, entity);
        } else {
            entity.setId(old.getId());
            entity.setUpdatedAt(now);
            returnMapper.updateById(entity);
        }
    }

    private String keyOf(ReturnItem entity) {
        if (entity.getReturnId() != null) {
            return "r" + entity.getReturnId();
        }
        return "p" + entity.getPostingNumber() + "#" + entity.getSku();
    }

    private Map<String, ReturnItem> loadExisting() {
        Map<String, ReturnItem> map = new HashMap<>();
        for (ReturnItem item : returnMapper.selectList(null)) {
            map.put(keyOf(item), item);
        }
        return map;
    }

    @Override
    public IPage<ReturnItem> page(long current, long size, String status, String keyword) {
        LambdaQueryWrapper<ReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), ReturnItem::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ReturnItem::getPostingNumber, keyword)
                    .or().like(ReturnItem::getOfferId, keyword)
                    .or().like(ReturnItem::getProductName, keyword));
        }
        wrapper.orderByDesc(ReturnItem::getId);
        return returnMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
