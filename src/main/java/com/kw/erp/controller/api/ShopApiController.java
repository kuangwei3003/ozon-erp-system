package com.kw.erp.controller.api;

import com.kw.erp.common.Result;
import com.kw.erp.entity.ShopConfig;
import com.kw.erp.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店铺设置接口
 */
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopApiController {

    private final ShopService shopService;

    @GetMapping
    public Result<List<ShopConfig>> list() {
        return Result.ok(shopService.list());
    }

    @PostMapping
    public Result<ShopConfig> save(@RequestBody ShopConfig config) {
        return Result.ok(shopService.save(config));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shopService.delete(id);
        return Result.ok();
    }

    /** 密钥是否已就绪 */
    @GetMapping("/ready")
    public Result<Boolean> ready() {
        return Result.ok(shopService.ready());
    }

    /** 连通性测试 */
    @PostMapping("/test")
    public Result<String> test() {
        return Result.ok(shopService.testConnection());
    }
}
