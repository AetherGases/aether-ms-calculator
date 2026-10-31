package com.aether.ms_calculator.calculator.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Schema(name = "ReductionForecastRequest")
public class ReductionForecastRequestDTO {

  @JsonProperty("years")
  @Schema(
      description = "Forecast horizon in years, an integer of at least 1.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minimum = "1",
      example = "5")
  private Integer years;

  @JsonProperty("current_emission")
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private CurrentEmissionRequestDTO currentEmission;

  @JsonProperty("green_gas")
  @Schema(
      description = "Catalog id of the biogenic gas that replaces the current one.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minimum = "1",
      example = "4")
  private Integer greenGas;

  private final Set<String> unknownFields = new LinkedHashSet<>();

  @JsonAnySetter
  public void addUnknownField(String name, Object value) {
    unknownFields.add(name);
  }

  public Integer getYears() {
    return years;
  }

  public CurrentEmissionRequestDTO getCurrentEmission() {
    return currentEmission;
  }

  public Integer getGreenGas() {
    return greenGas;
  }

  @JsonIgnore
  public Set<String> getUnknownFields() {
    return Collections.unmodifiableSet(unknownFields);
  }

  @Schema(name = "CurrentEmissionRequest", description = "The emission being replaced.")
  public static class CurrentEmissionRequestDTO {

    @JsonProperty("gas")
    @Schema(
        description = "Catalog id of the current, non-biogenic gas.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "1",
        example = "1")
    private Integer gas;

    @JsonProperty("volume_co2e")
    @Schema(
        description = "Yearly volume in tCO2e, zero or positive.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "0",
        example = "10")
    private BigDecimal volumeCo2e;

    @JsonProperty("scope")
    @Schema(
        description = "GHG Protocol scope: 1, 2, or 3.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"1", "2", "3"},
        example = "1")
    private Integer scope;

    private final Set<String> unknownFields = new LinkedHashSet<>();

    @JsonAnySetter
    public void addUnknownField(String name, Object value) {
      unknownFields.add(name);
    }

    public Integer getGas() {
      return gas;
    }

    public BigDecimal getVolumeCo2e() {
      return volumeCo2e;
    }

    public Integer getScope() {
      return scope;
    }

    @JsonIgnore
    public Set<String> getUnknownFields() {
      return Collections.unmodifiableSet(unknownFields);
    }
  }
}
