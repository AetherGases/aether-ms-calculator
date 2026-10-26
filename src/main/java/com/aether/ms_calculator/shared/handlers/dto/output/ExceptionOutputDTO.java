package com.aether.ms_calculator.shared.handlers.dto.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExceptionOutputDTO(
    String message,
    String status,
    @JsonProperty("error_code") String errorCode
) {
  public ExceptionOutputDTO(String message, HttpStatus status) {
    this(message, status.name(), null);
  }

  public ExceptionOutputDTO(String message, HttpStatus status, String errorCode) {
    this(message, status.name(), errorCode);
  }
}
