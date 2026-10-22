package com.kw.erp.controller.page;

import com.kw.erp.client.OzonApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 页面公共模型：导航高亮与密钥就绪提示
 */
@ControllerAdvice(basePackages = "com.kw.erp.controller.page")
@RequiredArgsConstructor
public class PageModelAdvice {

    private final OzonApiClient client;

    /** 未配置密钥时页面显示引导提示 */
    @ModelAttribute("ozonReady")
    public boolean ozonReady() {
        return client.isReady();
    }
}
