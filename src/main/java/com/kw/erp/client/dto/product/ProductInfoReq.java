package com.kw.erp.client.dto.product;

import lombok.Data;

import java.util.List;

/**
 * /v3/product/info/list 请求
 */
@Data
public class ProductInfoReq {

    private List<Long> productId;

    private List<Long> sku;

    private List<String> offerId;

    /** 兼容旧版：offer_id 列表 */
    private List<String> productUrls;
}
