package com.kw.erp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.chat.ChatSendReq;
import com.kw.erp.entity.ChatMessage;
import com.kw.erp.entity.ChatRoom;

import java.util.List;

/**
 * 买家聊天管理
 */
public interface ChatService {

    /** 同步聊天室列表 */
    int syncRooms();

    /** 本地聊天室列表（按未读与时间倒序） */
    List<ChatRoom> rooms(String filter);

    /** 从 Ozon 拉取指定聊天室的历史消息并落地 */
    int syncMessages(String chatId);

    /** 本地消息记录 */
    List<ChatMessage> messages(String chatId);

    /** 发送消息 */
    JsonNode send(ChatSendReq req);

    /** 未读消息总数 */
    long unreadCount();
}
