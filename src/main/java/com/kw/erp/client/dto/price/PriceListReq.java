package com.kw.erp.client.dto.price;

import lombok.Data;

import java.util.List;

/**
 * /v5/product/info/prices 请求
 */
@Data
public class PriceListReq {

    private String cursor = "";

    private int limit = 100;

    private Filter filter;

    @Data
    public static class Filter {
        /** all/with_comment/price_expired */
        private String withErrors;
        private Boolean visibilityDetails;
        private List<String> productId;
        private List<String> offerId;
    }
}
