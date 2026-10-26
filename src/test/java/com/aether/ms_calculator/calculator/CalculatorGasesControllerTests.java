package com.aether.ms_calculator.calculator;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aether.ms_calculator.calculator.services.GasCatalogService;
import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.aether.ms_calculator.shared.persistence.postgres.entities.GasEntity;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.GasRepository;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

class CalculatorGasesControllerTests {

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
    GasCatalogService gasCatalogService = new GasCatalogService(gasRepository);
    mockMvc = CalculatorMockMvcSupport.gasCatalogMockMvc(KEY, jwtTokenProvider, gasCatalogService);
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
  }

  @Test
  void gases_withoutJwt_returns401() throws Exception {
    mockMvc.perform(get("/calculator/gases")).andExpect(status().isUnauthorized());
  }

  @Test
  void gases_withAuthenticationKeyOnly_returns401() throws Exception {
    mockMvc
        .perform(get("/calculator/gases").header(AUTH_HEADER, KEY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void gases_withInvalidJwt_returns401() throws Exception {
    mockMvc
        .perform(get("/calculator/gases").header("Authorization", BEARER_INVALID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void gases_withJwtMissingCalculatorGas_returns403() throws Exception {
    mockMvc
        .perform(get("/calculator/gases").header("Authorization", BEARER_NO_GAS))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error_code").value("forbidden"));
  }

  @Test
  void gases_withCalculatorRoiOnly_returns403() throws Exception {
    mockMvc
        .perform(get("/calculator/gases").header("Authorization", BEARER_ROI))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error_code").value("forbidden"))
        .andExpect(jsonPath("$.message").value("Missing calculator:gas permission."));
  }

  @Test
  void gases_withCalculatorGas_returnsFullCatalog() throws Exception {
    when(gasRepository.findAll())
        .thenReturn(
            List.of(
                new GasEntity(1, "Carbon dioxide", "CO2", false, new BigDecimal("1"), null),
                new GasEntity(2, "Methane", "CH4", false, new BigDecimal("28"), null),
                new GasEntity(
                    4, "Biogenic carbon dioxide", "CO2(b)", true, new BigDecimal("1"), null)));

    mockMvc
        .perform(get("/calculator/gases").header("Authorization", BEARER_GAS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Carbon dioxide"))
        .andExpect(jsonPath("$[0].formula").value("CO2"))
        .andExpect(jsonPath("$[0].is_green").value(false))
        .andExpect(jsonPath("$[0].gwp").value(1))
        .andExpect(jsonPath("$[2].id").value(4))
        .andExpect(jsonPath("$[2].is_green").value(true));
  }

  @Test
  void gases_emptyCatalog_returnsEmptyArray() throws Exception {
    when(gasRepository.findAll()).thenReturn(List.of());

    mockMvc
        .perform(get("/calculator/gases").header("Authorization", BEARER_GAS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void gases_unconfiguredInternalKey_withJwt_returns200() throws Exception {
    when(gasRepository.findAll()).thenReturn(List.of());
    MockMvc unconfigured =
        CalculatorMockMvcSupport.gasCatalogMockMvc(
            "", jwtTokenProvider, new GasCatalogService(gasRepository));

    unconfigured
        .perform(get("/calculator/gases").header("Authorization", BEARER_GAS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
