package com.easy.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * 操作日志敏感字段脱敏工具。
 * 对 JSON 中的密码、令牌、密钥等字段统一替换为 ***。
 */
public final class SensitiveDataMasker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i).*(password|passwd|pwd|token|secret|authorization|credential|api[-_]?key|access[-_]?key).*");
    private static final Pattern STRING_FALLBACK = Pattern.compile(
            "(?i)(\"(?:password|oldpassword|newpassword|passwd|pwd|token|accesstoken|refreshtoken|"
                    + "secret|authorization|credential|apikey|api_key|accesskey|access_key)\"\\s*:\\s*)"
                    + "\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern JWT_VALUE = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private SensitiveDataMasker() {
    }

    public static String maskJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(raw);
            maskNode(root);
            return maskJwtValue(OBJECT_MAPPER.writeValueAsString(root));
        } catch (Exception ignored) {
            return maskJwtValue(STRING_FALLBACK.matcher(raw).replaceAll("$1\"***\""));
        }
    }

    private static String maskJwtValue(String raw) {
        return JWT_VALUE.matcher(raw).replaceAll("***");
    }

    private static void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            Iterator<String> names = object.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode value = object.get(name);
                if (SENSITIVE_KEY.matcher(name).matches() && value != null && !value.isNull()) {
                    object.set(name, TextNode.valueOf("***"));
                } else {
                    maskNode(value);
                }
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (JsonNode child : array) {
                maskNode(child);
            }
        }
    }
}
