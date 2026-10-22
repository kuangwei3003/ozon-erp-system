package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String chatId;

    private String messageId;

    private Long userId;

    /** 发送方：seller / customer */
    private String sender;

    /** 发送方标识原始值 */
    private String senderAsset;

    /** 消息类型 */
    private String type;

    /** 状态：sent / delivered / read */
    private String status;

    private String content;

    /** Ozon 侧消息时间 */
    private LocalDateTime createdDt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
