package com.aether.ms_calculator.calculator.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FutureEmissionOutputDTO(
    int gas,
    @JsonProperty("volume_co2e") double volumeCo2e,
    int scope) {}
