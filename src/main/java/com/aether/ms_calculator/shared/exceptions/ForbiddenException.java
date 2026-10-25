package com.aether.ms_calculator.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends MsCalculatorException {

  public ForbiddenException(String message) {
    super(message);
  }

  private ForbiddenException(String message, boolean messageIsKey, String errorCode) {
    super(message, messageIsKey, errorCode);
  }

  public static ForbiddenException withLiteralMessage(String message, String errorCode) {
    return new ForbiddenException(message, false, errorCode);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.FORBIDDEN;
  }
}
