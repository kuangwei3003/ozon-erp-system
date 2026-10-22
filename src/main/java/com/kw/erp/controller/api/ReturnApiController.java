package com.kw.erp.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kw.erp.common.PageResult;
import com.kw.erp.common.Result;
import com.kw.erp.entity.ReturnItem;
import com.kw.erp.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退货接口
 */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnApiController {

    private final ReturnService returnService;

    @PostMapping("/sync")
    public Result<Integer> sync() {
        return Result.ok(returnService.sync());
    }

    @GetMapping
    public Result<PageResult<ReturnItem>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String keyword) {
        IPage<ReturnItem> page = returnService.page(current, size, status, keyword);
        return Result.ok(PageResult.of(page));
    }
}
