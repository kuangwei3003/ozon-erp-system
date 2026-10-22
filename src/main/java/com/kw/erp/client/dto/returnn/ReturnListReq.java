package com.kw.erp.client.dto.returnn;

import lombok.Data;

/**
 * /v1/return/list 请求（退货列表）
 */
@Data
public class ReturnListReq {

    /** 上一页返回的 next 游标；首页为 0 */
    private int page = 1;

    private int pageSize = 50;

    private Integer filter;

    private String createdAtStart;

    private String createdAtEnd;

    private String destination;

    private String productId;

    private String postingNumber;
}
