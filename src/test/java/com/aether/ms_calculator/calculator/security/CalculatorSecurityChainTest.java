package com.aether.ms_calculator.calculator.security;

import com.aether.ms_calculator.calculator.config.CalculatorAuthenticationConfiguration;
import com.aether.ms_calculator.calculator.config.CalculatorJsonNodeWebConfiguration;
import com.aether.ms_calculator.calculator.domain.InvestmentCalculator;
import com.aether.ms_calculator.calculator.domain.InvestmentValidator;
import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.aether.ms_calculator.calculator.web.CalculatorExceptionHandler;
import com.aether.ms_calculator.calculator.web.CalculatorPaybackController;
import com.aether.ms_calculator.calculator.web.CalculatorRoiController;
import com.aether.ms_calculator.calculator.web.CalculatorService;
import com.aether.ms_calculator.shared.config.JacksonConfig;
import com.aether.ms_calculator.shared.config.SecurityConfig;
import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import com.aether.ms_calculator.shared.handlers.GlobalExceptionHandler;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = {CalculatorRoiController.class, CalculatorPaybackController.class},
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
@AutoConfigureMockMvc
@Import({
  SecurityConfig.class,
  JacksonConfig.class,
  CalculatorJsonNodeWebConfiguration.class,
  CalculatorAuthenticationConfiguration.class,
  CalculatorService.class,
  InvestmentValidator.class,
  InvestmentCalculator.class,
  CalculatorExceptionHandler.class
})
@TestPropertySource(
    properties = "calculator.authentication.internal-key=" + CalculatorMockMvcSupport.TEST_INTERNAL_KEY)
class CalculatorSecurityChainTest {

  private static final String AUTH_HEADER = "authentication-key";
  private static final String KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;
  private static final String WORKED_EXAMPLE =
      "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";

  @Autowired private MockMvc mockMvc;

  @Autowired private List<FilterRegistrationBean<?>> filterRegistrations;

  @MockitoBean private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void jwtProviderRejectsAnyResolvedToken() {
    when(jwtTokenProvider.resolveToken(any(HttpServletRequest.class)))
        .thenAnswer(
            invocation -> {
              HttpServletRequest request = invocation.getArgument(0);
              String header = request.getHeader("Authorization");
              if (header != null && header.startsWith("Bearer ")) {
                return header.substring("Bearer ".length());
              }
              return null;
            });
    when(jwtTokenProvider.validateToken(anyString()))
        .thenThrow(new UnauthorizedException("exception.jwt.invalid-or-expired"));
  }

  @Test
  void roi_matchingKey_returnsWorkedExampleThroughSecurityChain() throws Exception {
    mockMvc
        .perform(
            post("/calculator/roi")
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WORKED_EXAMPLE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roi_percent").value(33.33))
        .andExpect(jsonPath("$.viable").value(true));
  }

  @Test
  void roi_matchingKey_ignoresInvalidBearer() throws Exception {
    mockMvc
        .perform(
            post("/calculator/roi")
                .header(AUTH_HEADER, KEY)
                .header("Authorization", "Bearer not-a-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WORKED_EXAMPLE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capex").value(150));
  }

  @Test
  void calculatorFilterIsNotRegisteredOnTheServletContainer() {
    List<FilterRegistrationBean<?>> calculatorRegistrations =
        filterRegistrations.stream()
            .filter(registration -> registration.getFilter() instanceof CalculatorAuthenticationFilter)
            .toList();
    assertThat(calculatorRegistrations).isNotEmpty();
    assertThat(calculatorRegistrations).allMatch(registration -> !registration.isEnabled());
  }
}
