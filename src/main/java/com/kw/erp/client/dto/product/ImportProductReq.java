package com.kw.erp.client.dto.product;

import lombok.Data;

import java.util.List;

/**
 * /v3/product/import 上传/更新商品请求
 */
@Data
public class ImportProductReq {

    private List<ImportItem> items;

    @Data
    public static class ImportItem {
        /** 卖家商品编号 */
        private String offerId;
        /** 商品名称 */
        private String name;
        /** 类目属性模板的 description_category_id */
        private Long categoryId;
        /** 类目属性模板的 type_id */
        private Long typeId;
        /** 价格（最小货币单位整数，如卢布×100） */
        private String price;
        /** 折扣前原价 */
        private String oldPrice;
        /** 最低价 */
        private String minPrice;
        /** 是否免费商品 */
        private Boolean isFree;
        /** 币种 */
        private String currencyCode;
        /** 佣金百分比 */
        private String vat;
        /** 是否品牌卖家 */
        private Boolean vendor;
        /** 包装尺寸 */
        private Long height;
        private Long depth;
        private Long width;
        /** 尺寸单位 mm/cm/m */
        private String dimensionUnit;
        /** 重量（克） */
        private Long weight;
        /** 重量单位 g/kg/lb */
        private String weightUnit;
        /** 图片列表 */
        private List<String> images;
        private List<String> images360;
        /** 条码 */
        private List<String> barcode;
        /** 属性与特征 */
        private List<AttrItem> attributes;
        /** 组合属性（如颜色尺寸网格） */
        private List<ComplexAttribute> complexAttributes;
        /** 关联到已有 SKU（合并卡片） */
        private Long newSku;
        /** 自动生成条码 */
        private Boolean autoBarcodesGenerate;

        @Data
        public static class AttrItem {
            /** 属性 ID（来自 /v1/description-category/attribute） */
            private Long id;
            /** 复杂属性行号，普通属性为 0 */
            private Long complexId;
            private List<Value> values;

            @Data
            public static class Value {
                /** 文本值 */
                private String value;
                /** 字典值 ID（字典属性必填） */
                private Long dictionaryValueId;
            }
        }

        @Data
        public static class ComplexAttribute {
            private Long complexId;
            private List<AttrItem> attributes;
        }
    }
}
