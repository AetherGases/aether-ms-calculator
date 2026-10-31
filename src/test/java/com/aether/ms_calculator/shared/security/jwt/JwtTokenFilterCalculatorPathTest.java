package com.aether.ms_calculator.shared.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;

class JwtTokenFilterCalculatorPathTest {

  private final JwtTokenFilter filter =
      new JwtTokenFilter(mock(JwtTokenProvider.class), mock(HandlerExceptionResolver.class));

  @Test
  void skipsAuthenticationKeyRoutes() {
    assertThat(filter.shouldNotFilter(request("/roi"))).isTrue();
    assertThat(filter.shouldNotFilter(request("/payback"))).isTrue();
  }

  @Test
  void filtersJwtRoutes() {
    assertThat(filter.shouldNotFilter(request("/gases"))).isFalse();
    assertThat(filter.shouldNotFilter(request("/reduction/forecast"))).isFalse();
    assertThat(filter.shouldNotFilter(request("/cost-reduction/forecast"))).isFalse();
  }

  private static MockHttpServletRequest request(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setServletPath(path);
    return request;
  }
}
