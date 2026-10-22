package com.kw.erp.client.dto.order;

import lombok.Data;

/**
 * /v5/posting/cancel 请求
 */
@Data
public class CancelReq {

    private String postingNumber;

    /** 取消原因 ID（来自 cancel-reason 接口） */
    private Integer cancelReasonId;

    /** 取消原因补充说明 */
    private String cancelReasonMessage;

    private String account;
}
