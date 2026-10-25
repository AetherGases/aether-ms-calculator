package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

public record CalculateRoiOutputDTO(
    @JsonProperty("capex") JsonNode capex,
    @JsonProperty("wacc_monthly") JsonNode waccMonthly,
    int months,
    @JsonProperty("vp_total") double vpTotal,
    double vpl,
    @JsonProperty("roi_percent") double roiPercent,
    boolean viable) {}
