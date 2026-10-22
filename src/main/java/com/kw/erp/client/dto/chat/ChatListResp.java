package com.kw.erp.client.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * /v1/chat/list 响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatListResp {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private List<Chat> chats;
        private boolean haveMore;
        private String cursor;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chat {
        private String chatId;
        private String chatStatus;
        private String chatType;
        private String createdBy;
        private Long createdTs;
        private Integer firstUnreadIssue;
        private Integer lastMessageId;
        private Integer unreadItems;
        private Long updatedTs;
        private Integer chatCount;
    }
}
