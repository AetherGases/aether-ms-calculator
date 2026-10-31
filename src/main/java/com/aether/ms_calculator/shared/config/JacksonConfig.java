package com.aether.ms_calculator.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  /**
   * Typed request DTOs must reject what the hub contract rejects: 15.0 or "15" is not an integer,
   * and "100" or true is not a number. Jackson coerces all of these by default.
   */
  @Bean
  public JsonMapperBuilderCustomizer strictScalarCoercionCustomizer() {
    return JacksonConfig::strictScalarCoercion;
  }

  public static JsonMapper.Builder strictScalarCoercion(JsonMapper.Builder builder) {
    return builder
        .withCoercionConfig(
            LogicalType.Integer,
            config ->
                config
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
        .withCoercionConfig(
            LogicalType.Float,
            config ->
                config
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
  }
}
