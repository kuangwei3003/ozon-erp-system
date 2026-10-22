package com.kw.erp.controller.api;

import com.kw.erp.common.Result;
import com.kw.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表盘统计接口
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }
}
