package com.kw.erp.controller.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kw.erp.entity.Posting;
import com.kw.erp.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * FBS / rFBS 订单页面
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderPageController {

    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") long current,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String scheme,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        IPage<Posting> page = orderService.page(current, size, status, scheme, keyword);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        model.addAttribute("scheme", scheme);
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", "orders");
        return "order/list";
    }

    @GetMapping("/{postingNumber}")
    public String detail(@PathVariable String postingNumber, Model model) {
        model.addAttribute("posting", orderService.detail(postingNumber));
        model.addAttribute("postingNumber", postingNumber);
        model.addAttribute("active", "orders");
        return "order/detail";
    }
}
