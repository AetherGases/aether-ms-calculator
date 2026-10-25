package com.aether.ms_calculator.shared.security.jwt;

import com.aether.ms_calculator.shared.exceptions.ForbiddenException;
import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import com.aether.ms_calculator.shared.security.CalculatorPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
  private final JwtTokenProvider tokenProvider;

  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    if (path == null || path.isEmpty()) {
      path = request.getRequestURI();
    }
    return shouldNotFilterPath(path);
  }

  static boolean shouldNotFilterPath(String path) {
    return CalculatorPaths.isAuthenticationKeyRoute(path);
  }

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filter)
      throws IOException, ServletException {
    try {
      String token = tokenProvider.resolveToken((HttpServletRequest) request);

      if (StringUtils.isNotBlank(token) && tokenProvider.validateToken(token)) {
        Authentication authentication = tokenProvider.getAuthentication(token);

        if (authentication != null) {
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }

      enforceCalculatorGasAuthority(request);

      filter.doFilter(request, response);
    } catch (UnauthorizedException | ForbiddenException e) {
      handlerExceptionResolver.resolveException(request, response, null, e);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void enforceCalculatorGasAuthority(HttpServletRequest request) {
    String path = request.getServletPath();
    if (path == null || path.isEmpty()) {
      path = request.getRequestURI();
    }
    if (!CalculatorPaths.requiresCalculatorGasAuthority(path)) {
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw UnauthorizedException.withLiteralMessage("Missing or invalid JWT.", "unauthorized");
    }

    boolean hasGasAuthority =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CalculatorPaths.CALCULATOR_GAS_AUTHORITY::equals);

    if (!hasGasAuthority) {
      throw ForbiddenException.withLiteralMessage(
          "Missing calculator:gas permission.", "forbidden");
    }
  }
}
