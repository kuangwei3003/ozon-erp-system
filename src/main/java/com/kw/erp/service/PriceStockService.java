package com.kw.erp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.price.PriceImportReq;
import com.kw.erp.client.dto.stock.StocksReq;
import com.kw.erp.entity.PriceStock;

/**
 * 商品价格与库存管理
 */
public interface PriceStockService {

    /** 从 Ozon 拉取价格与库存并写入本地表 */
    int sync();

    IPage<PriceStock> page(long current, long size, String keyword);

    /** 批量更新价格 */
    JsonNode updatePrices(PriceImportReq req);

    /** 批量更新库存 */
    JsonNode updateStocks(StocksReq req);
}
