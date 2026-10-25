package com.aether.ms_calculator.shared.exceptions;

import org.springframework.http.HttpStatus;

public abstract class MsCalculatorException extends RuntimeException {
  private final boolean messageIsKey;
  private final String errorCode;

  public MsCalculatorException(String message) {
    this(message, true, null);
  }

  public MsCalculatorException(String message, Throwable cause) {
    this(message, cause, true, null);
  }

  protected MsCalculatorException(String message, boolean messageIsKey, String errorCode) {
    super(message);
    this.messageIsKey = messageIsKey;
    this.errorCode = errorCode;
  }

  protected MsCalculatorException(
      String message, Throwable cause, boolean messageIsKey, String errorCode) {
    super(message, cause);
    this.messageIsKey = messageIsKey;
    this.errorCode = errorCode;
  }

  public boolean isMessageKey() {
    return messageIsKey;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public abstract HttpStatus getStatus();
}
