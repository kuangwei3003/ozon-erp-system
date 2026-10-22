package com.kw.erp.client.dto.order;

import lombok.Data;

/**
 * /v3/posting/fbs/cancel-reason 请求
 */
@Data
public class CancelReasonReq {

    private String relatedPostingNumber;
}
