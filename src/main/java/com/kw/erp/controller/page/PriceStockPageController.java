package com.kw.erp.controller.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kw.erp.entity.PriceStock;
import com.kw.erp.service.PriceStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 价格与库存页面
 */
@Controller
@RequestMapping("/price-stock")
@RequiredArgsConstructor
public class PriceStockPageController {

    private final PriceStockService priceStockService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") long current,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        IPage<PriceStock> page = priceStockService.page(current, size, keyword);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", "priceStock");
        return "pricestock/list";
    }
}
