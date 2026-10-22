package com.kw.erp.client.dto.chat;

import lombok.Data;

/**
 * /v1/chat/send/message 请求（发送消息）
 */
@Data
public class ChatSendReq {

    private String chatId;

    private String text;

    /** 消息类型：USER_DEFINED / ISSUE_UPDATED 等 */
    private String messageType;
}
