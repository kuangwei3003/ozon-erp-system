package com.kw.erp.service;

import java.util.Map;

/**
 * 仪表盘汇总统计
 */
public interface DashboardService {

    /**
     * 汇总卡片数据：商品数、待发货订单数、退货数、未读聊天数、零库存数等，
     * 以及各模块最近一次同步时间。
     */
    Map<String, Object> overview();
}
