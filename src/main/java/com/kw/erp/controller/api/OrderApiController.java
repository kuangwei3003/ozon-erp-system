package com.kw.erp.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.order.CancelReq;
import com.kw.erp.client.dto.order.CancelReasonResp;
import com.kw.erp.common.PageResult;
import com.kw.erp.common.Result;
import com.kw.erp.entity.Posting;
import com.kw.erp.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FBS / rFBS 订单接口
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;

    /** 同步订单，scheme 可为 fbs / rfbs / 空（全部） */
    @PostMapping("/sync")
    public Result<Integer> sync(@RequestParam(required = false) String scheme) {
        return Result.ok(orderService.sync(scheme));
    }

    @GetMapping
    public Result<PageResult<Posting>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String scheme,
                                            @RequestParam(required = false) String keyword) {
        IPage<Posting> page = orderService.page(current, size, status, scheme, keyword);
        return Result.ok(PageResult.of(page));
    }

    /** 实时订单详情（原始报文） */
    @GetMapping("/remote/{postingNumber}")
    public Result<JsonNode> detailFromApi(@PathVariable String postingNumber) {
        return Result.ok(orderService.detailFromApi(postingNumber));
    }

    /** 可选取消原因 */
    @GetMapping("/{postingNumber}/cancel-reasons")
    public Result<CancelReasonResp> cancelReasons(@PathVariable String postingNumber) {
        return Result.ok(orderService.cancelReasons(postingNumber));
    }

    /** 取消订单 */
    @PostMapping("/cancel")
    public Result<JsonNode> cancel(@RequestBody CancelReq req) {
        return Result.ok(orderService.cancel(req));
    }
}
