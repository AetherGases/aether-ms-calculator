package com.aether.ms_calculator.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CalculatorPathsTest {

  @Test
  void roiAndPayback_areAuthenticationKeyRoutes() {
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/roi")).isTrue();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/payback")).isTrue();
  }

  @Test
  void jwtRoutes_areNotAuthenticationKeyRoutes() {
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/gases")).isFalse();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/reduction/forecast")).isFalse();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/cost-reduction/forecast")).isFalse();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute("/roi/extra")).isFalse();
    assertThat(CalculatorPaths.isAuthenticationKeyRoute(null)).isFalse();
  }

  @Test
  void resolvePath_fallsBackToRequestUriWhenServletPathIsEmpty() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/roi");
    request.setServletPath("");

    assertThat(CalculatorPaths.resolvePath(request)).isEqualTo("/roi");
  }
}
