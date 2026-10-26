package com.aether.ms_calculator.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalculatorPathsTest {

  @Test
  void investmentRoutesStayOnAuthenticationKey() {
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/calculator/roi")).isTrue();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/calculator/payback")).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/calculator/roi")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/calculator/payback")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/calculator/roi")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/calculator/payback")).isFalse();
  }

  @Test
  void gasesAndForecastRequireCalculatorGas() {
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/calculator/gases")).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/calculator/reduction/forecast"))
        .isTrue();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/calculator/gases")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/calculator/reduction/forecast"))
        .isFalse();
  }

  @Test
  void costReductionForecastRequiresCalculatorRoiNotGas() {
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority(
            CalculatorPaths.COST_REDUCTION_FORECAST_PATH))
        .isTrue();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority(
            CalculatorPaths.COST_REDUCTION_FORECAST_PATH))
        .isFalse();
  }

  @Test
  void unknownCalculatorPathRequiresCalculatorGas() {
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/calculator/other")).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/calculator/gases/extra")).isTrue();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/calculator/other")).isFalse();
  }

  @Test
  void nonCalculatorPathsDoNotRequireCalculatorGas() {
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/api/auth/login")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorGasAuthority("/v3/api-docs")).isFalse();
    assertThat(CalculatorPaths.requiresCalculatorRoiAuthority("/api/auth/login")).isFalse();
  }
}
