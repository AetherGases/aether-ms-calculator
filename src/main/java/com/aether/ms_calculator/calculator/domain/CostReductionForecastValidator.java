package com.aether.ms_calculator.calculator.domain;

import com.aether.ms_calculator.shared.exceptions.BadRequestException;
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

  public CostReductionForecastParsed validate(CostReductionForecastRequest request) {
    if (request == null) {
      throw invalid("The request must be a JSON object.");
    }

    rejectUnknownFields(request.unknownFields());

    BigDecimal monthlyCost = parseMonthlyCost(request.monthlyCost());
    int reductionPercentage = parseReductionPercentage(request.reductionPercentage());
    int periodMonths = parsePeriodMonths(request.periodMonths());

    return new CostReductionForecastParsed(monthlyCost, reductionPercentage, periodMonths);
  }

  private void rejectUnknownFields(Set<String> fields) {
    if (fields == null || fields.isEmpty()) {
      return;
    }
    Set<String> unknown = new TreeSet<>(fields);
    throw invalid(
        "The request takes no "
            + String.join(", ", unknown)
            + ". It accepts: "
            + String.join(", ", REQUEST_FIELDS)
            + ".");
  }

  private BigDecimal parseMonthlyCost(BigDecimal value) {
    if (value == null) {
      throw invalid("\"monthlyCost\" must be a number, got None.");
    }
    if (value.signum() < 0) {
      throw invalid("\"monthlyCost\" must be zero or positive, got " + value.toPlainString() + ".");
    }
    BigDecimal normalized = value.stripTrailingZeros();
    if (normalized.scale() > MAX_MONEY_SCALE) {
      throw invalid(
          "\"monthlyCost\" must have at most two decimal places, got "
              + value.toPlainString()
              + ".");
    }
    if (normalized.scale() < 0) {
      return normalized.setScale(0);
    }
    return normalized;
  }

  private int parseReductionPercentage(Integer value) {
    if (value == null) {
      throw invalid("\"reductionPercentage\" must be an integer, got None.");
    }
    if (value < 0 || value > MAX_REDUCTION_PERCENTAGE) {
      throw invalid(
          "\"reductionPercentage\" must be between 0 and "
              + MAX_REDUCTION_PERCENTAGE
              + ", got "
              + value
              + ".");
    }
    return value;
  }

  private int parsePeriodMonths(Integer value) {
    if (value == null) {
      throw invalid("\"periodMonths\" must be an integer, got None.");
    }
    if (value < MIN_PERIOD_MONTHS || value > MAX_PERIOD_MONTHS) {
      throw invalid(
          "\"periodMonths\" must be between "
              + MIN_PERIOD_MONTHS
              + " and "
              + MAX_PERIOD_MONTHS
              + ", got "
              + value
              + ".");
    }
    return value;
  }

  private static BadRequestException invalid(String message) {
    return BadRequestException.withLiteralMessage(message, "validation_error");
  }
}
