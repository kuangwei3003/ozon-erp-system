package com.kw.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.finance.AnalyticsReq;
import com.kw.erp.client.dto.finance.TransactionReq;
import com.kw.erp.client.dto.finance.TransactionResp;
import com.kw.erp.exception.DomainException;
import com.kw.erp.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 财务与分析实现：直接透传 Ozon 接口（数据量大且无需本地检索）
 */
@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /** 默认查询区间：近 30 天 */
    private static final int DEFAULT_DAYS = 30;

    private final OzonApiClient client;

    @Override
    public TransactionResp transactions(TransactionReq req) {
        TransactionReq actual = req == null ? new TransactionReq() : req;
        if (actual.getFilter() == null) {
            actual.setFilter(new TransactionReq.Filter());
        }
        TransactionReq.Filter filter = actual.getFilter();
        if (!StringUtils.hasText(filter.getDateFrom())) {
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            filter.setDateFrom(nowUtc.minusDays(DEFAULT_DAYS).format(UTC_FORMAT));
            filter.setDateTo(nowUtc.format(UTC_FORMAT));
        }
        if (actual.getPage() < 1) {
            actual.setPage(1);
        }
        if (actual.getPageSize() < 1 || actual.getPageSize() > 1000) {
            actual.setPageSize(100);
        }
        return client.financeTransactions(actual);
    }

    @Override
    public JsonNode analytics(AnalyticsReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getMetrics())) {
            throw new DomainException("请至少选择一个统计指标");
        }
        if (req.getFilter() == null) {
            AnalyticsReq.Filter filter = new AnalyticsReq.Filter();
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            filter.setDateFrom(nowUtc.minusDays(DEFAULT_DAYS).format(UTC_FORMAT));
            filter.setDateTo(nowUtc.format(UTC_FORMAT));
            req.setFilter(filter);
        }
        return client.analyticsData(req);
    }
}
