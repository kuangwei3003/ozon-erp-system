package com.kw.erp.client.dto.price;

import lombok.Data;

import java.util.List;

/**
 * /v1/product/import/prices 请求
 */
@Data
public class PriceImportReq {

    private List<PriceItem> prices;

    /** 是否自动追加人民币价格 */
    private Boolean autoActionEnabled;

    @Data
    public static class PriceItem {
        private String offerId;
        private Long productId;
        /** 价格（最小货币单位整数） */
        private String price;
        private String oldPrice;
        private String minPrice;
        private String currencyCode;
        private String vat;
    }
}
