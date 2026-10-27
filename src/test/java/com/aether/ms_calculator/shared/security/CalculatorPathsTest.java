package com.aether.ms_calculator.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalculatorPathsTest {

  @Test
  void investmentRoutesStayOnAuthenticationKey() {
    assertThat(CalculatorPaths.isAuthenticationKeyRoute(CalculatorPaths.ROI_PATH)).isTrue();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute(CalculatorPaths.PAYBACK_PATH)).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority(CalculatorPaths.ROI_PATH)).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority(CalculatorPaths.PAYBACK_PATH))
        .isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority(CalculatorPaths.ROI_PATH)).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority(CalculatorPaths.PAYBACK_PATH))
        .isFalse();
  }

  @Test
  void gasesAndForecastRequireCalculatorGas() {
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority(CalculatorPaths.GASES_PATH)).isTrue();
    assertThat(
            CalculatorPaths.requiresCalculatorGasAuthority(
                CalculatorPaths.REDUCTION_FORECAST_PATH))
        .isTrue();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority(CalculatorPaths.GASES_PATH))
        .isFalse();
    assertThat(
            CalculatorPaths.requiresCalculatorRoiAuthority(
                CalculatorPaths.REDUCTION_FORECAST_PATH))
        .isFalse();
  }

  @Test
  void costReductionForecastRequiresCalculatorRoiNotGas() {
    assertThat(
            CalculatorPaths.requiresCalculatorRoiAuthority(
                CalculatorPaths.COST_REDUCTION_FORECAST_PATH))
        .isTrue();
    assertThat(
            CalculatorPaths.requiresCalculatorGasAuthority(
                CalculatorPaths.COST_REDUCTION_FORECAST_PATH))
        .isFalse();
  }

  @Test
  void unknownGasesOrReductionPathRequiresCalculatorGas() {
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/api/calculator/reduction/other")).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/api/calculator/gases/extra")).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/api/calculator/reduction/other")).isFalse();
  }

  @Test
  void nonCalculatorPathsDoNotRequireCalculatorGas() {
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/api/auth/login")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/v3/api-docs")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/other")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/api/auth/login")).isFalse();
  }
}
