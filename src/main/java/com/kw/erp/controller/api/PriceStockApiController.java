package com.kw.erp.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.price.PriceImportReq;
import com.kw.erp.client.dto.stock.StocksReq;
import com.kw.erp.common.PageResult;
import com.kw.erp.common.Result;
import com.kw.erp.entity.PriceStock;
import com.kw.erp.service.PriceStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 价格与库存接口
 */
@RestController
@RequestMapping("/api/price-stock")
@RequiredArgsConstructor
public class PriceStockApiController {

    private final PriceStockService priceStockService;

    @PostMapping("/sync")
    public Result<Integer> sync() {
        return Result.ok(priceStockService.sync());
    }

    @GetMapping
    public Result<PageResult<PriceStock>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String keyword) {
        IPage<PriceStock> page = priceStockService.page(current, size, keyword);
        return Result.ok(PageResult.of(page));
    }

    /** 批量更新价格 */
    @PostMapping("/prices")
    public Result<JsonNode> updatePrices(@RequestBody PriceImportReq req) {
        return Result.ok(priceStockService.updatePrices(req));
    }

    /** 批量更新库存 */
    @PostMapping("/stocks")
    public Result<JsonNode> updateStocks(@RequestBody StocksReq req) {
        return Result.ok(priceStockService.updateStocks(req));
    }
}
