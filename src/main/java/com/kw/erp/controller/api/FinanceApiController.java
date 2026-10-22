package com.kw.erp.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.finance.AnalyticsReq;
import com.kw.erp.client.dto.finance.TransactionReq;
import com.kw.erp.client.dto.finance.TransactionResp;
import com.kw.erp.common.Result;
import com.kw.erp.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务与分析接口
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceApiController {

    private final FinanceService financeService;

    /** 交易流水 */
    @PostMapping("/transactions")
    public Result<TransactionResp> transactions(@RequestBody(required = false) TransactionReq req) {
        return Result.ok(financeService.transactions(req));
    }

    /** 分析指标 */
    @PostMapping("/analytics")
    public Result<JsonNode> analytics(@RequestBody AnalyticsReq req) {
        return Result.ok(financeService.analytics(req));
    }
}
