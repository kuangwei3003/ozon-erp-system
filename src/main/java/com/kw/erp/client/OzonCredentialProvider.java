package com.kw.erp.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kw.erp.config.OzonProperties;
import com.kw.erp.entity.ShopConfig;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ShopConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 凭证来源：优先使用配置文件（application.yml / 环境变量），
 * 未配置时回退到 shop_config 中启用的店铺，从而支持在页面上维护密钥。
 */
@Component
@RequiredArgsConstructor
public class OzonCredentialProvider {

    private static final String PLACEHOLDER = "your-client-id";

    private final OzonProperties props;
    private final ShopConfigMapper shopConfigMapper;

    public String clientId() {
        if (isConfigured(props.getClientId()) && !PLACEHOLDER.equals(props.getClientId())) {
            return props.getClientId();
        }
        ShopConfig shop = enabledShop();
        if (shop != null && isConfigured(shop.getClientId())) {
            return shop.getClientId();
        }
        throw new DomainException("未找到可用的 Ozon Client-Id，请在配置文件或店铺设置中填写");
    }

    public String apiKey() {
        if (isConfigured(props.getApiKey()) && !"your-api-key".equals(props.getApiKey())) {
            return props.getApiKey();
        }
        ShopConfig shop = enabledShop();
        if (shop != null && isConfigured(shop.getApiKey())) {
            return shop.getApiKey();
        }
        throw new DomainException("未找到可用的 Ozon Api-Key，请在配置文件或店铺设置中填写");
    }

    /** 配置文件未提供密钥且店铺表为空时，判定为未就绪 */
    public boolean isReady() {
        try {
            clientId();
            apiKey();
            return true;
        } catch (DomainException e) {
            return false;
        }
    }

    private ShopConfig enabledShop() {
        LambdaQueryWrapper<ShopConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopConfig::getEnabled, 1).orderByAsc(ShopConfig::getId).last("LIMIT 1");
        return shopConfigMapper.selectOne(wrapper);
    }

    private static boolean isConfigured(String value) {
        return StringUtils.hasText(value);
    }
}
