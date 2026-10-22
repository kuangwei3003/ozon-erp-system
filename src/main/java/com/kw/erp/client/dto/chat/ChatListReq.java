package com.kw.erp.client.dto.chat;

import lombok.Data;

/**
 * /v1/chat/list 请求
 */
@Data
public class ChatListReq {

    private int limit = 100;

    /** 上一页返回的 chat_id；首页为空字符串 */
    private String after = "";

    /** all / unread / unanswered */
    private String chatStatus;
}
