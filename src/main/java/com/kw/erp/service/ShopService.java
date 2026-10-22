package com.kw.erp.service;

import com.kw.erp.entity.ShopConfig;

import java.util.List;

/**
 * 店铺配置管理与连通性检查
 */
public interface ShopService {

    /** 店铺列表（密钥已脱敏） */
    List<ShopConfig> list();

    /** 新增或更新店铺 */
    ShopConfig save(ShopConfig config);

    void delete(Long id);

    /** 是否已具备调用 Ozon 接口的凭证 */
    boolean ready();

    /** 连通性测试，返回可读结果描述 */
    String testConnection();
}
