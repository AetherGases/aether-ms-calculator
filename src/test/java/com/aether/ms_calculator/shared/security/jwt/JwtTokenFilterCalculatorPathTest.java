package com.aether.ms_calculator.shared.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.aether.ms_calculator.shared.security.CalculatorPaths;
import org.junit.jupiter.api.Test;

class JwtTokenFilterCalculatorPathTest {

  @Test
  void shouldNotFilter_roiAndPayback() {
    assertThat(JwtTokenFilter.shouldNotFilterPath(CalculatorPaths.ROI_PATH)).isTrue();
    assertThat(JwtTokenFilter.shouldNotFilterPath(CalculatorPaths.PAYBACK_PATH)).isTrue();
  }

  @Test
  void shouldNotFilter_gasesForecastAndCostReduction() {
    assertThat(JwtTokenFilter.shouldNotFilterPath(CalculatorPaths.GASES_PATH)).isFalse();
    assertThat(JwtTokenFilter.shouldNotFilterPath(CalculatorPaths.REDUCTION_FORECAST_PATH))
        .isFalse();
    assertThat(JwtTokenFilter.shouldNotFilterPath(CalculatorPaths.COST_REDUCTION_FORECAST_PATH))
        .isFalse();
  }
}
