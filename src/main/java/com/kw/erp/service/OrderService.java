package com.kw.erp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.order.CancelReasonResp;
import com.kw.erp.client.dto.order.CancelReq;
import com.kw.erp.entity.Posting;

import java.util.List;

/**
 * FBS / rFBS 订单管理
 */
public interface OrderService {

    /**
     * 同步订单：先拉 posting 列表，再逐个取详情写入本地表
     *
     * @param scheme fbs / rfbs，为空表示两种模式都同步
     * @return 同步的订单数量
     */
    int sync(String scheme);

    IPage<Posting> page(long current, long size, String status, String scheme, String keyword);

    /** 本地订单详情 */
    Posting detail(String postingNumber);

    /** 实时从 Ozon 获取订单详情原始报文 */
    JsonNode detailFromApi(String postingNumber);

    /** 可选取消原因 */
    CancelReasonResp cancelReasons(String postingNumber);

    /** 取消订单 */
    JsonNode cancel(CancelReq req);

    /** 待发货订单（运单处理页使用） */
    List<Posting> pendingShipment(int limit);
}
