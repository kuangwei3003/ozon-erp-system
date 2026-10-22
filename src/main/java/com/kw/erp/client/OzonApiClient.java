package com.kw.erp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kw.erp.client.dto.attr.AttributeReq;
import com.kw.erp.client.dto.attr.AttributeValuesReq;
import com.kw.erp.client.dto.attr.ProductAttributesReq;
import com.kw.erp.client.dto.chat.ChatGetReq;
import com.kw.erp.client.dto.chat.ChatGetResp;
import com.kw.erp.client.dto.chat.ChatListReq;
import com.kw.erp.client.dto.chat.ChatListResp;
import com.kw.erp.client.dto.chat.ChatMessagesReq;
import com.kw.erp.client.dto.chat.ChatSendReq;
import com.kw.erp.client.dto.finance.AnalyticsReq;
import com.kw.erp.client.dto.finance.TransactionReq;
import com.kw.erp.client.dto.finance.TransactionResp;
import com.kw.erp.client.dto.order.CancelReasonReq;
import com.kw.erp.client.dto.order.CancelReasonResp;
import com.kw.erp.client.dto.order.CancelReq;
import com.kw.erp.client.dto.order.FbsGetReq;
import com.kw.erp.client.dto.order.FbsListReq;
import com.kw.erp.client.dto.order.FbsListResp;
import com.kw.erp.client.dto.order.FbsPosting;
import com.kw.erp.client.dto.price.PriceImportReq;
import com.kw.erp.client.dto.price.PriceListReq;
import com.kw.erp.client.dto.product.ImportProductReq;
import com.kw.erp.client.dto.product.ImportStatusReq;
import com.kw.erp.client.dto.product.ProductInfoReq;
import com.kw.erp.client.dto.product.ProductListReq;
import com.kw.erp.client.dto.product.ProductListResp;
import com.kw.erp.client.dto.returnn.ReturnListReq;
import com.kw.erp.client.dto.shipment.LabelReq;
import com.kw.erp.client.dto.shipment.SetTrackedReq;
import com.kw.erp.client.dto.stock.StockInfoReq;
import com.kw.erp.client.dto.stock.StocksReq;
import com.kw.erp.config.OzonProperties;
import com.kw.erp.exception.OzonApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Ozon Seller API 统一客户端：鉴权头注入、超时、错误解析、限流重试
 */
@Slf4j
@Component
public class OzonApiClient {

    private final OzonCredentialProvider credentials;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 捕获 4xx/5xx 响应体，用于解析 Ozon 错误信息 */
    private final ThreadLocal<String> lastErrorBody = new ThreadLocal<>();

    public OzonApiClient(OzonProperties props, OzonCredentialProvider credentials) {
        this.credentials = credentials;
        this.restClient = buildRestClient(props);
    }

    private RestClient buildRestClient(OzonProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        ClientHttpRequestFactory requestFactory = factory;

        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    lastErrorBody.set(null);
                    var response = execution.execute(request, body);
                    if (response.getStatusCode().isError()) {
                        byte[] bytes = response.getBody().readAllBytes();
                        lastErrorBody.set(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                    }
                    return response;
                })
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ==================== 商品 ====================

    /** 商品列表（游标分页） */
    public ProductListResp productList(ProductListReq req) {
        return post("/v3/product/list", req, ProductListResp.class);
    }

    /** 商品信息批量查询（原始报文，字段容错解析） */
    public JsonNode productInfoList(ProductInfoReq req) {
        return post("/v3/product/info/list", req, JsonNode.class);
    }

    /** 上传/更新商品，返回含 task_id 的原始响应 */
    public JsonNode productImport(ImportProductReq req) {
        return post("/v3/product/import", req, JsonNode.class);
    }

    /** 查询上传任务状态 */
    public JsonNode productImportInfo(ImportStatusReq req) {
        return post("/v1/product/import/info", req, JsonNode.class);
    }

    // ==================== 属性与特征 ====================

    /** 类目树（原始响应） */
    public JsonNode categoryTree() {
        return post("/v1/description-category/tree", Map.of("language", "RU"), JsonNode.class);
    }

    /** 类目属性模板 */
    public JsonNode categoryAttribute(AttributeReq req) {
        return post("/v1/description-category/attribute", req, JsonNode.class);
    }

    /** 字典属性值 */
    public JsonNode attributeValues(AttributeValuesReq req) {
        return post("/v1/description-category/attribute/values", req, JsonNode.class);
    }

    /** 商品已填属性/特征 */
    public JsonNode productAttributes(ProductAttributesReq req) {
        return post("/v1/product/attributes", req, JsonNode.class);
    }

    // ==================== 价格与库存 ====================

    /** 价格列表（游标分页） */
    public JsonNode priceList(PriceListReq req) {
        return post("/v5/product/info/prices", req, JsonNode.class);
    }

    /** 更新价格 */
    public JsonNode pricesImport(PriceImportReq req) {
        return post("/v1/product/import/prices", req, JsonNode.class);
    }

    /** 库存查询（游标分页） */
    public JsonNode stocksInfo(StockInfoReq req) {
        return post("/v4/product/info/stocks", req, JsonNode.class);
    }

    /** 更新库存 */
    public JsonNode stocksUpdate(StocksReq req) {
        return post("/v2/products/stocks", req, JsonNode.class);
    }

    // ==================== 退货 ====================

    public JsonNode returnList(ReturnListReq req) {
        return post("/v1/return/list", req, JsonNode.class);
    }

    // ==================== 订单（FBS / rFBS） ====================

    public FbsListResp fbsList(FbsListReq req) {
        return post("/v3/posting/fbs/list", req, FbsListResp.class);
    }

    public FbsPosting fbsGet(FbsGetReq req) {
        return post("/v3/posting/fbs/get", req, FbsPosting.class);
    }

    /** 订单详情原始报文（展示完整字段） */
    public JsonNode fbsGetRaw(FbsGetReq req) {
        return post("/v3/posting/fbs/get", req, JsonNode.class);
    }

    public CancelReasonResp cancelReasons(CancelReasonReq req) {
        return post("/v3/posting/fbs/cancel-reason", req, CancelReasonResp.class);
    }

    public JsonNode cancel(CancelReq req) {
        return post("/v5/posting/cancel", req, JsonNode.class);
    }

    // ==================== 聊天 ====================

    public ChatListResp chatList(ChatListReq req) {
        return post("/v1/chat/list", req, ChatListResp.class);
    }

    public ChatGetResp chatGet(ChatGetReq req) {
        return post("/v1/chat/get", req, ChatGetResp.class);
    }

    public JsonNode chatMessages(ChatMessagesReq req) {
        return post("/v3/chat/messages", req, JsonNode.class);
    }

    public JsonNode chatSend(ChatSendReq req) {
        return post("/v1/chat/send/message", req, JsonNode.class);
    }

    // ==================== 财务与分析 ====================

    public TransactionResp financeTransactions(TransactionReq req) {
        return post("/v1/finance/transaction/list", req, TransactionResp.class);
    }

    public JsonNode analyticsData(AnalyticsReq req) {
        return post("/v1/analytics/data", req, JsonNode.class);
    }

    // ==================== 运单 / 仓库 ====================

    public JsonNode warehouseList() {
        return post("/v1/warehouse/list", Map.of(), JsonNode.class);
    }

    /** 获取包装标签（原始响应含 pdf 基数据） */
    public JsonNode packageLabeling(LabelReq req) {
        return post("/v2/posting/fbs/package-labeling", req, JsonNode.class);
    }

    /** 录入运单号 */
    public JsonNode setTracked(SetTrackedReq req) {
        return post("/v5/posting/fbs/set-tracked", req, JsonNode.class);
    }

    // ==================== 通用调用 ====================

    private <T> T post(String path, Object body, Class<T> type) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return restClient.post()
                        .uri(path)
                        .header("Client-Id", credentials.clientId())
                        .header("Api-Key", credentials.apiKey())
                        .body(body)
                        .retrieve()
                        .body(type);
            } catch (Exception e) {
                String errBody = lastErrorBody.get();
                if (attempt == 1 && shouldRetry(errBody, e)) {
                    log.warn("Ozon API 限流/超时，重试一次 path={} : {}", path, describe(errBody, e));
                    sleepQuietly(1200);
                    continue;
                }
                log.error("Ozon API 调用失败 path={} : {}", path, describe(errBody, e));
                throw new OzonApiException(describe(errBody, e), e);
            }
        }
    }

    /** 当前店铺是否已就绪（已配置可用密钥） */
    public boolean isReady() {
        return credentials.isReady();
    }

    private boolean shouldRetry(String errBody, Exception e) {
        if (errBody != null && errBody.contains("TOO_MANY_REQUESTS")) {
            return true;
        }
        if (e instanceof org.springframework.web.client.HttpStatusCodeException he) {
            return he.getStatusCode().value() == 429 || he.getStatusCode().is5xxServerError();
        }
        return e instanceof IOException;
    }

    /** 解析 Ozon 错误响应中的 message */
    private String describe(String errBody, Exception e) {
        if (errBody != null && !errBody.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(errBody);
                String message = node.path("message").asText(node.path("error").asText(""));
                String code = node.path("code").asText("");
                if (!message.isBlank()) {
                    return code.isBlank() ? message : code + ": " + message;
                }
                return errBody.length() > 300 ? errBody.substring(0, 300) : errBody;
            } catch (Exception ignore) {
                return errBody.length() > 300 ? errBody.substring(0, 300) : errBody;
            }
        }
        return e.getMessage();
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
