package com.aether.ms_calculator.calculator.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.StringJoiner;

final class PythonRepr {

    private PythonRepr() {}

    static String of(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "None";
        }
        if (node.isBoolean()) {
            return node.booleanValue() ? "True" : "False";
        }
        if (node.isTextual()) {
            return quote(node.textValue());
        }
        if (node.isNumber()) {
            return formatNumber(node);
        }
        if (node.isArray()) {
            return formatArray((ArrayNode) node);
        }
        return node.toString();
    }

    private static String formatArray(ArrayNode array) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (JsonNode element : array) {
            joiner.add(of(element));
        }
        return joiner.toString();
    }

    private static String formatNumber(JsonNode node) {
        if (node.isIntegralNumber() && node.canConvertToLong()) {
            return Long.toString(node.longValue());
        }
        double value = node.doubleValue();
        if (Double.isNaN(value)) {
            return "nan";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "inf";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }
        return trimFloat(value);
    }

    private static String trimFloat(double value) {
        String text = Double.toString(value);
        if (text.contains("E") || text.contains("e")) {
            return text;
        }
        if (text.contains(".")) {
            while (text.endsWith("0")) {
                text = text.substring(0, text.length() - 1);
            }
            if (text.endsWith(".")) {
                text = text.substring(0, text.length() - 1);
            }
        }
        return text;
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
