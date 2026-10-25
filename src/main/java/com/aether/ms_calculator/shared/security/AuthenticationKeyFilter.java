package com.aether.ms_calculator.shared.security;

import com.aether.ms_calculator.shared.config.properties.CalculatorAuthenticationProperties;
import com.aether.ms_calculator.shared.exceptions.MsCalculatorException;
import com.aether.ms_calculator.shared.exceptions.ServiceUnavailableException;
import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
public class AuthenticationKeyFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "authentication-key";

  private final CalculatorAuthenticationProperties properties;
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    return !isInvestmentPath(request.getServletPath()) && !isInvestmentPath(request.getRequestURI());
  }

  static boolean isInvestmentPath(String path) {
    return "/calculator/roi".equals(path) || "/calculator/payback".equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      String providedKey = request.getHeader(HEADER_NAME);

      if (providedKey == null) {
        throw UnauthorizedException.withLiteralMessage("Missing authentication key.", "unauthorized");
      }

      String configuredKey = properties.getInternalKey();
      if (configuredKey == null || configuredKey.isEmpty()) {
        throw ServiceUnavailableException.withLiteralMessage(
            "Calculator authentication is not configured.");
      }

      if (!constantTimeEquals(configuredKey, providedKey)) {
        throw UnauthorizedException.withLiteralMessage("Invalid authentication key.", "unauthorized");
      }

      filterChain.doFilter(request, response);
    } catch (MsCalculatorException exception) {
      handlerExceptionResolver.resolveException(request, response, null, exception);
    }
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
