package com.aether.ms_calculator.calculator.support;

import com.aether.ms_calculator.calculator.config.CalculatorAuthenticationProperties;
import com.aether.ms_calculator.calculator.domain.InvestmentCalculator;
import com.aether.ms_calculator.calculator.domain.InvestmentValidator;
import com.aether.ms_calculator.calculator.security.CalculatorAuthenticationFilter;
import com.aether.ms_calculator.calculator.web.CalculatorExceptionHandler;
import com.aether.ms_calculator.calculator.web.CalculatorPaybackController;
import com.aether.ms_calculator.calculator.web.CalculatorRoiController;
import com.aether.ms_calculator.calculator.web.CalculatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

public final class CalculatorMockMvcSupport {

  public static final String TEST_INTERNAL_KEY = "test-internal-calculator-key";

  private CalculatorMockMvcSupport() {}

  public static MockMvc authOnlyMockMvc(String internalAuthenticationKey) {
    ObjectMapper objectMapper = new ObjectMapper();
    CalculatorAuthenticationProperties properties = new CalculatorAuthenticationProperties();
    properties.setInternalKey(internalAuthenticationKey);
    CalculatorAuthenticationFilter filter =
        new CalculatorAuthenticationFilter(properties, objectMapper);

    return MockMvcBuilders.standaloneSetup(new AuthProbeController())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        .addFilters(filter)
        .build();
  }

  public static MockMvc mockMvc(String internalAuthenticationKey) {
    ObjectMapper objectMapper = new ObjectMapper();
    CalculatorAuthenticationProperties properties = new CalculatorAuthenticationProperties();
    properties.setInternalKey(internalAuthenticationKey);
    CalculatorAuthenticationFilter filter =
        new CalculatorAuthenticationFilter(properties, objectMapper);
    CalculatorService service =
        new CalculatorService(new InvestmentValidator(), new InvestmentCalculator());

    return MockMvcBuilders.standaloneSetup(
            new CalculatorRoiController(service), new CalculatorPaybackController(service))
        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        .setControllerAdvice(new CalculatorExceptionHandler())
        .addFilters(filter)
        .build();
  }

  @RestController
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
