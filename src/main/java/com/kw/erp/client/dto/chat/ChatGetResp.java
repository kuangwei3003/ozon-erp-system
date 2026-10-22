package com.kw.erp.client.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * /v1/chat/get 响应（聊天室详情）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatGetResp {

    private Result result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Chat chat;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chat {
        private String chatId;
        private String chatStatus;
        private String chatType;
        private String createdBy;
        private Long createdTs;
        private Integer unreadItems;
        private Long updatedTs;
        private List<String> tags;
    }
}
