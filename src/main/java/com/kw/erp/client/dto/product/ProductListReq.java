package com.kw.erp.client.dto.product;

import lombok.Data;

import java.util.List;

/**
 * /v3/product/list 请求
 */
@Data
public class ProductListReq {

    private String visibility;

    private Filter filter;

    private int limit = 100;

    private String lastId = "";

    @Data
    public static class Filter {
        private String visibility;
        private List<String> offerIdList;
    }
}
