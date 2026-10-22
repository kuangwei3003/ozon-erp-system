package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退货记录
 */
@Data
@TableName("return_item")
public class ReturnItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long returnId;

    private String postingNumber;

    private Long sku;

    private String offerId;

    private String productName;

    /** 退货状态 */
    private String status;

    /** 退货方式：seller / ozon */
    private String returnMode;

    private String reasonType;

    private String customerName;

    private LocalDateTime awaitingDelivDeadline;

    private LocalDateTime decisionDate;

    private LocalDateTime returnDate;

    private LocalDateTime rebateDate;

    /** 原始报文，便于字段核对与排查 */
    private String rawJson;

    private LocalDateTime syncedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
