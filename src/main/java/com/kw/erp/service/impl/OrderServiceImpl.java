package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.order.CancelReasonReq;
import com.kw.erp.client.dto.order.CancelReasonResp;
import com.kw.erp.client.dto.order.CancelReq;
import com.kw.erp.client.dto.order.FbsGetReq;
import com.kw.erp.client.dto.order.FbsListReq;
import com.kw.erp.client.dto.order.FbsListResp;
import com.kw.erp.client.dto.order.FbsPosting;
import com.kw.erp.entity.Posting;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.PostingMapper;
import com.kw.erp.service.OrderService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FBS / rFBS 订单实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int LIST_LIMIT = 100;

    /** 单次同步最多拉取的订单详情数，避免打爆接口配额 */
    private static final int MAX_DETAIL_PER_SYNC = 300;

    /** 默认回溯天数 */
    private static final int DEFAULT_LOOKBACK_DAYS = 90;

    /** 待发货状态（用于运单处理页） */
    private static final List<String> PENDING_STATUSES =
            List.of("awaiting_packaging", "awaiting_deliver", "awaiting_registration");

    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final OzonApiClient client;
    private final PostingMapper postingMapper;

    @Override
    public int sync(String scheme) {
        List<FbsListResp.PostingId> postings = fetchPostingIds(scheme);
        Map<String, Posting> existing = loadExisting();
        int count = 0;
        for (FbsListResp.PostingId pid : postings) {
            if (count >= MAX_DETAIL_PER_SYNC) {
                log.info("已达单次同步上限 {}，剩余订单将在下次同步处理", MAX_DETAIL_PER_SYNC);
                break;
            }
            FbsGetReq getReq = new FbsGetReq();
            getReq.setPostingNumber(pid.getPostingNumber());
            FbsPosting detail;
            try {
                detail = client.fbsGet(getReq);
            } catch (RuntimeException e) {
                log.warn("订单 {} 详情拉取失败：{}", pid.getPostingNumber(), e.getMessage());
                continue;
            }
            Posting entity = toEntity(detail, pid, scheme);
            if (entity != null) {
                save(entity, existing);
                count++;
            }
        }
        log.info("订单同步完成（scheme={}），共 {} 条", scheme, count);
        return count;
    }

    private List<FbsListResp.PostingId> fetchPostingIds(String scheme) {
        FbsListReq req = new FbsListReq();
        FbsListReq.Filter filter = new FbsListReq.Filter();
        if (StringUtils.hasText(scheme)) {
            filter.setScheme(scheme.toLowerCase());
        }
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        filter.setSince(nowUtc.minusDays(DEFAULT_LOOKBACK_DAYS).format(UTC_FORMAT));
        filter.setTo(nowUtc.format(UTC_FORMAT));
        req.setFilter(filter);
        req.setLimit(LIST_LIMIT);
        req.setOffset(0);
        FbsListResp resp = client.fbsList(req);
        if (resp == null || resp.getResult() == null || resp.getResult().getPostings() == null) {
            return List.of();
        }
        return resp.getResult().getPostings();
    }

    private Posting toEntity(FbsPosting detail, FbsListResp.PostingId pid, String scheme) {
        FbsPosting.FbsPostingResult r = detail == null ? null : detail.getResult();
        if (r == null || !StringUtils.hasText(r.getPostingNumber())) {
            return null;
        }
        Posting entity = new Posting();
        entity.setPostingNumber(r.getPostingNumber());
        entity.setOrderId(r.getOrderId() != null ? r.getOrderId() : (pid == null ? null : pid.getOrderId()));
        entity.setScheme(r.getScheme() != null ? r.getScheme() : scheme);
        entity.setStatus(r.getStatus());
        entity.setSubstatus(r.getSubstatus());
        entity.setTrackingNumber(r.getTrackingNumber());
        entity.setDeliveryMethod(r.getDeliveryMethod() == null ? null : r.getDeliveryMethod().getName());
        entity.setWarehouseId(r.getDeliveryMethod() == null ? null : r.getDeliveryMethod().getWarehouseId());
        entity.setCreatedAt(JsonUtils.parseDate(r.getCreatedAt()));
        entity.setShipmentDate(JsonUtils.parseDate(r.getShipmentDate()));
        entity.setShipmentPlanDate(JsonUtils.parseDate(r.getShipmentPlanDate()));
        entity.setDeadlineDate(JsonUtils.parseDate(r.getDeadlineOnAggregator()));
        entity.setAnyAtDelivery(JsonUtils.parseDate(r.getAnyAtDelivery()));
        entity.setUpdatedOnApi(JsonUtils.parseDate(r.getUpdatedOn()));
        entity.setCancelReason(r.getCancel() == null ? null : r.getCancel().getReason());
        entity.setCancelerId(r.getCancel() == null ? null : r.getCancel().getCancelerId());
        entity.setIsMultiPackage(Boolean.TRUE.equals(r.getIsMultiPackage()) ? 1 : 0);
        entity.setProductsJson(JsonUtils.toJson(r.getProducts()));
        entity.setFinancialDataJson(JsonUtils.toJson(r.getFinancialData()));
        entity.setCustomerJson(JsonUtils.toJson(r.getCustomer()));
        entity.setBarcodesJson(JsonUtils.toJson(r.getBarcodes()));
        entity.setSyncedAt(LocalDateTime.now());
        return entity;
    }

    private void save(Posting entity, Map<String, Posting> existing) {
        Posting old = existing.get(entity.getPostingNumber());
        if (old == null) {
            postingMapper.insert(entity);
            existing.put(entity.getPostingNumber(), entity);
        } else {
            entity.setId(old.getId());
            postingMapper.updateById(entity);
        }
    }

    private Map<String, Posting> loadExisting() {
        Map<String, Posting> map = new HashMap<>();
        for (Posting p : postingMapper.selectList(null)) {
            map.put(p.getPostingNumber(), p);
        }
        return map;
    }

    @Override
    public IPage<Posting> page(long current, long size, String status, String scheme, String keyword) {
        LambdaQueryWrapper<Posting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), Posting::getStatus, status);
        wrapper.eq(StringUtils.hasText(scheme), Posting::getScheme, scheme);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Posting::getPostingNumber, keyword).or().like(Posting::getTrackingNumber, keyword));
        }
        wrapper.orderByDesc(Posting::getId);
        return postingMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public Posting detail(String postingNumber) {
        LambdaQueryWrapper<Posting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Posting::getPostingNumber, postingNumber).last("LIMIT 1");
        Posting posting = postingMapper.selectOne(wrapper);
        if (posting == null) {
            throw new DomainException("订单不存在，请先同步订单数据");
        }
        return posting;
    }

    @Override
    public JsonNode detailFromApi(String postingNumber) {
        FbsGetReq req = new FbsGetReq();
        req.setPostingNumber(requirePostingNumber(postingNumber));
        return client.fbsGetRaw(req);
    }

    @Override
    public CancelReasonResp cancelReasons(String postingNumber) {
        CancelReasonReq req = new CancelReasonReq();
        req.setRelatedPostingNumber(requirePostingNumber(postingNumber));
        return client.cancelReasons(req);
    }

    @Override
    public JsonNode cancel(CancelReq req) {
        if (req == null || !StringUtils.hasText(req.getPostingNumber())) {
            throw new DomainException("订单号不能为空");
        }
        if (req.getCancelReasonId() == null) {
            throw new DomainException("请选择取消原因");
        }
        JsonNode resp = client.cancel(req);
        refreshOne(req.getPostingNumber());
        return resp;
    }

    /** 取消后刷新本地状态 */
    private void refreshOne(String postingNumber) {
        try {
            FbsGetReq getReq = new FbsGetReq();
            getReq.setPostingNumber(postingNumber);
            FbsPosting detail = client.fbsGet(getReq);
            save(toEntity(detail, null, null), loadExisting());
        } catch (RuntimeException e) {
            log.warn("订单 {} 状态刷新失败：{}", postingNumber, e.getMessage());
        }
    }

    @Override
    public List<Posting> pendingShipment(int limit) {
        LambdaQueryWrapper<Posting> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Posting::getStatus, PENDING_STATUSES)
                .orderByAsc(Posting::getShipmentPlanDate)
                .last("LIMIT " + Math.max(1, Math.min(limit, 500)));
        return postingMapper.selectList(wrapper);
    }

    private static String requirePostingNumber(String postingNumber) {
        if (!StringUtils.hasText(postingNumber)) {
            throw new DomainException("订单号不能为空");
        }
        return postingNumber;
    }
}
