package com.aether.ms_calculator.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends MsCalculatorException {
  public ServiceUnavailableException(String message) {
    super(message);
  }

  private ServiceUnavailableException(String message, boolean messageIsKey, String errorCode) {
    super(message, messageIsKey, errorCode);
  }

  public static ServiceUnavailableException withLiteralMessage(String message) {
    return new ServiceUnavailableException(message, false, null);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.SERVICE_UNAVAILABLE;
  }
}
