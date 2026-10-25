package com.aether.ms_calculator.calculator.web;

import com.aether.ms_calculator.calculator.domain.InvestmentValidationException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CalculatorRoiController.class, CalculatorPaybackController.class})
public class CalculatorExceptionHandler {

  @ExceptionHandler(InvestmentValidationException.class)
  public ResponseEntity<ObjectNode> handleValidation(InvestmentValidationException ex) {
    ObjectNode body = JsonNodeFactory.instance.objectNode();
    body.put("error_code", "validation_error");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}
