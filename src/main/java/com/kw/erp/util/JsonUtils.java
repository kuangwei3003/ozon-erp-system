package com.kw.erp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Ozon 报文解析工具：兼容多种字段命名与日期格式，避免因单个字段格式异常导致整次同步失败
 */
public final class JsonUtils {

    /** 用于把本地对象序列化成与 Ozon 一致的 snake_case JSON */
    private static final ObjectMapper SNAKE = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

    private JsonUtils() {
    }

    /** 取第一个存在且非空的字符串值 */
    public static String text(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            JsonNode child = node.path(name);
            if (child.isValueNode() && !child.asText().isBlank()) {
                return child.asText();
            }
        }
        return null;
    }

    public static Long longVal(JsonNode node, String... names) {
        String v = text(node, names);
        if (v == null) {
            return null;
        }
        try {
            return (long) Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer intVal(JsonNode node, String... names) {
        Long v = longVal(node, names);
        return v == null ? null : v.intValue();
    }

    /** 逐层下钻，任一层缺失返回 MissingNode（不会抛异常） */
    public static JsonNode path(JsonNode node, String... names) {
        JsonNode current = node == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : node;
        for (String name : names) {
            current = current.path(name);
        }
        return current;
    }

    /**
     * 解析 Ozon 时间：支持
     * "2026-01-05T12:00:00Z" / "2026-01-05T12:00:00.000Z" / "2026-01-05T12:00:00+03:00"
     * / "2026-01-05" / "2006-01-02 15:04:05"
     */
    public static LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank() || "0001-01-01T00:00:00Z".equals(raw) || "0001-01-01".equals(raw)) {
            return null;
        }
        String v = raw.trim();
        try {
            return OffsetDateTime.parse(v).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignore) {
            // 继续尝试其他格式
        }
        try {
            return Instant.parse(v).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignore) {
            // 继续尝试其他格式
        }
        try {
            return LocalDateTime.parse(v.replace(' ', 'T'));
        } catch (DateTimeParseException ignore) {
            // 继续尝试其他格式
        }
        try {
            return LocalDate.parse(v.substring(0, 10)).atStartOfDay();
        } catch (Exception ignore) {
            return null;
        }
    }

    /** 解析 epoch 秒（聊天接口使用） */
    public static LocalDateTime parseEpochSecond(Long epochSecond) {
        if (epochSecond == null || epochSecond <= 0) {
            return null;
        }
        return Instant.ofEpochSecond(epochSecond).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDateTime parseDate(JsonNode node, String... names) {
        return parseDate(text(node, names));
    }

    public static String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return SNAKE.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /** 数组节点转 List<JsonNode>，非数组返回空列表 */
    public static java.util.List<JsonNode> elements(JsonNode node) {
        if (node == null || !node.isArray()) {
            return java.util.List.of();
        }
        java.util.List<JsonNode> list = new java.util.ArrayList<>();
        node.forEach(list::add);
        return list;
    }
}
