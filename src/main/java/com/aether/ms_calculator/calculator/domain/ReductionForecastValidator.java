package com.aether.ms_calculator.calculator.domain;

import com.aether.ms_calculator.shared.exceptions.BadRequestException;
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

  public ReductionForecastParsed validate(ReductionForecastRequest request) {
    if (request == null) {
      throw invalid("The request must be a JSON object.");
    }

    rejectUnknownFields(request.unknownFields(), REQUEST_FIELDS, "request");

    int years = parsePositiveInteger(request.years(), "years");

    ReductionForecastRequest.CurrentEmission current = request.currentEmission();
    if (current == null) {
      throw invalid("\"current_emission\" must be a JSON object.");
    }
    rejectUnknownFields(current.unknownFields(), CURRENT_EMISSION_FIELDS, "current_emission");

    int currentGasId = parsePositiveInteger(current.gas(), "current_emission.gas");
    BigDecimal volumeCo2e =
        parseNonNegativeNumber(current.volumeCo2e(), "current_emission.volume_co2e");
    int scope = parseScope(current.scope());

    int greenGasId = parsePositiveInteger(request.greenGas(), "green_gas");

    return new ReductionForecastParsed(years, currentGasId, volumeCo2e, scope, greenGasId);
  }

  private void rejectUnknownFields(Set<String> fields, List<String> allowed, String label) {
    if (fields == null || fields.isEmpty()) {
      return;
    }
    Set<String> unknown = new TreeSet<>(fields);
    throw invalid(
        "The "
            + label
            + " takes no "
            + String.join(", ", unknown)
            + ". It accepts: "
            + String.join(", ", allowed)
            + ".");
  }

  private int parsePositiveInteger(Integer value, String field) {
    if (value == null) {
      throw invalid("\"" + field + "\" must be an integer, got None.");
    }
    if (value < 1) {
      throw invalid("\"" + field + "\" must be an integer of at least 1, got " + value + ".");
    }
    return value;
  }

  private int parseScope(Integer value) {
    if (value == null) {
      throw invalid("\"current_emission.scope\" must be 1, 2, or 3, got None.");
    }
    if (!ALLOWED_SCOPES.contains(value)) {
      throw invalid("\"current_emission.scope\" must be 1, 2, or 3, got " + value + ".");
    }
    return value;
  }

  private BigDecimal parseNonNegativeNumber(BigDecimal value, String field) {
    if (value == null) {
      throw invalid("\"" + field + "\" must be a number, got None.");
    }
    if (value.signum() < 0) {
      throw invalid(
          "\"" + field + "\" must be zero or positive, got " + value.toPlainString() + ".");
    }
    return value;
  }

  private static BadRequestException invalid(String message) {
    return BadRequestException.withLiteralMessage(message, "validation_error");
  }
}
