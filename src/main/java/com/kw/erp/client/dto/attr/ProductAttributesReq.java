package com.kw.erp.client.dto.attr;

import lombok.Data;

import java.util.List;

/**
 * /v1/product/attributes 请求（查询商品已填属性与特征）
 */
@Data
public class ProductAttributesReq {

    private Integer limit = 100;

    private Integer lastId = 0;

    private Filter filter;

    @Data
    public static class Filter {
        private List<Long> productId;
        private List<Long> sku;
        private List<String> offerId;
    }
}
