package com.kw.erp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天室
 */
@Data
@TableName("chat_room")
public class ChatRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String chatId;

    /** user / issue / OzonChatStatusChanged 等 */
    private String chatType;

    /** opened / closed */
    private String chatStatus;

    /** 未读数 */
    private Integer unread;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 最后消息发送者用户 ID */
    private Long userId;

    /** Ozon 侧更新时间 */
    private LocalDateTime updatedTs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime syncedAt;
}
