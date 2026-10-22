package com.kw.erp.client.dto.product;

import lombok.Data;

import java.util.List;

/**
 * /v1/product/import/info 请求（查询上传任务状态）
 */
@Data
public class ImportStatusReq {

    /** 导入任务 ID（来自 /v3/product/import 返回的 task_id） */
    private Long taskId;

    private List<String> offerId;
}
