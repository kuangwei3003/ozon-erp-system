package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品属性 / 特征
 */
@Data
@TableName("product_attribute")
public class ProductAttribute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long sku;

    private String offerId;

    private Long attributeId;

    /** 属性名称 */
    private String attrName;

    /** 属性值（JSON 数组文本） */
    private String valueJson;

    private Long complexId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
