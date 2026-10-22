package com.kw.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ozon Seller API 接入配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ozon")
public class OzonProperties {

    /** API 基址 */
    private String baseUrl = "https://api-seller.ozon.ru";

    /** 店铺 Client-Id */
    private String clientId;

    /** 店铺 Api-Key */
    private String apiKey;

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 5000;

    /** 读取超时（毫秒） */
    private int readTimeoutMs = 30000;
}
