package com.kw.erp.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.dto.product.ImportProductReq;
import com.kw.erp.common.PageResult;
import com.kw.erp.common.Result;
import com.kw.erp.entity.Product;
import com.kw.erp.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;

    /** 从 Ozon 同步商品 */
    @PostMapping("/sync")
    public Result<Integer> sync() {
        return Result.ok(productService.sync());
    }

    /** 商品分页查询 */
    @GetMapping
    public Result<PageResult<Product>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status) {
        IPage<Product> page = productService.page(current, size, keyword, status);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    /** 上传或更新商品，返回 Ozon 原始响应（含 task_id） */
    @PostMapping("/import")
    public Result<JsonNode> upload(@RequestBody ImportProductReq req) {
        return Result.ok(productService.upload(req));
    }

    /** 查询上传任务状态 */
    @GetMapping("/import/status/{taskId}")
    public Result<JsonNode> uploadStatus(@PathVariable Long taskId) {
        return Result.ok(productService.uploadStatus(taskId));
    }
}
