package com.kw.erp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kw.erp.entity.ReturnItem;

/**
 * 退货管理
 */
public interface ReturnService {

    /** 从 Ozon 拉取退货信息并写入本地表 */
    int sync();

    IPage<ReturnItem> page(long current, long size, String status, String keyword);
}
