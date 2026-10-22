package com.kw.erp.exception;

import com.kw.erp.client.dto.attr.AttributeReq;
import com.kw.erp.controller.api.AttributeApiController;
import com.kw.erp.service.AttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST 错误契约测试：异常类型 → HTTP 状态码 + 统一 Result 结构
 */
@ExtendWith(MockitoExtension.class)
class ApiErrorContractTest {

    @Mock
    private AttributeService attributeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AttributeApiController(attributeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingRequiredParameterIs400() throws Exception {
        mockMvc.perform(get("/api/attributes/values"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少必填参数：attr_id"));
    }

    @Test
    void wrongParameterTypeIs400() throws Exception {
        mockMvc.perform(get("/api/attributes/values?attr_id=abc&category_id=1&type_id=2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("参数类型不正确：attr_id"));
    }

    @Test
    void domainExceptionIs400WithReadableMessage() throws Exception {
        when(attributeService.categoryAttributes(any(AttributeReq.class)))
                .thenThrow(new DomainException("未找到可用的 Ozon Client-Id，请在配置文件或店铺设置中填写"));

        mockMvc.perform(get("/api/attributes/category/17028920/91951"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("未找到可用的 Ozon Client-Id，请在配置文件或店铺设置中填写"));
    }

    @Test
    void ozonApiExceptionIs502() throws Exception {
        when(attributeService.categoryTree())
                .thenThrow(new OzonApiException(429, "TOO_MANY_REQUESTS"));

        mockMvc.perform(get("/api/attributes/category-tree"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("Ozon接口错误: TOO_MANY_REQUESTS"));
    }

    @Test
    void unexpectedExceptionNeverLeaksStack() throws Exception {
        when(attributeService.categoryTree()).thenThrow(new IllegalStateException("内部细节"));

        mockMvc.perform(get("/api/attributes/category-tree"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
