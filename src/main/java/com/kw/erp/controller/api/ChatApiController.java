package com.kw.erp.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.chat.ChatSendReq;
import com.kw.erp.common.Result;
import com.kw.erp.entity.ChatMessage;
import com.kw.erp.entity.ChatRoom;
import com.kw.erp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天接口
 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;

    @PostMapping("/sync")
    public Result<Integer> syncRooms() {
        return Result.ok(chatService.syncRooms());
    }

    @GetMapping("/rooms")
    public Result<List<ChatRoom>> rooms(@RequestParam(required = false) String filter) {
        return Result.ok(chatService.rooms(filter));
    }

    /** 拉取指定聊天室消息 */
    @PostMapping("/{chatId}/sync")
    public Result<Integer> syncMessages(@PathVariable String chatId) {
        return Result.ok(chatService.syncMessages(chatId));
    }

    @GetMapping("/{chatId}/messages")
    public Result<List<ChatMessage>> messages(@PathVariable String chatId) {
        return Result.ok(chatService.messages(chatId));
    }

    /** 发送消息 */
    @PostMapping("/send")
    public Result<JsonNode> send(@RequestBody ChatSendReq req) {
        return Result.ok(chatService.send(req));
    }

    @GetMapping("/unread")
    public Result<Long> unread() {
        return Result.ok(chatService.unreadCount());
    }
}
