package com.kw.erp.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 财务与分析页面：数据量按需在页面内查询，不落本地表
 */
@Controller
@RequestMapping("/finance")
public class FinancePageController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("active", "finance");
        return "finance/index";
    }
}
