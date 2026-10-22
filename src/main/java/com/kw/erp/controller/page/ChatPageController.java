package com.kw.erp.controller.page;

import com.kw.erp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 聊天页面
 */
@Controller
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatPageController {

    private final ChatService chatService;

    @GetMapping
    public String rooms(@RequestParam(required = false) String filter, Model model) {
        model.addAttribute("rooms", chatService.rooms(filter));
        model.addAttribute("filter", filter);
        model.addAttribute("active", "chats");
        return "chat/rooms";
    }

    @GetMapping("/{chatId}")
    public String room(@PathVariable String chatId, Model model) {
        model.addAttribute("chatId", chatId);
        model.addAttribute("messages", chatService.messages(chatId));
        model.addAttribute("rooms", chatService.rooms(null));
        model.addAttribute("active", "chats");
        return "chat/room";
    }
}
