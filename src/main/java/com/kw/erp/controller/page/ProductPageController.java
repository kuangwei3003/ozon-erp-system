package com.kw.erp.controller.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kw.erp.entity.Product;
import com.kw.erp.service.AttributeService;
import com.kw.erp.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品页面：列表、详情、上传表单
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductPageController {

    private final ProductService productService;
    private final AttributeService attributeService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") long current,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       Model model) {
        IPage<Product> page = productService.page(current, size, keyword, status);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("active", "products");
        return "product/list";
    }

    @GetMapping("/upload")
    public String upload(Model model) {
        model.addAttribute("active", "productUpload");
        return "product/upload";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productService.detail(id);
        model.addAttribute("product", product);
        model.addAttribute("attributes", attributeService.attributesOfProduct(product.getSku()));
        model.addAttribute("active", "products");
        return "product/detail";
    }
}
