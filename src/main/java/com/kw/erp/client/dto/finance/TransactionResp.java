package com.kw.erp.client.dto.finance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * /v1/finance/transaction/list 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResp {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private int rowCount;
        private List<Transaction> transactions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transaction {
        private Long operationId;
        private String operationDt;
        private String operationType;
        private BigDecimal operationAmount;
        private String logicNumber;
        private String kato;
        private String bizType;
        private String bizId;
        private String keyword;
        private String active;
        private String comment;
        private ItemInfo item;
        private List<PostingInfo> postings;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ItemInfo {
            private String sku;
            private String name;
            private String offerId;
            private String brandName;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PostingInfo {
            private String postingNumber;
            private String scheme;
            private Long orderId;
            private Long warehouseId;
        }
    }
}
