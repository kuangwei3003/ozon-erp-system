package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.client.dto.chat.ChatListReq;
import com.kw.erp.client.dto.chat.ChatListResp;
import com.kw.erp.client.dto.chat.ChatMessagesReq;
import com.kw.erp.client.dto.chat.ChatSendReq;
import com.kw.erp.entity.ChatMessage;
import com.kw.erp.entity.ChatRoom;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ChatMessageMapper;
import com.kw.erp.mapper.ChatRoomMapper;
import com.kw.erp.service.ChatService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天管理实现：聊天室同步 + 消息拉取/发送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int CHAT_PAGE_LIMIT = 100;

    private static final int MAX_PAGES = 20;

    private static final int MESSAGE_PAGE_LIMIT = 50;

    private static final int MAX_MESSAGE_PAGES = 10;

    private final OzonApiClient client;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public int syncRooms() {
        Map<String, ChatRoom> existing = new HashMap<>();
        for (ChatRoom room : chatRoomMapper.selectList(null)) {
            existing.put(room.getChatId(), room);
        }
        String after = "";
        int count = 0;
        for (int i = 0; i < MAX_PAGES; i++) {
            ChatListReq req = new ChatListReq();
            req.setLimit(CHAT_PAGE_LIMIT);
            req.setAfter(after);
            ChatListResp resp = client.chatList(req);
            if (resp == null || resp.getResult() == null
                    || resp.getResult().getChats() == null || resp.getResult().getChats().isEmpty()) {
                break;
            }
            for (ChatListResp.Chat chat : resp.getResult().getChats()) {
                ChatRoom entity = new ChatRoom();
                entity.setChatId(chat.getChatId());
                entity.setChatType(chat.getChatType());
                entity.setChatStatus(chat.getChatStatus());
                entity.setUnread(chat.getUnreadItems() == null ? 0 : chat.getUnreadItems());
                entity.setUpdatedTs(JsonUtils.parseEpochSecond(chat.getUpdatedTs()));
                entity.setSyncedAt(LocalDateTime.now());
                saveRoom(entity, existing);
                count++;
            }
            String cursor = resp.getResult().getCursor();
            if (!StringUtils.hasText(cursor) || cursor.equals(after)) {
                break;
            }
            after = cursor;
            if (!resp.getResult().isHaveMore()) {
                break;
            }
        }
        log.info("聊天室同步完成，共 {} 个", count);
        return count;
    }

    /** 聊天室已存在时保留本地已有的最后一条消息摘要 */
    private void saveRoom(ChatRoom entity, Map<String, ChatRoom> existing) {
        ChatRoom old = existing.get(entity.getChatId());
        if (old == null) {
            entity.setCreatedAt(LocalDateTime.now());
            chatRoomMapper.insert(entity);
            existing.put(entity.getChatId(), entity);
        } else {
            entity.setId(old.getId());
            entity.setLastMessage(old.getLastMessage());
            chatRoomMapper.updateById(entity);
        }
    }

    @Override
    public List<ChatRoom> rooms(String filter) {
        LambdaQueryWrapper<ChatRoom> wrapper = new LambdaQueryWrapper<>();
        if ("unread".equalsIgnoreCase(filter)) {
            wrapper.gt(ChatRoom::getUnread, 0);
        } else if ("closed".equalsIgnoreCase(filter)) {
            wrapper.eq(ChatRoom::getChatStatus, "closed");
        }
        wrapper.orderByDesc(ChatRoom::getUnread).orderByDesc(ChatRoom::getUpdatedTs);
        return chatRoomMapper.selectList(wrapper);
    }

    @Override
    public int syncMessages(String chatId) {
        if (!StringUtils.hasText(chatId)) {
            throw new DomainException("聊天室 ID 不能为空");
        }
        Map<String, ChatMessage> existing = new HashMap<>();
        for (ChatMessage msg : messages(chatId)) {
            if (msg.getMessageId() != null) {
                existing.put(msg.getMessageId(), msg);
            }
        }
        String cursor = "";
        int count = 0;
        for (int i = 0; i < MAX_MESSAGE_PAGES; i++) {
            ChatMessagesReq req = new ChatMessagesReq();
            req.setChatId(chatId);
            req.setLimit(MESSAGE_PAGE_LIMIT);
            req.setCursor(cursor);
            req.setDir("asc");
            JsonNode resp = client.chatMessages(req);
            JsonNode messagesNode = JsonUtils.path(resp, "result", "messages");
            if (!messagesNode.isArray() && resp != null) {
                messagesNode = resp.path("messages");
            }
            List<JsonNode> nodes = JsonUtils.elements(messagesNode);
            if (nodes.isEmpty()) {
                break;
            }
            for (JsonNode node : nodes) {
                ChatMessage entity = parseMessage(chatId, node);
                if (entity.getMessageId() == null || existing.containsKey(entity.getMessageId())) {
                    continue;
                }
                entity.setCreatedAt(LocalDateTime.now());
                chatMessageMapper.insert(entity);
                existing.put(entity.getMessageId(), entity);
                count++;
            }
            String next = JsonUtils.text(JsonUtils.path(resp, "result"), "cursor");
            if (next == null) {
                next = JsonUtils.text(resp, "cursor");
            }
            if (!StringUtils.hasText(next) || next.equals(cursor)) {
                break;
            }
            cursor = next;
        }
        // 回填聊天室最后一条消息
        updateLastMessage(chatId);
        log.info("聊天 {} 消息同步完成，新增 {} 条", chatId, count);
        return count;
    }

    private ChatMessage parseMessage(String chatId, JsonNode node) {
        ChatMessage entity = new ChatMessage();
        entity.setChatId(chatId);
        entity.setMessageId(JsonUtils.text(node, "message_id", "id"));
        entity.setUserId(JsonUtils.longVal(node, "user_id"));
        entity.setType(JsonUtils.text(node, "type", "message_type"));
        entity.setStatus(JsonUtils.text(node, "status", "message_status"));
        entity.setSenderAsset(JsonUtils.text(node, "sender_asset", "from"));
        entity.setSender(resolveSender(node));
        entity.setContent(extractContent(node));
        entity.setCreatedDt(parseMessageTime(node));
        return entity;
    }

    /** 卖家自己发送的消息通常带 seller 标识，否则视为买家 */
    private String resolveSender(JsonNode node) {
        String asset = JsonUtils.text(node, "sender_asset", "from");
        if (asset == null) {
            asset = JsonUtils.text(JsonUtils.path(node, "data"), "from");
        }
        if (asset == null) {
            return "customer";
        }
        String lower = asset.toLowerCase();
        if (lower.contains("seller") || lower.contains("shop") || "SELLER".equals(JsonUtils.text(node, "type"))) {
            return "seller";
        }
        return "customer";
    }

    /** 消息正文兼容 data 为字符串或 {text:...} 对象 */
    private String extractContent(JsonNode node) {
        JsonNode data = node.path("data");
        if (data.isTextual()) {
            return data.asText();
        }
        String text = JsonUtils.text(data, "text", "value", "content");
        if (text != null) {
            return text;
        }
        return data.isMissingNode() ? null : data.toString();
    }

    private LocalDateTime parseMessageTime(JsonNode node) {
        JsonNode createdAt = node.path("created_at");
        if (createdAt.isObject()) {
            LocalDateTime dt = JsonUtils.parseDate(JsonUtils.text(createdAt, "dt", "date"));
            if (dt != null) {
                return dt;
            }
            return JsonUtils.parseEpochSecond(JsonUtils.longVal(createdAt, "unix", "timestamp"));
        }
        LocalDateTime dt = JsonUtils.parseDate(createdAt.asText(null));
        return dt != null ? dt : JsonUtils.parseEpochSecond(JsonUtils.longVal(node, "created_at_unix"));
    }

    private void updateLastMessage(String chatId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getChatId, chatId).orderByDesc(ChatMessage::getId).last("LIMIT 1");
        ChatMessage last = chatMessageMapper.selectOne(wrapper);
        if (last == null) {
            return;
        }
        LambdaQueryWrapper<ChatRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(ChatRoom::getChatId, chatId).last("LIMIT 1");
        ChatRoom room = chatRoomMapper.selectOne(roomWrapper);
        if (room != null) {
            room.setLastMessage(last.getContent());
            room.setUserId(last.getUserId());
            chatRoomMapper.updateById(room);
        }
    }

    @Override
    public List<ChatMessage> messages(String chatId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getChatId, chatId).orderByAsc(ChatMessage::getId);
        return chatMessageMapper.selectList(wrapper);
    }

    @Override
    public JsonNode send(ChatSendReq req) {
        if (req == null || !StringUtils.hasText(req.getChatId())) {
            throw new DomainException("请选择聊天室");
        }
        if (!StringUtils.hasText(req.getText())) {
            throw new DomainException("消息内容不能为空");
        }
        JsonNode resp = client.chatSend(req);
        // 发送成功后本地立即落一条卖家消息，避免等待下一次同步
        ChatMessage entity = new ChatMessage();
        entity.setChatId(req.getChatId());
        entity.setMessageId(JsonUtils.text(JsonUtils.path(resp, "result"), "message_id"));
        entity.setSender("seller");
        entity.setType("text");
        entity.setStatus("sent");
        entity.setContent(req.getText());
        entity.setCreatedDt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(entity);
        updateLastMessage(req.getChatId());
        return resp;
    }

    @Override
    public long unreadCount() {
        LambdaQueryWrapper<ChatRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(ChatRoom::getUnread, 0);
        return chatRoomMapper.selectList(wrapper).stream()
                .mapToLong(room -> room.getUnread() == null ? 0 : room.getUnread())
                .sum();
    }
}
