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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RequiredArgsConstructor
public class AuthenticationKeyFilter extends OncePerRequestFilter {
  public static final String HEADER_NAME = "authentication-key";
  public static final String INTERNAL_CLIENT_AUTHORITY = "INTERNAL_CLIENT";
  private static final String PRINCIPAL = "internal-client";

  private final CalculatorAuthenticationProperties properties;
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !CalculatorPaths.isAuthenticationKeyRoute(CalculatorPaths.resolvePath(request));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      String providedKey = request.getHeader(HEADER_NAME);

      if (providedKey == null) throw new UnauthorizedException("exception.authentication-key.required");

      String configuredKey = properties.getInternalKey();

      if (configuredKey == null || configuredKey.isEmpty()) {
        throw new ServiceUnavailableException("exception.authentication-key.not-configured");
      }

      if (!isValid(configuredKey, providedKey)) throw new UnauthorizedException("exception.authentication-key.invalid");

      SecurityContextHolder.getContext().setAuthentication(
          UsernamePasswordAuthenticationToken.authenticated(
              PRINCIPAL,
              null,
              List.of(new SimpleGrantedAuthority(INTERNAL_CLIENT_AUTHORITY))
          )
      );

      filterChain.doFilter(request, response);
    } catch (MsCalculatorException exception) {
      handlerExceptionResolver.resolveException(request, response, null, exception);
    }
  }

  static boolean isValid(String configuredKey, String providedKey) {
    return MessageDigest.isEqual(
        configuredKey.getBytes(StandardCharsets.UTF_8),
        providedKey.getBytes(StandardCharsets.UTF_8)
    );
  }
}
