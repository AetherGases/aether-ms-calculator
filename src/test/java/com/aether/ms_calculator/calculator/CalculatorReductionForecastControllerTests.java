package com.aether.ms_calculator.calculator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aether.ms_calculator.calculator.services.ReductionForecastService;
import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.GasRepository;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class CalculatorReductionForecastControllerTests {

  private static final String AUTH_HEADER = AuthenticationKeyFilter.HEADER_NAME;
  private static final String KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;
  private static final String BEARER_GAS = "Bearer employee-with-gas";
  private static final String BEARER_ROI = "Bearer employee-with-roi";
  private static final String BEARER_NO_GAS = "Bearer employee-without-gas";
  private static final String BEARER_INVALID = "Bearer invalid";

  private final GasRepository gasRepository = mock(GasRepository.class);
  private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ReductionForecastService reductionForecastService =
        new ReductionForecastService(
            gasRepository,
            new com.aether.ms_calculator.calculator.domain.ReductionForecastValidator(),
            new com.aether.ms_calculator.calculator.domain.ReductionForecastCalculator());
    mockMvc =
        CalculatorMockMvcSupport.reductionForecastMockMvc(
            KEY, jwtTokenProvider, reductionForecastService);

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
    when(jwtTokenProvider.validateToken("employee-without-gas")).thenReturn(true);
    when(jwtTokenProvider.validateToken("invalid"))
        .thenThrow(
            com.aether.ms_calculator.shared.exceptions.UnauthorizedException.withLiteralMessage(
                "Invalid JWT.", "unauthorized"));
    when(jwtTokenProvider.getAuthentication("employee-with-gas"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "gas@aether.test",
                "",
                List.of(new SimpleGrantedAuthority("calculator:gas"))));
    when(jwtTokenProvider.getAuthentication("employee-without-gas"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "other@aether.test", "", List.of(new SimpleGrantedAuthority("calculator"))));
    when(jwtTokenProvider.getAuthentication("employee-with-roi"))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                "roi@aether.test",
                "",
                List.of(new SimpleGrantedAuthority("calculator:roi"))));

    stubSeededCatalog();
  }

  private void stubSeededCatalog() {
    when(gasRepository.findById(1))
        .thenReturn(
            Optional.of(
                new GasEntity(1, "Carbon dioxide", "CO2", false, new BigDecimal("1"), null)));
    when(gasRepository.findById(2))
        .thenReturn(
            Optional.of(new GasEntity(2, "Methane", "CH4", false, new BigDecimal("28"), null)));
    when(gasRepository.findById(4))
        .thenReturn(
            Optional.of(
                new GasEntity(
                    4, "Biogenic carbon dioxide", "CO2(b)", true, new BigDecimal("1"), null)));
  }

  @Test
  void forecast_co2ToCo2bTenTonsFiveYears_returnsFuture50Reduction50() throws Exception {
    String body =
        "{\"years\":5,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}";

    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.future_emission.gas").value(4))
        .andExpect(jsonPath("$.future_emission.scope").value(1))
        .andExpect(jsonPath("$.future_emission.volume_co2e").value(50))
        .andExpect(jsonPath("$.reduction_co2e").value(50));
  }

  @Test
  void forecast_ch4ToCo2bTwentyEightTonsFiveYears_returnsFuture5Reduction140() throws Exception {
    String body =
        "{\"years\":5,\"current_emission\":{\"gas\":2,\"volume_co2e\":28,\"scope\":2},\"green_gas\":4}";

    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.future_emission.gas").value(4))
        .andExpect(jsonPath("$.future_emission.scope").value(2))
        .andExpect(jsonPath("$.future_emission.volume_co2e").value(5))
        .andExpect(jsonPath("$.reduction_co2e").value(140));
  }

  @Test
  void forecast_withoutJwt_returns401() throws Exception {
    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void forecast_withAuthenticationKeyOnly_returns401() throws Exception {
    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header(AUTH_HEADER, KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void forecast_withInvalidJwt_returns401() throws Exception {
    postForecast(
            BEARER_INVALID,
            "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4}")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
    verifyNoInteractions(gasRepository);
  }

  @Test
  void forecast_withJwtMissingCalculatorGas_returns403() throws Exception {
    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_NO_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error_code").value("forbidden"));
    verifyNoInteractions(gasRepository);
  }

  @Test
  void forecast_withCalculatorRoiOnly_returns403() throws Exception {
    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_ROI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error_code").value("forbidden"))
        .andExpect(jsonPath("$.message").value("Missing calculator:gas permission."));
    verifyNoInteractions(gasRepository);
  }

  @Test
  void forecast_unconfiguredInternalKey_withJwt_returns200() throws Exception {
    ReductionForecastService service =
        new ReductionForecastService(
            gasRepository,
            new com.aether.ms_calculator.calculator.domain.ReductionForecastValidator(),
            new com.aether.ms_calculator.calculator.domain.ReductionForecastCalculator());
    MockMvc unconfigured =
        CalculatorMockMvcSupport.reductionForecastMockMvc("", jwtTokenProvider, service);

    unconfigured
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reduction_co2e").value(10));
  }

  @Test
  void forecast_validJwtBadBody_returns400Not401() throws Exception {
    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  @Test
  void forecast_extraField_returns400() throws Exception {
    String body =
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":1,\"scope\":1},\"green_gas\":4,\"inventory_id\":9}";

    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  @Test
  void forecast_zeroVolume_returnsZeros() throws Exception {
    String body =
        "{\"years\":5,\"current_emission\":{\"gas\":1,\"volume_co2e\":0,\"scope\":3},\"green_gas\":4}";

    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.future_emission.volume_co2e").value(0))
        .andExpect(jsonPath("$.reduction_co2e").value(0));
  }

  @Test
  void forecast_biogenicCurrentGas_returns400() throws Exception {
    String body =
        "{\"years\":1,\"current_emission\":{\"gas\":4,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}";

    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  @Test
  void forecast_nonBiogenicGreenGas_returns400() throws Exception {
    String body =
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":1}";

    mockMvc
        .perform(
            post("/calculator/reduction/forecast")
                .header("Authorization", BEARER_GAS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  @Test
  void forecast_unknownGasId_returns400() throws Exception {
    when(gasRepository.findById(99)).thenReturn(Optional.empty());

    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":99,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_yearsOne_appliesFormulaOnce() throws Exception {
    postForecast(
            BEARER_GAS,
            "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.future_emission.volume_co2e").value(10))
        .andExpect(jsonPath("$.reduction_co2e").value(10));
  }

  @Test
  void forecast_greenGwpLargerThanCurrent_keepsFullFossilReduction() throws Exception {
    when(gasRepository.findById(5))
        .thenReturn(
            Optional.of(
                new GasEntity(
                    5, "Heavy biogenic", "X(b)", true, new BigDecimal("56"), null)));

    postForecast(
            BEARER_GAS,
            "{\"years\":2,\"current_emission\":{\"gas\":2,\"volume_co2e\":10,\"scope\":1},\"green_gas\":5}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.future_emission.volume_co2e").value(40))
        .andExpect(jsonPath("$.reduction_co2e").value(20));
  }

  @Test
  void forecast_yearsZero_returns400() throws Exception {
    expectValidationError(
        "{\"years\":0,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_yearsNegative_returns400() throws Exception {
    expectValidationError(
        "{\"years\":-1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_yearsFloat_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1.0,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_yearsString_returns400() throws Exception {
    expectValidationError(
        "{\"years\":\"5\",\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_negativeVolume_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":-1,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_objectVolume_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":{},\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_booleanGasId_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":true,\"volume_co2e\":10,\"scope\":1},\"green_gas\":4}");
  }

  @Test
  void forecast_scopeZero_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":0},\"green_gas\":4}");
  }

  @Test
  void forecast_scopeFour_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":4},\"green_gas\":4}");
  }

  @Test
  void forecast_scopeOverflowsInt_returns400() throws Exception {
    expectValidationError(
        "{\"years\":1,\"current_emission\":{\"gas\":1,\"volume_co2e\":10,\"scope\":4294967297},\"green_gas\":4}");
  }

  @Test
  void forecast_currentEmissionArray_returns400WithEnvelope() throws Exception {
    expectValidationError("{\"years\":1,\"current_emission\":[],\"green_gas\":4}");
  }

  @Test
  void forecast_malformedJson_returns400WithEnvelope() throws Exception {
    postForecast(BEARER_GAS, "{")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  private void expectValidationError(String body) throws Exception {
    postForecast(BEARER_GAS, body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"));
  }

  private ResultActions postForecast(String bearer, String body) throws Exception {
    return mockMvc.perform(
        post("/calculator/reduction/forecast")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }
}
