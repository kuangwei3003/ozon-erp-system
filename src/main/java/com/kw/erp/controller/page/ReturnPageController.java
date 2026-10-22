package com.kw.erp.controller.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kw.erp.entity.ReturnItem;
import com.kw.erp.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 退货页面
 */
@Controller
@RequestMapping("/returns")
@RequiredArgsConstructor
public class ReturnPageController {

    private final ReturnService returnService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") long current,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        IPage<ReturnItem> page = returnService.page(current, size, status, keyword);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", "returns");
        return "return/list";
    }
}
