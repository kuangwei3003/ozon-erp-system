package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品本地缓存
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Ozon SKU */
    private Long sku;

    /** 卖家商品编号 */
    private String offerId;

    private String name;

    private String primaryImage;

    private String skuCode;

    private Long typeId;

    private Long categoryId;

    private String brand;

    /** 状态：purchase_available / expired / inactive 等 */
    private String status;

    /** 可见性 */
    private String visibility;

    private Integer hasStock;

    /** 是否限制取消 */
    private Integer limitCancel;

    /** 原始报文 */
    private String rawJson;

    private LocalDateTime syncedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
