package com.kw.erp.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 属性与特征页面
 */
@Controller
@RequestMapping("/attributes")
public class AttributePageController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("active", "attributes");
        return "attribute/index";
    }
}
