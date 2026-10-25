package com.aether.ms_calculator.shared.security;

public final class CalculatorPaths {

  public static final String CALCULATOR_GAS_AUTHORITY = "calculator:gas";

  private CalculatorPaths() {}

  public static boolean isAuthenticationKeyRoute(String path) {
    return "/calculator/roi".equals(path) || "/calculator/payback".equals(path);
  }

  public static boolean requiresCalculatorGasAuthority(String path) {
    return "/calculator/gases".equals(path) || "/calculator/reduction/forecast".equals(path);
  }
}
