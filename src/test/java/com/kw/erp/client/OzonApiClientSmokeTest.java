package com.kw.erp.client;

import com.kw.erp.client.dto.product.ProductListReq;
import com.kw.erp.client.dto.product.ProductListResp;
import com.kw.erp.config.OzonProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ozon 真实接口冒烟测试。
 * 默认禁用：CI 环境没有密钥也没有外网。本地自测时先设置环境变量
 * OZON_CLIENT_ID / OZON_API_KEY，再去掉方法上的 @Disabled。
 */
@Disabled("需要真实 Ozon 密钥与外网访问，仅本地自测时手动开启")
class OzonApiClientSmokeTest {

    private OzonApiClient realClient() {
        OzonProperties props = new OzonProperties();
        props.setClientId(System.getenv("OZON_CLIENT_ID"));
        props.setApiKey(System.getenv("OZON_API_KEY"));
        Assumptions.assumeTrue(props.getClientId() != null && props.getApiKey() != null,
                "未设置 OZON_CLIENT_ID / OZON_API_KEY 环境变量");
        // 环境变量已就绪时不会查询店铺表，因此 mapper 传 null 即可
        return new OzonApiClient(props, new OzonCredentialProvider(props, null));
    }

    @Test
    void productListReturnsItems() {
        ProductListReq req = new ProductListReq();
        req.setFilter(new ProductListReq.Filter());
        req.setLimit(10);

        ProductListResp resp = realClient().productList(req);

        assertNotNull(resp.getResult());
        assertTrue(resp.getResult().getTotal() >= 0);
    }

    @Test
    void warehouseListIsReadable() {
        assertTrue(realClient().warehouseList().has("result"));
    }
}
