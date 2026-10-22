package com.kw.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.kw.erp.client.OzonApiClient;
import com.kw.erp.entity.ShopConfig;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ShopConfigMapper;
import com.kw.erp.service.ShopService;
import com.kw.erp.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 店铺配置实现。
 * 说明：列表接口对 Api-Key 做脱敏展示；生产环境建议将 shop_config.api_key 交由
 * 数据库加密或 KMS 管理，本系统默认优先读取配置文件中的密钥。
 */
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopConfigMapper shopConfigMapper;
    private final OzonApiClient client;

    @Override
    public List<ShopConfig> list() {
        List<ShopConfig> shops = shopConfigMapper.selectList(
                new LambdaQueryWrapper<ShopConfig>().orderByAsc(ShopConfig::getId));
        shops.forEach(shop -> shop.setApiKey(mask(shop.getApiKey())));
        return shops;
    }

    @Override
    public ShopConfig save(ShopConfig config) {
        if (config == null || !StringUtils.hasText(config.getShopName())) {
            throw new DomainException("店铺名称不能为空");
        }
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (config.getId() == null) {
            if (!StringUtils.hasText(config.getClientId()) || !StringUtils.hasText(config.getApiKey())) {
                throw new DomainException("新增店铺需要填写 Client-Id 与 Api-Key");
            }
            shopConfigMapper.insert(config);
        } else {
            ShopConfig existing = shopConfigMapper.selectById(config.getId());
            if (existing == null) {
                throw new DomainException("店铺不存在");
            }
            // 页面回显的是脱敏值，未重新填写时保留原密钥
            if (isMasked(config.getApiKey())) {
                config.setApiKey(existing.getApiKey());
            }
            shopConfigMapper.updateById(config);
        }
        ShopConfig saved = shopConfigMapper.selectById(config.getId());
        saved.setApiKey(mask(saved.getApiKey()));
        return saved;
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new DomainException("请选择要删除的店铺");
        }
        shopConfigMapper.deleteById(id);
    }

    @Override
    public boolean ready() {
        return client.isReady();
    }

    @Override
    public String testConnection() {
        JsonNode resp = client.warehouseList();
        int warehouses = JsonUtils.elements(resp == null ? null : resp.path("warehouses")).size();
        if (warehouses == 0) {
            warehouses = JsonUtils.elements(JsonUtils.path(resp, "result", "warehouses")).size();
        }
        return "连接成功，共获取到 " + warehouses + " 个仓库";
    }

    private static String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static boolean isMasked(String value) {
        return value != null && value.contains("****");
    }
}
