package com.aether.ms_calculator.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends MsCalculatorException {
  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  private UnauthorizedException(String message, boolean messageIsKey, String errorCode) {
    super(message, messageIsKey, errorCode);
  }

  public static UnauthorizedException withLiteralMessage(String message, String errorCode) {
    return new UnauthorizedException(message, false, errorCode);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.UNAUTHORIZED;
  }
}
