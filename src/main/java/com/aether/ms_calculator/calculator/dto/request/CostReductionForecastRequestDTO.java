package com.aether.ms_calculator.calculator.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Schema(name = "CostReductionForecastRequest")
public class CostReductionForecastRequestDTO {

  @JsonProperty("monthlyCost")
  @Schema(
      description = "Current monthly cost in BRL, zero or positive, at most two decimal places.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minimum = "0",
      example = "100000")
  private BigDecimal monthlyCost;

  @JsonProperty("reductionPercentage")
  @Schema(
      description = "Expected cost reduction, an integer from 0 to 50.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minimum = "0",
      maximum = "50",
      example = "15")
  private Integer reductionPercentage;

  @JsonProperty("periodMonths")
  @Schema(
      description = "Projection length in months, an integer from 1 to 60.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minimum = "1",
      maximum = "60",
      example = "12")
  private Integer periodMonths;

  private final Set<String> unknownFields = new LinkedHashSet<>();

  @JsonAnySetter
  public void addUnknownField(String name, Object value) {
    unknownFields.add(name);
  }

  public BigDecimal getMonthlyCost() {
    return monthlyCost;
  }

  public Integer getReductionPercentage() {
    return reductionPercentage;
  }

  public Integer getPeriodMonths() {
    return periodMonths;
  }

  @JsonIgnore
  public Set<String> getUnknownFields() {
    return Collections.unmodifiableSet(unknownFields);
  }
}
