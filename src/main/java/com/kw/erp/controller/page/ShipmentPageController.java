package com.kw.erp.controller.page;

import com.kw.erp.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 运单处理页面
 */
@Controller
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentPageController {

    private final OrderService orderService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("postings", orderService.pendingShipment(200));
        model.addAttribute("active", "shipments");
        return "shipment/list";
    }
}
