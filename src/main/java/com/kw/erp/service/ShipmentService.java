package com.kw.erp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.shipment.LabelReq;
import com.kw.erp.client.dto.shipment.SetTrackedReq;

/**
 * 运单处理：仓库、发货标签、运单号
 */
public interface ShipmentService {

    /** 卖家仓库列表 */
    JsonNode warehouses();

    /** 录入（批量更新）运单号 */
    JsonNode setTracking(SetTrackedReq req);

    /** 获取发货包装标签（响应内含 base64 PDF） */
    JsonNode label(LabelReq req);
}
