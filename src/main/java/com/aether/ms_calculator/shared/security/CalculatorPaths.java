package com.aether.ms_calculator.shared.security;

public final class CalculatorPaths {

  public static final String CALCULATOR_GAS_AUTHORITY = "calculator:gas";

  private CalculatorPaths() {}

  public static boolean isAuthenticationKeyRoute(String path) {
    return "/calculator/roi".equals(path) || "/calculator/payback".equals(path);
  }

  public static boolean requiresCalculatorGasAuthority(String path) {
    return isCalculatorPath(path) && !isAuthenticationKeyRoute(path);
  }

  static boolean isCalculatorPath(String path) {
    return path != null && (path.equals("/calculator") || path.startsWith("/calculator/"));
  }
}
