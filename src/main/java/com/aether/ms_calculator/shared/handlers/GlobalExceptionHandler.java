package com.aether.ms_calculator.shared.handlers;

import com.aether.ms_calculator.shared.exceptions.MsCalculatorException;
import com.aether.ms_calculator.shared.handlers.dto.output.ExceptionOutputDTO;
import com.aether.ms_calculator.shared.services.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
  private final MessageService messageService;
  @ExceptionHandler(MsCalculatorException.class)
  public ResponseEntity<ExceptionOutputDTO> handleMsCalculatorException(MsCalculatorException ex){
    String message = ex.isMessageKey()
        ? messageService.getMessage(ex.getMessage())
        : ex.getMessage();
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            message,
            ex.getStatus(),
            ex.getErrorCode()
        ),
        ex.getStatus()
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ExceptionOutputDTO> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    String message = invalidFieldMessage(ex);
    if (message == null) {
      message =
          isWellFormedNonObjectBody(ex)
              ? "The request must be a JSON object."
              : "The request body is not valid JSON.";
    }
    return new ResponseEntity<>(
        new ExceptionOutputDTO(message, HttpStatus.BAD_REQUEST, "validation_error"),
        HttpStatus.BAD_REQUEST);
  }

  /**
   * Typed request DTOs fail Jackson binding on a wrong value type ("15" for an integer). Name the
   * field, the way the domain validators do, instead of reporting a non-object body.
   */
  private static String invalidFieldMessage(HttpMessageNotReadableException ex) {
    for (Throwable current = ex; current != null; current = current.getCause()) {
      if (current instanceof MismatchedInputException mismatch) {
        String field = jsonPath(mismatch);
        if (field.isEmpty()) {
          return null;
        }
        Class<?> target = mismatch.getTargetType();
        if (target == Integer.class || target == int.class) {
          return "\"" + field + "\" must be an integer.";
        }
        if (target != null && Number.class.isAssignableFrom(target)) {
          return "\"" + field + "\" must be a number.";
        }
        if (target != null && !target.getName().startsWith("java.")) {
          return "\"" + field + "\" must be a JSON object.";
        }
        return "\"" + field + "\" has an invalid type.";
      }
      if (current instanceof InputCoercionException coercion) {
        String field = jsonPath(coercion);
        return field.isEmpty() ? null : "\"" + field + "\" is out of range.";
      }
    }
    return null;
  }

  private static String jsonPath(JacksonException exception) {
    StringBuilder path = new StringBuilder();
    for (JacksonException.Reference reference : exception.getPath()) {
      if (reference.getPropertyName() != null) {
        if (!path.isEmpty()) {
          path.append('.');
        }
        path.append(reference.getPropertyName());
      } else if (reference.getIndex() >= 0) {
        path.append('[').append(reference.getIndex()).append(']');
      }
    }
    return path.toString();
  }

  private static boolean isWellFormedNonObjectBody(HttpMessageNotReadableException ex) {
    for (Throwable current = ex; current != null; current = current.getCause()) {
      String name = current.getClass().getName();
      if (name.endsWith(".MismatchedInputException")
          || name.endsWith(".InvalidFormatException")) {
        return true;
      }
    }
    return false;
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ExceptionOutputDTO> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            "The Content-Type must be application/json.",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "unsupported_media_type"),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ExceptionOutputDTO> handleNoResourceFoundException(NoResourceFoundException ex){
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            messageService.getMessage("exception.route.not-found"),
            HttpStatus.NOT_FOUND
        ),
        HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionOutputDTO> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex
  ) {
    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fieldError -> {

          if (fieldError.getCode() != null
              && fieldError.getCode().equals("typeMismatch")) {

            Class<?> requiredType = ex.getBindingResult()
                .getFieldType(fieldError.getField());

            String allowedValues = Arrays.stream(requiredType.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));

            return messageService.getMessage(
                "exception.invalid.enum",
                fieldError.getField(),
                fieldError.getRejectedValue(),
                allowedValues
            );
          }

          return fieldError.getDefaultMessage();
        })
        .toList();

    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            String.join("\n", errors),
            HttpStatus.BAD_REQUEST
        ),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ExceptionOutputDTO> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException exception
  ) {

    if (exception.getRequiredType() != null
        && exception.getRequiredType().isEnum()) {

      String field = exception.getName();
      String invalidValue = String.valueOf(exception.getValue());

      String allowedValues = Arrays.stream(
              exception.getRequiredType().getEnumConstants()
          )
          .map(Object::toString)
          .collect(Collectors.joining(", "));

      String message = messageService.getMessage(
          "exception.invalid.enum",
          field,
          invalidValue,
          allowedValues
      );

      return ResponseEntity.badRequest().body(
          new ExceptionOutputDTO(
              message,
              HttpStatus.BAD_REQUEST
          )
      );
    }

    return ResponseEntity.badRequest().body(
        new ExceptionOutputDTO(
            messageService.getMessage(
                "exception.invalid.request.parameter"
            ),
            HttpStatus.BAD_REQUEST
        )
    );
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ExceptionOutputDTO> handleAuthenticationException(AuthenticationException ex){
    return new ResponseEntity<>(
        new ExceptionOutputDTO(
            ex.getMessage(),
            HttpStatus.FORBIDDEN
        ),
        HttpStatus.FORBIDDEN
    );
  }
}
