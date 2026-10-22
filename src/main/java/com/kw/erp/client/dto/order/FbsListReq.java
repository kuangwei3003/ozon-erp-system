package com.kw.erp.client.dto.order;

import lombok.Data;

/**
 * /v3/posting/fbs/list 请求
 */
@Data
public class FbsListReq {

    private Filter filter;

    private int limit = 100;

    private int offset = 0;

    private String dir = "desc";

    @Data
    public static class Filter {
        /** fbs / rfbs */
        private String scheme;
        private String posting;
        private String orderStatus;
        private String cancelReasonId;
        private String warehouseId;
        private String offerId;
        private Long productDensity;
        /** ISO 时间字符串，如 2026-01-01T00:00:00Z */
        private String since;
        private String to;
        /** 未处理订单（待发货）填 true */
        private Boolean unfulfilled;
        /** 交易截止 ISO 时间 */
        private String transactionId;
    }
}
