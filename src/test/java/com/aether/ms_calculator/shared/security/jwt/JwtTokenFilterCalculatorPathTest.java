package com.aether.ms_calculator.shared.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.aether.ms_calculator.shared.security.CalculatorPaths;
import org.junit.jupiter.api.Test;

class JwtTokenFilterCalculatorPathTest {

  @Test
  void shouldNotFilter_roiAndPayback() {
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/roi")).isTrue();
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/payback")).isTrue();
  }

  @Test
  void shouldNotFilter_gasesForecastAndCostReduction() {
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/gases")).isFalse();
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/reduction/forecast")).isFalse();
    assertThat(JwtTokenFilter.shouldNotFilterPath(CalculatorPaths.COST_REDUCTION_FORECAST_PATH))
        .isFalse();
  }
}
