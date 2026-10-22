package com.kw.erp.client;

import com.kw.erp.config.OzonProperties;
import com.kw.erp.entity.ShopConfig;
import com.kw.erp.exception.DomainException;
import com.kw.erp.mapper.ShopConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 凭证来源测试：配置文件优先，占位符回退店铺表，两者都缺时报错而不是发出无效请求
 */
@ExtendWith(MockitoExtension.class)
class OzonCredentialProviderTest {

    @Mock
    private ShopConfigMapper shopConfigMapper;

    private OzonProperties props(String clientId, String apiKey) {
        OzonProperties props = new OzonProperties();
        props.setClientId(clientId);
        props.setApiKey(apiKey);
        return props;
    }

    private ShopConfig shop(String clientId, String apiKey) {
        ShopConfig shop = new ShopConfig();
        shop.setId(1L);
        shop.setShopName("我的店铺");
        shop.setClientId(clientId);
        shop.setApiKey(apiKey);
        shop.setEnabled(1);
        return shop;
    }

    @Test
    void usesConfiguredCredentialsWithoutTouchingDatabase() {
        OzonCredentialProvider provider = new OzonCredentialProvider(props("123456", "secret-key"), shopConfigMapper);

        assertEquals("123456", provider.clientId());
        assertEquals("secret-key", provider.apiKey());
        assertTrue(provider.isReady());
        verify(shopConfigMapper, never()).selectOne(any());
    }

    @Test
    void fallsBackToEnabledShopWhenPlaceholdersConfigured() {
        when(shopConfigMapper.selectOne(any()))
                .thenReturn(shop("999", "shop-key"));
        OzonCredentialProvider provider =
                new OzonCredentialProvider(props("your-client-id", "your-api-key"), shopConfigMapper);

        assertEquals("999", provider.clientId());
        assertEquals("shop-key", provider.apiKey());
        assertTrue(provider.isReady());
    }

    @Test
    void partialConfigStillResolvesBothValues() {
        when(shopConfigMapper.selectOne(any())).thenReturn(shop(null, "shop-key"));
        OzonCredentialProvider provider = new OzonCredentialProvider(props("123456", null), shopConfigMapper);

        assertEquals("123456", provider.clientId());
        assertEquals("shop-key", provider.apiKey());
    }

    @Test
    void notReadyWhenNothingConfigured() {
        when(shopConfigMapper.selectOne(any())).thenReturn(null);
        OzonCredentialProvider provider = new OzonCredentialProvider(props(null, ""), shopConfigMapper);

        assertFalse(provider.isReady());
        DomainException ex = assertThrows(DomainException.class, provider::clientId);
        assertEquals("未找到可用的 Ozon Client-Id，请在配置文件或店铺设置中填写", ex.getMessage());
        assertEquals("未找到可用的 Ozon Api-Key，请在配置文件或店铺设置中填写",
                assertThrows(DomainException.class, provider::apiKey).getMessage());
    }
}
