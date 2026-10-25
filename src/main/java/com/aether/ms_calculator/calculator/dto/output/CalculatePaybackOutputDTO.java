package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

public record CalculatePaybackOutputDTO(
    @JsonProperty("capex") JsonNode capex,
    @JsonProperty("wacc_monthly") JsonNode waccMonthly,
    int months,
    @JsonProperty("vp_total") double vpTotal,
    @JsonProperty("payback_months") int paybackMonths,
    boolean viable) {}
