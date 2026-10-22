package com.kw.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时同步：默认关闭，通过 ozon.sync.enabled=true 开启
 * （未开启时仍可在各页面点击「同步」按钮手动拉取）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final ProductService productService;
    private final PriceStockService priceStockService;
    private final OrderService orderService;
    private final ReturnService returnService;
    private final ChatService chatService;

    @Value("${ozon.sync.enabled:false}")
    private boolean enabled;

    /** 每轮只同步一部分模块，避免单次任务占用过多接口配额 */
    @Scheduled(fixedDelayString = "${ozon.sync.interval-ms:1800000}", initialDelayString = "${ozon.sync.initial-delay-ms:60000}")
    public void syncAll() {
        if (!enabled) {
            return;
        }
        run("商品", productService::sync);
        run("价格库存", priceStockService::sync);
        run("订单", () -> orderService.sync(null));
        run("退货", returnService::sync);
        run("聊天", chatService::syncRooms);
    }

    private void run(String name, SyncTask task) {
        try {
            int count = task.run();
            log.info("定时同步[{}]完成：{} 条", name, count);
        } catch (Exception e) {
            log.warn("定时同步[{}]失败：{}", name, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface SyncTask {
        int run();
    }
}
