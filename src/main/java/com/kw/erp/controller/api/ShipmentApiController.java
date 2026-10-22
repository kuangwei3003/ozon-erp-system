package com.kw.erp.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.shipment.LabelReq;
import com.kw.erp.client.dto.shipment.SetTrackedReq;
import com.kw.erp.common.Result;
import com.kw.erp.entity.Posting;
import com.kw.erp.service.OrderService;
import com.kw.erp.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运单处理接口
 */
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentApiController {

    private final ShipmentService shipmentService;
    private final OrderService orderService;

    /** 卖家仓库列表 */
    @GetMapping("/warehouses")
    public Result<JsonNode> warehouses() {
        return Result.ok(shipmentService.warehouses());
    }

    /** 待发货订单（本地数据） */
    @GetMapping("/pending")
    public Result<List<Posting>> pending(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(orderService.pendingShipment(limit));
    }

    /** 录入运单号 */
    @PostMapping("/tracking")
    public Result<JsonNode> setTracking(@RequestBody SetTrackedReq req) {
        return Result.ok(shipmentService.setTracking(req));
    }

    /** 获取发货标签（响应内含 base64 PDF） */
    @PostMapping("/label")
    public Result<JsonNode> label(@RequestBody LabelReq req) {
        return Result.ok(shipmentService.label(req));
    }
}
