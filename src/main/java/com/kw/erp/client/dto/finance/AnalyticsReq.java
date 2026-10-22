package com.kw.erp.client.dto.finance;

import lombok.Data;

import java.util.List;

/**
 * /v1/analytics/data 请求
 */
@Data
public class AnalyticsReq {

    /** 指标列表，如 sales_turnover、posts 等 */
    private List<String> metrics;

    /** 维度列表，如 sku、city、delivery_region 等 */
    private List<String> dimensions;

    private Filter filter;

    /** sort 字段，如 -created_at */
    private String sort;

    private int limit = 100;

    private int offset = 0;

    @Data
    public static class Filter {
        /** ISO 时间 */
        private String dateFrom;
        private String dateTo;
    }
}
