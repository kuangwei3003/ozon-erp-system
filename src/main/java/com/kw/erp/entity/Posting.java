package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FBS / rFBS 订单（posting）
 */
@Data
@TableName("posting")
public class Posting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String postingNumber;

    private Long orderId;

    /** fbs / rfbs */
    private String scheme;

    private String status;

    private String substatus;

    /** 运单号 */
    private String trackingNumber;

    /** 配送方式名称 */
    private String deliveryMethod;

    private Long warehouseId;

    /** 计划发货日期 */
    private LocalDateTime shipmentDate;

    /** 订单创建时间（来自 Ozon） */
    private LocalDateTime createdAt;

    private LocalDateTime shipmentPlanDate;

    private LocalDateTime deadlineDate;

    private LocalDateTime anyAtDelivery;

    private String cancelReason;

    private String cancelerId;

    private Integer isMultiPackage;

    /** 商品行 JSON */
    private String productsJson;

    /** 财务信息 JSON */
    private String financialDataJson;

    /** 客户信息 JSON */
    private String customerJson;

    /** 条码 JSON */
    private String barcodesJson;

    /** Ozon 侧最后更新时间 */
    private LocalDateTime updatedOnApi;

    private LocalDateTime syncedAt;
}
