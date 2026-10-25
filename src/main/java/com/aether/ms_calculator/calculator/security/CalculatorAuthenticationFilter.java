package com.aether.ms_calculator.calculator.security;

import com.aether.ms_calculator.calculator.config.CalculatorAuthenticationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class CalculatorAuthenticationFilter extends OncePerRequestFilter {

  static final String AUTHENTICATION_KEY_HEADER = "authentication-key";

  private final CalculatorAuthenticationProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String path = request.getRequestURI();
    return !"/calculator/roi".equals(path) && !"/calculator/payback".equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String providedKey = request.getHeader(AUTHENTICATION_KEY_HEADER);

    if (providedKey == null) {
      writeUnauthorized(response, "Missing authentication key.");
      return;
    }

    String configuredKey = properties.getInternalKey();
    if (configuredKey.isEmpty()) {
      writeServiceUnavailable(response);
      return;
    }

    if (!constantTimeEquals(configuredKey, providedKey)) {
      writeUnauthorized(response, "Invalid authentication key.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, String> body = new LinkedHashMap<>();
    body.put("error_code", "unauthorized");
    body.put("message", message);
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, String> body = new LinkedHashMap<>();
    body.put("message", "Calculator authentication is not configured.");
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  static boolean constantTimeEquals(String expected, String actual) {
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
    if (expectedBytes.length != actualBytes.length) {
      return MessageDigest.isEqual(expectedBytes, expectedBytes) && false;
    }
    return MessageDigest.isEqual(expectedBytes, actualBytes);
  }
}
