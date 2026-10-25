package com.aether.ms_calculator.calculator.domain;

import com.aether.ms_calculator.shared.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class ReductionForecastValidator {

  private static final List<String> REQUEST_FIELDS = List.of("years", "current_emission", "green_gas");
  private static final List<String> CURRENT_EMISSION_FIELDS = List.of("gas", "volume_co2e", "scope");
  private static final Set<Integer> ALLOWED_SCOPES = Set.of(1, 2, 3);

  public ReductionForecastParsed validate(JsonNode request) {
    if (request == null || !request.isObject()) {
      throw invalid("The request must be a JSON object.");
    }

    rejectUnknownFields(request, REQUEST_FIELDS, "request");

    JsonNode yearsNode = request.get("years");
    int years = parsePositiveInteger(yearsNode, "years");

    JsonNode currentNode = request.get("current_emission");
    if (currentNode == null || currentNode.isNull() || !currentNode.isObject()) {
      throw invalid("\"current_emission\" must be a JSON object.");
    }
    rejectUnknownFields(currentNode, CURRENT_EMISSION_FIELDS, "current_emission");

    int currentGasId = parsePositiveInteger(currentNode.get("gas"), "current_emission.gas");
    BigDecimal volumeCo2e =
        parseNonNegativeNumber(currentNode.get("volume_co2e"), "current_emission.volume_co2e");
    int scope = parseScope(currentNode.get("scope"));

    int greenGasId = parsePositiveInteger(request.get("green_gas"), "green_gas");

    return new ReductionForecastParsed(years, currentGasId, volumeCo2e, scope, greenGasId);
  }

  private void rejectUnknownFields(JsonNode request, List<String> allowed, String label) {
    Set<String> unknown = new TreeSet<>();
    request.fieldNames().forEachRemaining(field -> {
      if (!allowed.contains(field)) {
        unknown.add(field);
      }
    });
    if (!unknown.isEmpty()) {
      throw invalid(
          "The "
              + label
              + " takes no "
              + String.join(", ", unknown)
              + ". It accepts: "
              + String.join(", ", allowed)
              + ".");
    }
  }

  private int parsePositiveInteger(JsonNode node, String field) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw invalid("\"" + field + "\" must be an integer, got None.");
    }
    if (node.isBoolean() || !node.isNumber() || !node.isIntegralNumber()) {
      throw invalid("\"" + field + "\" must be an integer, got " + PythonRepr.of(node) + ".");
    }
    long value = node.longValue();
    if (value < 1) {
      throw invalid("\"" + field + "\" must be an integer of at least 1, got " + PythonRepr.of(node) + ".");
    }
    if (value > Integer.MAX_VALUE) {
      throw invalid("\"" + field + "\" must be an integer, got " + PythonRepr.of(node) + ".");
    }
    return (int) value;
  }

  private int parseScope(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw invalid("\"current_emission.scope\" must be 1, 2, or 3, got None.");
    }
    if (node.isBoolean()
        || !node.isNumber()
        || !node.isIntegralNumber()
        || !node.canConvertToInt()) {
      throw invalid("\"current_emission.scope\" must be 1, 2, or 3, got " + PythonRepr.of(node) + ".");
    }
    int scope = node.intValue();
    if (!ALLOWED_SCOPES.contains(scope)) {
      throw invalid("\"current_emission.scope\" must be 1, 2, or 3, got " + PythonRepr.of(node) + ".");
    }
    return scope;
  }

  private BigDecimal parseNonNegativeNumber(JsonNode node, String field) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw invalid("\"" + field + "\" must be a number, got None.");
    }
    if (node.isBoolean() || !node.isNumber()) {
      throw invalid("\"" + field + "\" must be a number, got " + PythonRepr.of(node) + ".");
    }
    if (node.isFloatingPointNumber() && !Double.isFinite(node.doubleValue())) {
      throw invalid("\"" + field + "\" must be a number, got " + PythonRepr.of(node) + ".");
    }
    BigDecimal value = node.decimalValue();
    if (value.signum() < 0) {
      throw invalid("\"" + field + "\" must be zero or positive, got " + PythonRepr.of(node) + ".");
    }
    return value;
  }

  private static BadRequestException invalid(String message) {
    return BadRequestException.withLiteralMessage(message, "validation_error");
  }
}
