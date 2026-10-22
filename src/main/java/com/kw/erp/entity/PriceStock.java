package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 价格与库存快照
 */
@Data
@TableName("price_stock")
public class PriceStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sku;

    private String offerId;

    /** 现价（原始字符串，含货币小数位） */
    private String price;

    private String oldPrice;

    private String minPrice;

    private String marketPrice;

    private String currencyCode;

    /** 价格指数（INFO_PRICE_INDEX 等） */
    private String priceIndex;

    /** 可用库存 */
    private Integer stock;

    /** 预留数量 */
    private Integer reserved;

    private Long warehouseId;

    private String warehouseName;

    /** 待到货数量 */
    private Integer incoming;

    private LocalDateTime stocksUpdatedAt;

    private LocalDateTime pricesUpdatedAt;

    private LocalDateTime syncedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
