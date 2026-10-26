package com.aether.ms_calculator.calculator.domain;

import com.aether.ms_calculator.shared.exceptions.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class CostReductionForecastValidator {

  private static final List<String> REQUEST_FIELDS =
      List.of("monthlyCost", "reductionPercentage", "periodMonths");
  private static final int MAX_REDUCTION_PERCENTAGE = 50;
  private static final int MIN_PERIOD_MONTHS = 1;
  private static final int MAX_PERIOD_MONTHS = 60;
  private static final int MAX_MONEY_SCALE = 2;

  public CostReductionForecastParsed validate(JsonNode request) {
    if (request == null || !request.isObject()) {
      throw invalid("The request must be a JSON object.");
    }

    rejectUnknownFields(request);

    JsonNode monthlyCostNode = request.get("monthlyCost");
    BigDecimal monthlyCost = parseMonthlyCost(monthlyCostNode);

    JsonNode reductionNode = request.get("reductionPercentage");
    int reductionPercentage = parseReductionPercentage(reductionNode);

    JsonNode periodNode = request.get("periodMonths");
    int periodMonths = parsePeriodMonths(periodNode);

    return new CostReductionForecastParsed(
        monthlyCostNode, reductionNode, periodNode, monthlyCost, reductionPercentage, periodMonths);
  }

  private void rejectUnknownFields(JsonNode request) {
    Set<String> unknown = new TreeSet<>();
    request.fieldNames().forEachRemaining(field -> {
      if (!REQUEST_FIELDS.contains(field)) {
        unknown.add(field);
      }
    });
    if (!unknown.isEmpty()) {
      throw invalid(
          "The request takes no "
              + String.join(", ", unknown)
              + ". It accepts: "
              + String.join(", ", REQUEST_FIELDS)
              + ".");
    }
  }

  private BigDecimal parseMonthlyCost(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw invalid("\"monthlyCost\" must be a number, got None.");
    }
    if (node.isBoolean() || !node.isNumber()) {
      throw invalid("\"monthlyCost\" must be a number, got " + PythonRepr.of(node) + ".");
    }
    if (node.isFloatingPointNumber() && !Double.isFinite(node.doubleValue())) {
      throw invalid("\"monthlyCost\" must be a number, got " + PythonRepr.of(node) + ".");
    }
    BigDecimal value = node.decimalValue();
    if (value.signum() < 0) {
      throw invalid(
          "\"monthlyCost\" must be zero or positive, got " + PythonRepr.of(node) + ".");
    }
    BigDecimal normalized = value.stripTrailingZeros();
    if (normalized.scale() > MAX_MONEY_SCALE) {
      throw invalid(
          "\"monthlyCost\" must have at most two decimal places, got " + PythonRepr.of(node) + ".");
    }
    if (normalized.scale() < 0) {
      return normalized.setScale(0);
    }
    return normalized;
  }

  private int parseReductionPercentage(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw invalid("\"reductionPercentage\" must be an integer, got None.");
    }
    if (node.isBoolean() || !node.isNumber() || !node.isIntegralNumber()) {
      throw invalid(
          "\"reductionPercentage\" must be an integer, got " + PythonRepr.of(node) + ".");
    }
    if (!node.canConvertToInt()) {
      throw invalid(
          "\"reductionPercentage\" must be an integer, got " + PythonRepr.of(node) + ".");
    }
    int value = node.intValue();
    if (value < 0 || value > MAX_REDUCTION_PERCENTAGE) {
      throw invalid(
          "\"reductionPercentage\" must be between 0 and "
              + MAX_REDUCTION_PERCENTAGE
              + ", got "
              + PythonRepr.of(node)
              + ".");
    }
    return value;
  }

  private int parsePeriodMonths(JsonNode node) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw invalid("\"periodMonths\" must be an integer, got None.");
    }
    if (node.isBoolean() || !node.isNumber() || !node.isIntegralNumber()) {
      throw invalid("\"periodMonths\" must be an integer, got " + PythonRepr.of(node) + ".");
    }
    if (!node.canConvertToInt()) {
      throw invalid("\"periodMonths\" must be an integer, got " + PythonRepr.of(node) + ".");
    }
    int value = node.intValue();
    if (value < MIN_PERIOD_MONTHS || value > MAX_PERIOD_MONTHS) {
      throw invalid(
          "\"periodMonths\" must be between "
              + MIN_PERIOD_MONTHS
              + " and "
              + MAX_PERIOD_MONTHS
              + ", got "
              + PythonRepr.of(node)
              + ".");
    }
    return value;
  }

  private static BadRequestException invalid(String message) {
    return BadRequestException.withLiteralMessage(message, "validation_error");
  }
}
