package com.aether.ms_calculator.calculator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aether.ms_calculator.calculator.services.CostReductionForecastService;
import com.aether.ms_calculator.calculator.services.ReductionForecastService;
import com.aether.ms_calculator.shared.security.CalculatorPaths;
import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.GasRepository;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

class CalculatorUnsupportedMediaTypeTests {

  private static final String AUTH_HEADER = AuthenticationKeyFilter.HEADER_NAME;
  private static final String KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;
  private static final String BEARER_GAS = "Bearer employee-with-gas";
  private static final String BEARER_ROI = "Bearer employee-with-roi";
  private static final String COST_REDUCTION_BODY =
      "{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}";
  private static final String ROI_BODY =
      "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";
  private static final String FORECAST_BODY =
      "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4}";

  private final MockMvc investmentMockMvc = CalculatorMockMvcSupport.mockMvc(KEY);
  private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
  private MockMvc forecastMockMvc;
  private MockMvc costReductionMockMvc;

  @BeforeEach
  void setUpForecastMockMvc() {
    ReductionForecastService service =
        new ReductionForecastService(
            mock(GasRepository.class),
            new com.aether.ms_calculator.calculator.domain.ReductionForecastValidator(),
            new com.aether.ms_calculator.calculator.domain.ReductionForecastCalculator());
    forecastMockMvc =
        CalculatorMockMvcSupport.reductionForecastMockMvc(KEY, jwtTokenProvider, service);
    CostReductionForecastService costReductionService =
        new CostReductionForecastService(
            new com.aether.ms_calculator.calculator.domain.CostReductionForecastValidator(),
            new com.aether.ms_calculator.calculator.domain.CostReductionForecastCalculator());
    costReductionMockMvc =
        CalculatorMockMvcSupport.costReductionForecastMockMvc(
            KEY, jwtTokenProvider, costReductionService);

    when(jwtTokenProvider.resolveToken(any(HttpServletRequest.class)))
        .thenAnswer(
            invocation -> {
              HttpServletRequest request = invocation.getArgument(0);
              String header = request.getHeader("Authorization");
              if (header == null || !header.startsWith("Bearer ")) {
                return null;
              }
              return header.substring("Bearer ".length());
            });
    when(jwtTokenProvider.validateToken("employee-with-gas")).thenReturn(true);
    when(jwtTokenProvider.validateToken("employee-with-roi")).thenReturn(true);
    when(jwtTokenProvider.getAuthentication("employee-with-gas"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "gas@aether.test",
                "",
                List.of(new SimpleGrantedAuthority("calculator:gas"))));
    when(jwtTokenProvider.getAuthentication("employee-with-roi"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "roi@aether.test",
                "",
                List.of(new SimpleGrantedAuthority("calculator:roi"))));
  }

  @Test
  void roi_textPlain_returns415Envelope() throws Exception {
    investmentMockMvc
        .perform(
            post("/calculator/roi")
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.TEXT_PLAIN)
                .content(ROI_BODY))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.message").value("The Content-Type must be application/json."))
        .andExpect(jsonPath("$.status").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.error_code").value("unsupported_media_type"));
  }

  @Test
  void payback_textPlain_returns415Envelope() throws Exception {
    investmentMockMvc
        .perform(
            post("/calculator/payback")
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.TEXT_PLAIN)
                .content(ROI_BODY))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.message").value("The Content-Type must be application/json."))
        .andExpect(jsonPath("$.status").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.error_code").value("unsupported_media_type"));
  }

  @Test
  void costReductionForecast_textPlainWithJwt_returns415Envelope() throws Exception {
    costReductionMockMvc
        .perform(
            post(CalculatorPaths.COST_REDUCTION_FORECAST_PATH)
                .header("Authorization", BEARER_ROI)
                .contentType(MediaType.TEXT_PLAIN)
                .content(COST_REDUCTION_BODY))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.message").value("The Content-Type must be application/json."))
        .andExpect(jsonPath("$.status").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.error_code").value("unsupported_media_type"));
  }

  @Test
  void reductionForecast_textPlainWithJwt_returns415Envelope() throws Exception {
    forecastMockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.TEXT_PLAIN)
                .content(FORECAST_BODY))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.message").value("The Content-Type must be application/json."))
        .andExpect(jsonPath("$.status").value("UNSUPPORTED_MEDIA_TYPE"))
        .andExpect(jsonPath("$.error_code").value("unsupported_media_type"));
  }

  @Test
  void roi_wrongKeyWithTextPlain_stillReturns401() throws Exception {
    investmentMockMvc
        .perform(
            post("/calculator/roi")
                .header(AUTH_HEADER, "wrong")
                .contentType(MediaType.TEXT_PLAIN)
                .content(ROI_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void roi_missingKeyWithTextPlain_stillReturns401() throws Exception {
    investmentMockMvc
        .perform(
            post("/calculator/roi")
                .contentType(MediaType.TEXT_PLAIN)
                .content(ROI_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void costReductionForecast_applicationJsonCharsetUtf8_stillSucceeds() throws Exception {
    costReductionMockMvc
        .perform(
            post(CalculatorPaths.COST_REDUCTION_FORECAST_PATH)
                .header("Authorization", BEARER_ROI)
                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                .content(COST_REDUCTION_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void roi_applicationJsonCharsetUtf8_stillSucceeds() throws Exception {
    investmentMockMvc
        .perform(
            post("/calculator/roi")
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                .content(ROI_BODY))
        .andExpect(status().isOk());
  }

  @Test
  void roi_applicationJson_stillReachesValidation() throws Exception {
    investmentMockMvc
        .perform(
            post("/calculator/roi")
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }
}
