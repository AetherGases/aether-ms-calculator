package com.aether.ms_calculator.shared.security;

import jakarta.servlet.http.HttpServletRequest;

public final class CalculatorPaths {

  public static final String ROI_PATH = "/roi";
  public static final String PAYBACK_PATH = "/payback";

  private CalculatorPaths() {}

  public static boolean isAuthenticationKeyRoute(String path) {
    return ROI_PATH.equals(path) || PAYBACK_PATH.equals(path);
  }

  public static String resolvePath(HttpServletRequest request) {
    String path = request.getServletPath();

    if (path == null || path.isEmpty()) return request.getRequestURI();

    return path;
  }
}
