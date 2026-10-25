package com.aether.ms_calculator.calculator.support;

import com.aether.ms_calculator.calculator.controllers.CalculatorGasController;
import com.aether.ms_calculator.calculator.controllers.CalculatorReductionForecastController;
import com.aether.ms_calculator.calculator.controllers.CalculatorPaybackController;
import com.aether.ms_calculator.calculator.controllers.CalculatorRoiController;
import com.aether.ms_calculator.calculator.services.GasCatalogService;
import com.aether.ms_calculator.calculator.services.ReductionForecastService;
import com.aether.ms_calculator.calculator.domain.InvestmentCalculator;
import com.aether.ms_calculator.calculator.domain.InvestmentValidator;
import com.aether.ms_calculator.calculator.services.CalculatorService;
import com.aether.ms_calculator.shared.config.properties.CalculatorAuthenticationProperties;
import com.aether.ms_calculator.shared.exceptions.MsCalculatorException;
import com.aether.ms_calculator.shared.handlers.GlobalExceptionHandler;
import com.aether.ms_calculator.shared.handlers.dto.output.ExceptionOutputDTO;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import com.aether.ms_calculator.shared.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

public final class CalculatorMockMvcSupport {

  public static final String TEST_INTERNAL_KEY = "test-internal-calculator-key";

  private CalculatorMockMvcSupport() {}

  public static MockMvc authOnlyMockMvc(String internalAuthenticationKey) {
    ObjectMapper objectMapper = new ObjectMapper();
    GlobalExceptionHandler advice = exceptionHandler();
    AuthenticationKeyFilter filter = authenticationKeyFilter(internalAuthenticationKey, objectMapper, advice);

    return MockMvcBuilders.standaloneSetup(new AuthProbeController())
        .setMessageConverters(jackson3Converter())
        .setControllerAdvice(advice)
        .addFilters(filter)
        .build();
  }

  public static MockMvc reductionForecastMockMvc(
      String internalAuthenticationKey,
      JwtTokenProvider jwtTokenProvider,
      ReductionForecastService reductionForecastService) {
    ObjectMapper objectMapper = new ObjectMapper();
    GlobalExceptionHandler advice = exceptionHandler();
    AuthenticationKeyFilter keyFilter =
        authenticationKeyFilter(internalAuthenticationKey, objectMapper, advice);
    JwtTokenFilter jwtFilter =
        new JwtTokenFilter(jwtTokenProvider, exceptionResolver(objectMapper, advice));

    return MockMvcBuilders.standaloneSetup(
            new CalculatorReductionForecastController(reductionForecastService))
        .setMessageConverters(jackson3Converter())
        .setControllerAdvice(advice)
        .addFilters(keyFilter, jwtFilter)
        .build();
  }

  public static MockMvc gasCatalogMockMvc(
      String internalAuthenticationKey,
      JwtTokenProvider jwtTokenProvider,
      GasCatalogService gasCatalogService) {
    ObjectMapper objectMapper = new ObjectMapper();
    GlobalExceptionHandler advice = exceptionHandler();
    AuthenticationKeyFilter keyFilter =
        authenticationKeyFilter(internalAuthenticationKey, objectMapper, advice);
    JwtTokenFilter jwtFilter =
        new JwtTokenFilter(jwtTokenProvider, exceptionResolver(objectMapper, advice));

    return MockMvcBuilders.standaloneSetup(new CalculatorGasController(gasCatalogService))
        .setMessageConverters(jackson3Converter())
        .setControllerAdvice(advice)
        .addFilters(keyFilter, jwtFilter)
        .build();
  }

  public static MockMvc mockMvc(String internalAuthenticationKey) {
    ObjectMapper objectMapper = new ObjectMapper();
    GlobalExceptionHandler advice = exceptionHandler();
    AuthenticationKeyFilter filter = authenticationKeyFilter(internalAuthenticationKey, objectMapper, advice);
    CalculatorService service =
        new CalculatorService(new InvestmentValidator(), new InvestmentCalculator());

    return MockMvcBuilders.standaloneSetup(
            new CalculatorRoiController(service), new CalculatorPaybackController(service))
        .setMessageConverters(jackson3Converter())
        .setControllerAdvice(advice)
        .addFilters(filter)
        .build();
  }

  private static JacksonJsonHttpMessageConverter jackson3Converter() {
    return new JacksonJsonHttpMessageConverter(JsonMapper.builder().build());
  }

  private static AuthenticationKeyFilter authenticationKeyFilter(
      String internalAuthenticationKey,
      ObjectMapper objectMapper,
      GlobalExceptionHandler advice) {
    CalculatorAuthenticationProperties properties = new CalculatorAuthenticationProperties();
    properties.setInternalKey(internalAuthenticationKey);
    return new AuthenticationKeyFilter(properties, exceptionResolver(objectMapper, advice));
  }

  private static GlobalExceptionHandler exceptionHandler() {
    StaticMessageSource source = new StaticMessageSource();
    source.setUseCodeAsDefaultMessage(true);
    return new GlobalExceptionHandler(new MessageService(source));
  }

  private static HandlerExceptionResolver exceptionResolver(
      ObjectMapper objectMapper, GlobalExceptionHandler advice) {
    return (request, response, handler, exception) -> {
      if (!(exception instanceof MsCalculatorException msException)) {
        return null;
      }
      ResponseEntity<ExceptionOutputDTO> entity = advice.handleMsCalculatorException(msException);
      response.setStatus(entity.getStatusCode().value());
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      try {
        objectMapper.writeValue(response.getOutputStream(), entity.getBody());
      } catch (IOException ioException) {
        throw new UncheckedIOException(ioException);
      }
      return new ModelAndView();
    };
  }

  @RestController
  @Profile("calculator-auth-probe")
  static class AuthProbeController {

    @PostMapping("/calculator/roi")
    Map<String, Object> roi() {
      return Map.of();
    }

    @PostMapping("/calculator/payback")
    Map<String, Object> payback() {
      return Map.of();
    }
  }
}
