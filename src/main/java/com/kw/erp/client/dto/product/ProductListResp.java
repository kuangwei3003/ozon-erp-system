package com.kw.erp.client.dto.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * /v3/product/list 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductListResp {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Item> items;
        private String lastId;
        private int total;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private long id;
        private String name;
        private String offerId;
        private ProductStatus status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductStatus {
        private String status;
        private StatusDetail statusDetail;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusDetail {
        private String status;
        /** 是否可售卖 */
        private boolean isValid;
    }
}
