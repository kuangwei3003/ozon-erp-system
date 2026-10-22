package com.kw.erp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kw.erp.client.dto.product.ImportProductReq;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 报文解析工具测试：Ozon 字段命名与日期格式多变，这里锁定容错行为
 */
class JsonUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void textReturnsFirstNonBlankCandidate() throws Exception {
        JsonNode node = MAPPER.readTree("{\"a\":\"  \",\"b\":\"B\",\"c\":\"C\"}");
        assertEquals("B", JsonUtils.text(node, "a", "b", "c"));
        assertNull(JsonUtils.text(node, "x", "y"));
        assertNull(JsonUtils.text(null, "a"));
    }

    @Test
    void numericFieldsAreReadable() throws Exception {
        JsonNode node = MAPPER.readTree("{\"sku\":1234567890123,\"ratio\":\"12.0\",\"txt\":\"abc\"}");
        assertEquals(1234567890123L, JsonUtils.longVal(node, "sku"));
        assertEquals(12L, JsonUtils.longVal(node, "ratio"));
        assertEquals(Integer.valueOf(12), JsonUtils.intVal(node, "ratio"));
        assertNull(JsonUtils.longVal(node, "txt"));
        assertNull(JsonUtils.longVal(node, "missing"));
    }

    @Test
    void pathNeverThrowsOnMissingBranch() throws Exception {
        JsonNode node = MAPPER.readTree("{\"result\":{}}");
        assertTrue(JsonUtils.path(node, "result", "items").isMissingNode());
        assertTrue(JsonUtils.path(null, "a").isMissingNode());
        assertFalse(JsonUtils.path(node, "result").isMissingNode());
    }

    @Test
    void parseDateSupportsOzonFormats() {
        LocalDateTime expected = OffsetDateTime.parse("2026-01-05T12:00:00Z")
                .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(expected, JsonUtils.parseDate("2026-01-05T12:00:00Z"));
        assertEquals(expected, JsonUtils.parseDate("2026-01-05T12:00:00.000Z"));
        assertEquals(LocalDateTime.of(2026, 1, 5, 12, 0), JsonUtils.parseDate("2026-01-05T12:00:00"));
        assertEquals(LocalDateTime.of(2026, 1, 5, 15, 4, 5), JsonUtils.parseDate("2026-01-05 15:04:05"));
        assertEquals(LocalDateTime.of(2026, 1, 5, 0, 0), JsonUtils.parseDate("2026-01-05"));
    }

    @Test
    void parseDateIgnoresZeroAndGarbage() {
        assertNull(JsonUtils.parseDate("0001-01-01T00:00:00Z"));
        assertNull(JsonUtils.parseDate("0001-01-01"));
        assertNull(JsonUtils.parseDate(""));
        assertNull(JsonUtils.parseDate(null));
        assertNull(JsonUtils.parseDate("not-a-date"));
    }

    @Test
    void parseEpochSecondHandlesInvalid() {
        assertNull(JsonUtils.parseEpochSecond(null));
        assertNull(JsonUtils.parseEpochSecond(0L));
        LocalDateTime expected = Instant.ofEpochSecond(1_700_000_000L)
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(expected, JsonUtils.parseEpochSecond(1_700_000_000L));
    }

    @Test
    void toJsonUsesSnakeCaseAndSkipsNulls() {
        ImportProductReq.ImportItem item = new ImportProductReq.ImportItem();
        item.setOfferId("A-1");
        item.setName("名称");
        item.setCategoryId(17028920L);

        String json = JsonUtils.toJson(item);
        assertTrue(json.contains("\"offer_id\":\"A-1\""), json);
        assertTrue(json.contains("\"category_id\":17028920"), json);
        assertFalse(json.contains("old_price"), json);
    }

    @Test
    void elementsOnlyAcceptsArrays() throws Exception {
        assertEquals(List.of(), JsonUtils.elements(MAPPER.readTree("{\"a\":1}").path("a")));
        assertEquals(List.of(), JsonUtils.elements(null));
        assertEquals(2, JsonUtils.elements(MAPPER.readTree("[1,2]")).size());
    }
}
