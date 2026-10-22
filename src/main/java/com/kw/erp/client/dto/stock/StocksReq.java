package com.kw.erp.client.dto.stock;

import lombok.Data;

import java.util.List;

/**
 * /v2/products/stocks 请求（更新库存）
 */
@Data
public class StocksReq {

    private List<StockItem> stocks;

    @Data
    public static class StockItem {
        private Long sku;
        private String offerId;
        private Integer stock;
        private Long warehouseId;
        /** 处理中心编号（FBO 用），卖家仓库可空 */
        private Long officeId;
        /** true / false / cancel */
        private String status;
    }
}
