package com.aether.ms_calculator.calculator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aether.ms_calculator.calculator.services.CostReductionForecastService;
import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class CalculatorCostReductionForecastControllerTests {

  private static final String KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;
  private static final String AUTH_HEADER = AuthenticationKeyFilter.HEADER_NAME;
  private static final String BEARER_ROI = "Bearer employee-with-roi";
  private static final String BEARER_ROI_AND_GAS = "Bearer employee-with-both";
  private static final String BEARER_GAS = "Bearer employee-with-gas";
  private static final String BEARER_LEGACY = "Bearer employee-with-calculator";
  private static final String BEARER_INVALID = "Bearer invalid";
  private static final String PATH = "/api/calculator/cost-reduction/forecast";

  private final JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    CostReductionForecastService service =
        new CostReductionForecastService(
            new com.aether.ms_calculator.calculator.domain.CostReductionForecastValidator(),
            new com.aether.ms_calculator.calculator.domain.CostReductionForecastCalculator());
    mockMvc = CalculatorMockMvcSupport.costReductionForecastMockMvc(KEY, jwtTokenProvider, service);

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
    when(jwtTokenProvider.validateToken("employee-with-roi")).thenReturn(true);
    when(jwtTokenProvider.validateToken("employee-with-both")).thenReturn(true);
    when(jwtTokenProvider.validateToken("employee-with-gas")).thenReturn(true);
    when(jwtTokenProvider.validateToken("employee-with-calculator")).thenReturn(true);
    when(jwtTokenProvider.validateToken("invalid"))
        .thenThrow(
            com.aether.ms_calculator.shared.exceptions.UnauthorizedException.withLiteralMessage(
                "Invalid JWT.", "unauthorized"));

    when(jwtTokenProvider.getAuthentication("employee-with-roi"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "roi@aether.test",
                "",
                List.of(new SimpleGrantedAuthority("calculator:roi"))));
    when(jwtTokenProvider.getAuthentication("employee-with-both"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "both@aether.test",
                "",
                List.of(
                    new SimpleGrantedAuthority("calculator:roi"),
                    new SimpleGrantedAuthority("calculator:gases"))));
    when(jwtTokenProvider.getAuthentication("employee-with-gas"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "gas@aether.test",
                "",
                List.of(new SimpleGrantedAuthority("calculator:gases"))));
    when(jwtTokenProvider.getAuthentication("employee-with-calculator"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "legacy@aether.test", "", List.of(new SimpleGrantedAuthority("calculator"))));
  }

  @Test
  void forecast_canonicalExample_returns200WithProjection() throws Exception {
    String body =
        "{\"monthlyCost\":100000,\"reductionPercentage\":15,\"periodMonths\":12}";

    postForecast(BEARER_ROI, body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("BRL"))
        .andExpect(jsonPath("$.monthlySavings").value(15000))
        .andExpect(jsonPath("$.newMonthlyCost").value(85000))
        .andExpect(jsonPath("$.totalSavings").value(180000))
        .andExpect(jsonPath("$.projection.length()").value(12))
        .andExpect(jsonPath("$.projection[0].month").value(1))
        .andExpect(jsonPath("$.projection[0].accumulatedSavings").value(15000))
        .andExpect(jsonPath("$.projection[11].month").value(12))
        .andExpect(jsonPath("$.projection[11].accumulatedSavings").value(180000))
        .andExpect(content().string(containsString("\"monthlyCost\":100000")))
        .andExpect(content().string(not(containsString("\"monthlyCost\":100000.0"))));
  }

  @Test
  void forecast_decimalMonthlyCost_halfUpOnMonthlySavingsFirst() throws Exception {
    postForecast(
            BEARER_ROI,
            "{\"monthlyCost\":123.45,\"reductionPercentage\":15,\"periodMonths\":3}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlySavings").value(18.52))
        .andExpect(jsonPath("$.newMonthlyCost").value(104.93))
        .andExpect(jsonPath("$.totalSavings").value(55.56));
  }

  @Test
  void forecast_zeroMonthlyCost_returnsZeros() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":0,\"reductionPercentage\":15,\"periodMonths\":3}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlySavings").value(0))
        .andExpect(jsonPath("$.newMonthlyCost").value(0))
        .andExpect(jsonPath("$.totalSavings").value(0))
        .andExpect(jsonPath("$.projection[0].accumulatedSavings").value(0))
        .andExpect(jsonPath("$.projection[2].accumulatedSavings").value(0));
  }

  @Test
  void forecast_zeroReduction_keepsMonthlyCost() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":100000,\"reductionPercentage\":0,\"periodMonths\":6}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlySavings").value(0))
        .andExpect(jsonPath("$.newMonthlyCost").value(100000))
        .andExpect(jsonPath("$.totalSavings").value(0));
  }

  @Test
  void forecast_fiftyPercentTwentyFourMonths_returnsExpectedTotals() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":100000,\"reductionPercentage\":50,\"periodMonths\":24}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlySavings").value(50000))
        .andExpect(jsonPath("$.newMonthlyCost").value(50000))
        .andExpect(jsonPath("$.totalSavings").value(1200000))
        .andExpect(jsonPath("$.projection.length()").value(24))
        .andExpect(jsonPath("$.projection[23].accumulatedSavings").value(1200000))
        .andExpect(content().string(not(containsString("E"))));
  }

  @Test
  void forecast_periodOne_returnsSingleProjectionPoint() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":100000,\"reductionPercentage\":15,\"periodMonths\":1}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projection.length()").value(1))
        .andExpect(jsonPath("$.projection[0].month").value(1))
        .andExpect(jsonPath("$.projection[0].accumulatedSavings").value(15000));
  }

  @Test
  void forecast_periodSixty_returnsSixtyPoints() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":1000,\"reductionPercentage\":10,\"periodMonths\":60}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projection.length()").value(60))
        .andExpect(jsonPath("$.projection[59].month").value(60))
        .andExpect(jsonPath("$.projection[59].accumulatedSavings").value(6000));
  }

  @Test
  void forecast_noJwt_returns401() throws Exception {
    postForecast(null, "{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void forecast_invalidJwt_returns401() throws Exception {
    postForecast(BEARER_INVALID, "{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void forecast_authenticationKeyOnly_returns401() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void forecast_gasOnlyJwt_returns403() throws Exception {
    postForecast(BEARER_GAS, "{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error_code").value("forbidden"))
        .andExpect(jsonPath("$.message").value("Missing calculator:roi permission."));
  }

  @Test
  void forecast_legacyCalculatorJwt_returns403() throws Exception {
    postForecast(
            BEARER_LEGACY, "{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Missing calculator:roi permission."));
  }

  @Test
  void forecast_roiAndGasJwt_returns200() throws Exception {
    postForecast(
            BEARER_ROI_AND_GAS,
            "{\"monthlyCost\":100000,\"reductionPercentage\":15,\"periodMonths\":12}")
        .andExpect(status().isOk());
  }

  @Test
  void forecast_validJwtEmptyBody_returns400Not401() throws Exception {
    postForecast(BEARER_ROI, "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value("\"monthlyCost\" must be a number, got None."));
  }

  @Test
  void forecast_extraFieldBeforeMissingMonthlyCost_returns400ForExtras() throws Exception {
    postForecast(BEARER_ROI, "{\"employeeId\":1}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("employeeId")))
        .andExpect(jsonPath("$.message").value(containsString("monthlyCost, reductionPercentage, periodMonths")));
  }

  @Test
  void forecast_nullMonthlyCost_returns400GotNone() throws Exception {
    postForecast(
            BEARER_ROI, "{\"monthlyCost\":null,\"reductionPercentage\":1,\"periodMonths\":1}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value("\"monthlyCost\" must be a number, got None."));
  }

  @Test
  void forecast_stringMonthlyCost_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":\"100000\",\"reductionPercentage\":1,\"periodMonths\":1}");
  }

  @Test
  void forecast_booleanReduction_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":true,\"periodMonths\":1}");
  }

  @Test
  void forecast_negativeMonthlyCost_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":-1,\"reductionPercentage\":1,\"periodMonths\":1}");
  }

  @Test
  void forecast_monthlyCostTooManyDecimals_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":100000.001,\"reductionPercentage\":1,\"periodMonths\":1}");
  }

  @Test
  void forecast_monthlyCostTrailingZeros_accepted() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":100.100,\"reductionPercentage\":10,\"periodMonths\":1}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlySavings").value(10.01))
        .andExpect(jsonPath("$.newMonthlyCost").value(90.09))
        .andExpect(jsonPath("$.totalSavings").value(10.01));
    postForecast(BEARER_ROI, "{\"monthlyCost\":100000.10,\"reductionPercentage\":10,\"periodMonths\":1}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlySavings").value(10000.01))
        .andExpect(jsonPath("$.newMonthlyCost").value(90000.09))
        .andExpect(jsonPath("$.totalSavings").value(10000.01));
  }

  @Test
  void forecast_reductionFloat_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":15.0,\"periodMonths\":1}");
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":15.5,\"periodMonths\":1}");
  }

  @Test
  void forecast_reductionOutOfRange_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":51,\"periodMonths\":1}");
  }

  @Test
  void forecast_periodBounds_accepted() throws Exception {
    postForecast(BEARER_ROI, "{\"monthlyCost\":1,\"reductionPercentage\":0,\"periodMonths\":5}")
        .andExpect(status().isOk());
    postForecast(BEARER_ROI, "{\"monthlyCost\":1,\"reductionPercentage\":50,\"periodMonths\":18}")
        .andExpect(status().isOk());
    postForecast(BEARER_ROI, "{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":36}")
        .andExpect(status().isOk());
  }

  @Test
  void forecast_periodOutOfRange_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":0}");
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":61}");
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":-1}");
  }

  @Test
  void forecast_periodFloat_returns400() throws Exception {
    expectValidationError("{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":3.0}");
  }

  @Test
  void forecast_nonObjectBody_returns400MustBeJsonObject() throws Exception {
    postForecast(BEARER_ROI, "[]")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value("The request must be a JSON object."));
    postForecast(BEARER_ROI, "true")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value("The request must be a JSON object."));
    postForecast(BEARER_ROI, "1")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value("The request must be a JSON object."));
  }

  @Test
  void forecast_malformedJson_returns400Envelope() throws Exception {
    postForecast(BEARER_ROI, "{")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("The request body is not valid JSON."))
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  @Test
  void forecast_textPlain_returns415() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .header("Authorization", BEARER_ROI)
                .contentType(MediaType.TEXT_PLAIN)
                .content("{\"monthlyCost\":1,\"reductionPercentage\":1,\"periodMonths\":1}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.error_code").value("unsupported_media_type"));
  }

  private void expectValidationError(String body) throws Exception {
    postForecast(BEARER_ROI, body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  private ResultActions postForecast(String bearer, String body) throws Exception {
    var builder =
        post(PATH).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      builder.header("Authorization", bearer);
    }
    return mockMvc.perform(builder);
  }
}
