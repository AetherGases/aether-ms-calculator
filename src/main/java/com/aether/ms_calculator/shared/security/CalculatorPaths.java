package com.aether.ms_calculator.shared.security;

public final class CalculatorPaths {

  public static final String CALCULATOR_GAS_AUTHORITY = "calculator:gas";
  public static final String CALCULATOR_ROI_AUTHORITY = "calculator:roi";
  public static final String ROI_PATH = "/roi";
  public static final String PAYBACK_PATH = "/payback";
  public static final String GASES_PATH = "/gases";
  public static final String REDUCTION_FORECAST_PATH = "/reduction/forecast";
  public static final String COST_REDUCTION_FORECAST_PATH = "/cost-reduction/forecast";

  private CalculatorPaths() {}

  public static boolean isAuthenticationKeyRoute(String path) {
    return ROI_PATH.equals(path) || PAYBACK_PATH.equals(path);
  }

  public static boolean requiresCalculatorRoiAuthority(String path) {
    return path != null
        && (path.equals(COST_REDUCTION_FORECAST_PATH) || path.startsWith("/cost-reduction/"));
  }

  public static boolean requiresCalculatorGasAuthority(String path) {
    if (!isCalculatorPath(path) || isAuthenticationKeyRoute(path)) {
      return false;
    }
    return !requiresCalculatorRoiAuthority(path);
  }

  static boolean isCalculatorPath(String path) {
    if (path == null) {
      return false;
    }
    return isAuthenticationKeyRoute(path)
        || path.equals(GASES_PATH)
        || path.startsWith("/gases/")
        || path.equals("/reduction")
        || path.startsWith("/reduction/")
        || path.equals("/cost-reduction")
        || path.startsWith("/cost-reduction/");
  }
}
