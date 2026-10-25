package com.aether.ms_calculator.calculator.dto.input;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public class CurrentEmissionInputDTO {

  @JsonProperty("gas")
  private JsonNode gas;

  @JsonProperty("volume_co2e")
  private JsonNode volumeCo2e;

  @JsonProperty("scope")
  private JsonNode scope;

  private final Map<String, JsonNode> unknownFields = new LinkedHashMap<>();

  @JsonAnySetter
  public void addUnknownField(String name, JsonNode value) {
    unknownFields.put(name, value);
  }

  public JsonNode getGas() {
    return gas;
  }

  public JsonNode getVolumeCo2e() {
    return volumeCo2e;
  }

  public JsonNode getScope() {
    return scope;
  }

  public Map<String, JsonNode> getUnknownFields() {
    return Collections.unmodifiableMap(unknownFields);
  }
}
