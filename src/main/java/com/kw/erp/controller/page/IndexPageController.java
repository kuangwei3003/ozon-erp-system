package com.kw.erp.controller.page;

import com.kw.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 仪表盘页面
 */
@Controller
@RequiredArgsConstructor
public class IndexPageController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("active", "dashboard");
        model.addAttribute("stats", dashboardService.overview());
        return "index";
    }
}
