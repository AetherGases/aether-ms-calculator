package com.aether.ms_calculator.shared.security;

import com.aether.ms_calculator.calculator.controllers.CalculatorCostReductionForecastController;
import com.aether.ms_calculator.calculator.controllers.CalculatorPaybackController;
import com.aether.ms_calculator.calculator.controllers.CalculatorReductionForecastController;
import com.aether.ms_calculator.calculator.controllers.CalculatorRoiController;
import com.aether.ms_calculator.calculator.controllers.FindManyGasesController;
import com.aether.ms_calculator.calculator.services.CalculatorService;
import com.aether.ms_calculator.calculator.services.CostReductionForecastService;
import com.aether.ms_calculator.calculator.services.FinaManyGasesService;
import com.aether.ms_calculator.calculator.services.ReductionForecastService;
import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.aether.ms_calculator.shared.config.AuthenticationKeyFilterConfiguration;
import com.aether.ms_calculator.shared.config.JacksonConfig;
import com.aether.ms_calculator.shared.config.SecurityConfig;
import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import com.aether.ms_calculator.shared.persistence.postgres.entities.PermissionEntity;
import com.aether.ms_calculator.shared.persistence.postgres.repositories.PermissionRepository;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_calculator.shared.services.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real {@link SecurityConfig} chain: /roi and /payback only accept the internal
 * authentication-key, every other route needs a JWT whose permission pattern (from the
 * permission table) matches the request URI.
 */
@WebMvcTest(controllers = {
    CalculatorRoiController.class,
    CalculatorPaybackController.class,
    FindManyGasesController.class,
    CalculatorReductionForecastController.class,
    CalculatorCostReductionForecastController.class
})
@AutoConfigureMockMvc
@Import({
    SecurityConfig.class,
    JacksonConfig.class,
    AuthenticationKeyFilterConfiguration.class,
    PermissionAuthorizationManager.class,
    MessageService.class
})
@TestPropertySource(
    properties = "calculator.authentication.internal-key=" + CalculatorMockMvcSupport.TEST_INTERNAL_KEY)
class AuthenticationKeyFilterChainTest {

  private static final String AUTH_HEADER = AuthenticationKeyFilter.HEADER_NAME;
  private static final String KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;

  private static final String BEARER_GAS = "Bearer employee-with-gas";
  private static final String BEARER_FORECAST = "Bearer employee-with-forecast";
  private static final String BEARER_NO_PERMISSION = "Bearer employee-without-permission";
  private static final String BEARER_INVALID = "Bearer invalid";

  private static final String MSG_KEY_REQUIRED = "A chave de autenticação é obrigatória.";
  private static final String MSG_KEY_INVALID = "Chave de autenticação inválida.";
  private static final String MSG_AUTH_REQUIRED = "É necessário estar autenticado para acessar este recurso.";
  private static final String MSG_PERMISSION_DENIED = "Você não tem permissão para acessar este recurso.";
  private static final String MSG_JWT_INVALID = "Token JWT expirado e/ou inválido.";

  @Autowired private MockMvc mockMvc;

  @Autowired private List<FilterRegistrationBean<?>> filterRegistrations;

  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private PermissionRepository permissionRepository;

  @MockitoBean private CalculatorService calculatorService;
  @MockitoBean private FinaManyGasesService finaManyGasesService;
  @MockitoBean private ReductionForecastService reductionForecastService;
  @MockitoBean private CostReductionForecastService costReductionForecastService;

  @BeforeEach
  void setUp() {
    when(jwtTokenProvider.resolveToken(any(HttpServletRequest.class)))
        .thenAnswer(
            invocation -> {
              HttpServletRequest request = invocation.getArgument(0);
              String header = request.getHeader("Authorization");
              if (header == null || !header.startsWith("Bearer ")) return null;
              return header.substring("Bearer ".length());
            });

    jwtFor("employee-with-gas", "calculator");
    jwtFor("employee-with-forecast", "calculator", "CALCULATOR:FORECAST");
    jwtFor("employee-without-permission", "inventory:edit");
    when(jwtTokenProvider.validateToken("invalid"))
        .thenThrow(new UnauthorizedException("exception.jwt.invalid-or-expired"));

    // Mirrors the permission table: name -> URI regex pattern.
    when(permissionRepository.findByNameIgnoreCase("calculator"))
        .thenReturn(new PermissionEntity("calculator", null, ".*/gas.*"));
    when(permissionRepository.findByNameIgnoreCase("CALCULATOR:FORECAST"))
        .thenReturn(new PermissionEntity("calculator:forecast", null, ".*/(reduction|cost-reduction)/forecast"));
    when(permissionRepository.findByNameIgnoreCase("inventory:edit"))
        .thenReturn(new PermissionEntity("inventory:edit", null, ".*/inventories.*"));
  }

  private void jwtFor(String token, String... permissions) {
    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.getAuthentication(token))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(
                token + "@aether.test",
                "",
                java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
  }

  private static MockHttpServletRequestBuilder postJson(String path) {
    return post(path).contentType(MediaType.APPLICATION_JSON).content("{}");
  }

  // ---- /roi and /payback: authentication-key only ----

  @Test
  void roi_matchingKey_returns200() throws Exception {
    mockMvc.perform(postJson("/roi").header(AUTH_HEADER, KEY)).andExpect(status().isOk());
  }

  @Test
  void payback_matchingKey_returns200() throws Exception {
    mockMvc.perform(postJson("/payback").header(AUTH_HEADER, KEY)).andExpect(status().isOk());
  }

  @Test
  void roi_missingKey_returns401WithMessage() throws Exception {
    mockMvc
        .perform(postJson("/roi"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(MSG_KEY_REQUIRED))
        .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
  }

  @Test
  void payback_wrongKey_returns401WithMessage() throws Exception {
    mockMvc
        .perform(postJson("/payback").header(AUTH_HEADER, "wrong-key"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(MSG_KEY_INVALID));
  }

  @Test
  void roi_jwtWithPermissionsButNoKey_returns401() throws Exception {
    mockMvc
        .perform(postJson("/roi").header("Authorization", BEARER_FORECAST))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(MSG_KEY_REQUIRED));
  }

  @Test
  void payback_jwtWithPermissionsButNoKey_returns401() throws Exception {
    mockMvc
        .perform(postJson("/payback").header("Authorization", BEARER_GAS))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void roi_matchingKey_ignoresInvalidBearer() throws Exception {
    mockMvc
        .perform(postJson("/roi").header(AUTH_HEADER, KEY).header("Authorization", BEARER_INVALID))
        .andExpect(status().isOk());
  }

  // ---- JWT routes: permission pattern from the permission table ----

  @Test
  void gases_jwtWithMatchingPermissionPattern_returns200() throws Exception {
    mockMvc.perform(get("/gases").header("Authorization", BEARER_GAS)).andExpect(status().isOk());
  }

  @Test
  void gases_withNameFilter_jwtWithMatchingPermissionPattern_returns200() throws Exception {
    mockMvc
        .perform(get("/gases").param("name", "metano").header("Authorization", BEARER_GAS))
        .andExpect(status().isOk());
  }

  @Test
  void gases_withoutJwt_returns401WithMessage() throws Exception {
    mockMvc
        .perform(get("/gases"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(MSG_AUTH_REQUIRED));
  }

  @Test
  void gases_authenticationKeyOnly_returns401() throws Exception {
    mockMvc
        .perform(get("/gases").header(AUTH_HEADER, KEY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(MSG_AUTH_REQUIRED));
  }

  @Test
  void gases_invalidJwt_returns401WithMessage() throws Exception {
    mockMvc
        .perform(get("/gases").header("Authorization", BEARER_INVALID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value(MSG_JWT_INVALID));
  }

  @Test
  void gases_jwtWithoutMatchingPermission_returns403WithMessage() throws Exception {
    mockMvc
        .perform(get("/gases").header("Authorization", BEARER_NO_PERMISSION))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(MSG_PERMISSION_DENIED))
        .andExpect(jsonPath("$.status").value("FORBIDDEN"));
  }

  @Test
  void reductionForecast_jwtWithMatchingPermission_returns200() throws Exception {
    mockMvc
        .perform(postJson("/reduction/forecast").header("Authorization", BEARER_FORECAST))
        .andExpect(status().isOk());
  }

  @Test
  void reductionForecast_jwtWhosePatternDoesNotMatch_returns403() throws Exception {
    mockMvc
        .perform(postJson("/reduction/forecast").header("Authorization", BEARER_GAS))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(MSG_PERMISSION_DENIED));
  }

  @Test
  void reductionForecast_withoutJwt_returns401() throws Exception {
    mockMvc.perform(postJson("/reduction/forecast")).andExpect(status().isUnauthorized());
  }

  @Test
  void costReductionForecast_jwtWithMatchingPermission_returns200() throws Exception {
    mockMvc
        .perform(postJson("/cost-reduction/forecast").header("Authorization", BEARER_FORECAST))
        .andExpect(status().isOk());
  }

  @Test
  void costReductionForecast_jwtWhosePatternDoesNotMatch_returns403() throws Exception {
    mockMvc
        .perform(postJson("/cost-reduction/forecast").header("Authorization", BEARER_GAS))
        .andExpect(status().isForbidden());
  }

  @Test
  void costReductionForecast_authenticationKeyOnly_returns401() throws Exception {
    mockMvc
        .perform(postJson("/cost-reduction/forecast").header(AUTH_HEADER, KEY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void calculatorFilterIsNotRegisteredOnTheServletContainer() {
    List<FilterRegistrationBean<?>> calculatorRegistrations =
        filterRegistrations.stream()
            .filter(registration -> registration.getFilter() instanceof AuthenticationKeyFilter)
            .toList();
    assertThat(calculatorRegistrations).isNotEmpty();
    assertThat(calculatorRegistrations).allMatch(registration -> !registration.isEnabled());
  }
}
