package com.kw.erp.client.dto.chat;

import lombok.Data;

/**
 * /v3/chat/messages 请求（历史消息）
 */
@Data
public class ChatMessagesReq {

    private String chatId;

    /** asc / desc */
    private String dir = "desc";

    /** 上一页返回的 cursor；首页为 "" */
    private String cursor = "";

    private int limit = 50;

    /** 是否包含已撤回消息 */
    private boolean withRevoked = false;
}
