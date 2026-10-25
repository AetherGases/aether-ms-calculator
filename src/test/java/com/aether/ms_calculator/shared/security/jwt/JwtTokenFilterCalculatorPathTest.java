package com.aether.ms_calculator.shared.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenFilterCalculatorPathTest {

  @Test
  void shouldNotFilter_roiAndPayback() {
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/roi")).isTrue();
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/payback")).isTrue();
  }

  @Test
  void shouldNotFilter_gasesAndForecast() {
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/gases")).isFalse();
    assertThat(JwtTokenFilter.shouldNotFilterPath("/calculator/reduction/forecast")).isFalse();
  }
}
