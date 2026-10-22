package com.kw.erp.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.kw.erp.client.dto.chat.ChatListResp;
import com.kw.erp.client.dto.finance.TransactionResp;
import com.kw.erp.client.dto.order.CancelReasonResp;
import com.kw.erp.client.dto.price.PriceImportReq;
import com.kw.erp.client.dto.product.ImportProductReq;
import com.kw.erp.client.dto.shipment.SetTrackedReq;
import com.kw.erp.client.dto.stock.StocksReq;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DTO 与 Ozon 报文的字段命名对齐测试（全局 Jackson 采用 SNAKE_CASE）
 */
class DtoJsonTest {

    /** 与 application.yml 中的 Jackson 配置保持一致：snake_case + 空字段不输出 */
    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void importProductRequestMatchesOzonShape() throws Exception {
        ImportProductReq req = new ImportProductReq();
        ImportProductReq.ImportItem item = new ImportProductReq.ImportItem();
        item.setOfferId("A-1");
        item.setName("Товар");
        item.setCategoryId(17028920L);
        item.setTypeId(91951L);
        item.setPrice("999.90");
        item.setCurrencyCode("RUB");
        item.setImages(List.of("https://img/1.jpg"));
        item.setAutoBarcodesGenerate(true);

        ImportProductReq.ImportItem.AttrItem attr = new ImportProductReq.ImportItem.AttrItem();
        attr.setId(8229L);
        attr.setComplexId(0L);
        ImportProductReq.ImportItem.AttrItem.Value value = new ImportProductReq.ImportItem.AttrItem.Value();
        value.setValue("Название");
        value.setDictionaryValueId(91435L);
        attr.setValues(List.of(value));
        item.setAttributes(List.of(attr));
        req.setItems(List.of(item));

        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"offer_id\":\"A-1\""), json);
        assertTrue(json.contains("\"category_id\":17028920"), json);
        assertTrue(json.contains("\"dictionary_value_id\":91435"), json);
        assertTrue(json.contains("\"auto_barcodes_generate\":true"), json);
        // 未填写的可选字段不应出现
        assertFalse(json.contains("old_price"), json);
        assertFalse(json.contains("complex_attributes"), json);
    }

    @Test
    void priceAndStockRequestsUseSnakeCase() throws Exception {
        PriceImportReq priceReq = new PriceImportReq();
        PriceImportReq.PriceItem price = new PriceImportReq.PriceItem();
        price.setOfferId("A-1");
        price.setPrice("1000.00");
        price.setCurrencyCode("RUB");
        priceReq.setPrices(List.of(price));
        String priceJson = mapper.writeValueAsString(priceReq);
        assertTrue(priceJson.contains("\"prices\":[{\"offer_id\":\"A-1\""), priceJson);

        StocksReq stocksReq = new StocksReq();
        StocksReq.StockItem stock = new StocksReq.StockItem();
        stock.setSku(1000001L);
        stock.setWarehouseId(922000000001L);
        stock.setStock(10);
        stock.setStatus("src");
        stocksReq.setStocks(List.of(stock));
        String stockJson = mapper.writeValueAsString(stocksReq);
        assertTrue(stockJson.contains("\"warehouse_id\":922000000001"), stockJson);
        assertTrue(stockJson.contains("\"stock\":10"), stockJson);
    }

    @Test
    void setTrackedRequestUsesPostingsArray() throws Exception {
        SetTrackedReq req = new SetTrackedReq();
        SetTrackedReq.Tracking tracking = new SetTrackedReq.Tracking();
        tracking.setPostingNumber("01223344-0001-1");
        tracking.setTrackingNumber("RR123456789RU");
        req.setPostings(List.of(tracking));

        String json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"posting_number\":\"01223344-0001-1\""), json);
        assertTrue(json.contains("\"tracking_number\":\"RR123456789RU\""), json);
    }

    @Test
    void transactionResponseParsesOzonSample() throws Exception {
        String sample = """
                {"result":{"row_count":2,"transactions":[
                  {"operation_id":123,"operation_dt":"2026-01-05 15:04:05","operation_type":"OperationGoodsReceipt",
                   "operation_amount":1234.56,"item":{"sku":"1000001","offer_id":"A-1","name":"Товар"},
                   "postings":[{"posting_number":"01223344-0001-1","scheme":"fbs","order_id":99}],"comment":"c"}
                ]}}
                """;
        TransactionResp resp = mapper.readValue(sample, TransactionResp.class);
        assertEquals(2, resp.getResult().getRowCount());
        TransactionResp.Transaction tx = resp.getResult().getTransactions().get(0);
        assertEquals(123L, tx.getOperationId());
        assertEquals("OperationGoodsReceipt", tx.getOperationType());
        assertEquals(new BigDecimal("1234.56"), tx.getOperationAmount());
        assertEquals("A-1", tx.getItem().getOfferId());
        assertEquals("01223344-0001-1", tx.getPostings().get(0).getPostingNumber());
    }

    @Test
    void cancelReasonResponseParsesOzonSample() throws Exception {
        String sample = """
                {"result":[{"id":1503,"title":"Товар не удалось доставить","description":"desc",
                            "type":23,"pipeline":14,"need_comment_for_seller":true,"need_photo":false}]}
                """;
        CancelReasonResp resp = mapper.readValue(sample, CancelReasonResp.class);
        CancelReasonResp.Reason reason = resp.getResult().get(0);
        assertEquals(1503, reason.getId());
        assertTrue(reason.getNeedCommentForSeller());
        assertFalse(reason.getNeedPhoto());
    }

    @Test
    void chatListResponseParsesOzonSample() throws Exception {
        String sample = """
                {"result":{"chats":[{"chat_id":"c1","chat_status":"open","chat_type":"USER",
                                      "unread_items":3,"updated_ts":1700000000,"last_message_id":42}],
                            "have_more":true,"cursor":"abc"}}
                """;
        ChatListResp resp = mapper.readValue(sample, ChatListResp.class);
        ChatListResp.Chat chat = resp.getResult().getChats().get(0);
        assertEquals("c1", chat.getChatId());
        assertEquals(3, chat.getUnreadItems());
        assertEquals(1700000000L, chat.getUpdatedTs());
        assertTrue(resp.getResult().isHaveMore());
        assertEquals("abc", resp.getResult().getCursor());
    }
}
