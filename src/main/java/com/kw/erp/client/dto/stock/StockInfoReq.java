package com.kw.erp.client.dto.stock;

import lombok.Data;

import java.util.List;

/**
 * /v4/product/info/stocks 请求
 */
@Data
public class StockInfoReq {

    private String cursor = "";

    private int limit = 100;

    private Filter filter;

    @Data
    public static class Filter {
        private Long sku;
        private String offerId;
        private Long warehouseId;
    }
}
