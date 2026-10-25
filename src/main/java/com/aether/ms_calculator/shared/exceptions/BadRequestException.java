package com.aether.ms_calculator.shared.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends MsCalculatorException {
  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  private BadRequestException(String message, boolean messageIsKey, String errorCode) {
    super(message, messageIsKey, errorCode);
  }

  public static BadRequestException withLiteralMessage(String message, String errorCode) {
    return new BadRequestException(message, false, errorCode);
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}
