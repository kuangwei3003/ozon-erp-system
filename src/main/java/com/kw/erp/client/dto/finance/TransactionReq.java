package com.kw.erp.client.dto.finance;

import lombok.Data;

import java.util.List;

/**
 * /v1/finance/transaction/list 请求
 */
@Data
public class TransactionReq {

    private int page = 1;

    private int pageSize = 100;

    private Filter filter;

    @Data
    public static class Filter {
        /** ISO 时间，如 2026-01-01T00:00:00Z */
        private String dateFrom;
        private String dateTo;
        private List<String> operationTypes;
        private List<String> operationTypesGroup;
        private List<String> postings;
        private Boolean hasPayingClientAnalytics;
    }
}
