package com.aether.ms_calculator.shared.security;

public final class CalculatorPaths {

  public static final String CALCULATOR_GAS_AUTHORITY = "calculator:gas";
  public static final String CALCULATOR_ROI_AUTHORITY = "calculator:roi";
  public static final String COST_REDUCTION_FORECAST_PATH = "/calculator/cost-reduction/forecast";

  private CalculatorPaths() {}

  public static boolean isAuthenticationKeyRoute(String path) {
    return "/calculator/roi".equals(path) || "/calculator/payback".equals(path);
  }

  public static boolean requiresCalculatorRoiAuthority(String path) {
    return COST_REDUCTION_FORECAST_PATH.equals(path);
  }

  public static boolean requiresCalculatorGasAuthority(String path) {
    if (!isCalculatorPath(path) || isAuthenticationKeyRoute(path)) {
      return false;
    }
    return !requiresCalculatorRoiAuthority(path);
  }

  static boolean isCalculatorPath(String path) {
    return path != null && (path.equals("/calculator") || path.startsWith("/calculator/"));
  }
}
