package com.aether.ms_calculator.shared.security;

public final class CalculatorPaths {

  public static final String CALCULATOR_GAS_AUTHORITY = "calculator:gas";
  public static final String CALCULATOR_ROI_AUTHORITY = "calculator:roi";
  public static final String BASE_PATH = "/api/calculator";
  public static final String ROI_PATH = BASE_PATH + "/roi";
  public static final String PAYBACK_PATH = BASE_PATH + "/payback";
  public static final String GASES_PATH = BASE_PATH + "/gases";
  public static final String REDUCTION_PATH = BASE_PATH + "/reduction";
  public static final String REDUCTION_FORECAST_PATH = REDUCTION_PATH + "/forecast";
  public static final String COST_REDUCTION_PATH = BASE_PATH + "/cost-reduction";
  public static final String COST_REDUCTION_FORECAST_PATH = COST_REDUCTION_PATH + "/forecast";

  private CalculatorPaths() {}

  public static boolean isAuthenticationKeyRoute(String path) {
    return ROI_PATH.equals(path) || PAYBACK_PATH.equals(path);
  }

  public static boolean requiresCalculatorRoiAuthority(String path) {
    return path != null
        && (path.equals(COST_REDUCTION_FORECAST_PATH) || path.startsWith(COST_REDUCTION_PATH + "/"));
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
        || path.startsWith(GASES_PATH + "/")
        || path.equals(REDUCTION_PATH)
        || path.startsWith(REDUCTION_PATH + "/")
        || path.equals(COST_REDUCTION_PATH)
        || path.startsWith(COST_REDUCTION_PATH + "/");
  }
}
