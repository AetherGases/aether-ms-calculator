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
        if (node.isObject()) {
            return formatObject(node);
        }
        return node.toString();
    }

    private static String formatObject(JsonNode object) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        object.fieldNames()
                .forEachRemaining(name -> joiner.add(quote(name) + ": " + of(object.get(name))));
        return joiner.toString();
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
        return Double.toString(value);
    }

    private static String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
