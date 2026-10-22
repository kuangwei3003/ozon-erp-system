package com.kw.erp.controller.page;

import com.kw.erp.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 店铺设置页面
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsPageController {

    private final ShopService shopService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("shops", shopService.list());
        model.addAttribute("active", "settings");
        return "shop/settings";
    }
}
