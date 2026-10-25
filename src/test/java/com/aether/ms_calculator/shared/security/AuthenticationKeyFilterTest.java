package com.aether.ms_calculator.shared.security;

import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationKeyFilterTest {

  private static final String AUTH_HEADER = AuthenticationKeyFilter.HEADER_NAME;
  private static final String VALID_KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;
  private static final String BODY =
      "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";

  private final MockMvc mockMvc = CalculatorMockMvcSupport.authOnlyMockMvc(VALID_KEY);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void roi_missingHeader_returns401() throws Exception {
    mockMvc.perform(post("/calculator/roi")
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"))
        .andExpect(jsonPath("$.message").value("Missing authentication key."));
  }

  @Test
  void payback_missingHeader_returns401() throws Exception {
    mockMvc.perform(post("/calculator/payback")
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"))
        .andExpect(jsonPath("$.message").value("Missing authentication key."));
  }

  @Test
  void payback_wrongKey_returns401() throws Exception {
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, "wrong-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"))
        .andExpect(jsonPath("$.message").value("Invalid authentication key."));
  }

  @Test
  void roi_wrongKey_returns401() throws Exception {
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, "wrong-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"))
        .andExpect(jsonPath("$.message").value("Invalid authentication key."));
  }

  @Test
  void roi_validKey_returns200() throws Exception {
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, VALID_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void payback_validKey_returns200() throws Exception {
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, VALID_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Nested
  class WhenInternalKeyNotConfigured {

    private final MockMvc unconfiguredMvc = CalculatorMockMvcSupport.authOnlyMockMvc("");

    @Test
    void roi_missingHeader_stillReturns401() throws Exception {
      unconfiguredMvc.perform(post("/calculator/roi")
              .contentType(MediaType.APPLICATION_JSON)
              .content(BODY))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.error_code").value("unauthorized"))
          .andExpect(jsonPath("$.message").value("Missing authentication key."));
    }

    @Test
    void roi_headerPresent_returns503WithoutErrorCode() throws Exception {
      MvcResult result = unconfiguredMvc.perform(post("/calculator/roi")
              .header(AUTH_HEADER, "any-key")
              .contentType(MediaType.APPLICATION_JSON)
              .content(BODY))
          .andExpect(status().isServiceUnavailable())
          .andReturn();

      JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
      assertThat(body.has("message")).isTrue();
      assertThat(body.get("message").asText())
          .isEqualTo("Calculator authentication is not configured.");
      assertThat(body.has("error_code")).isFalse();
    }

    @Test
    void payback_headerPresent_returns503WithoutErrorCode() throws Exception {
      MvcResult result = unconfiguredMvc.perform(post("/calculator/payback")
              .header(AUTH_HEADER, "any-key")
              .contentType(MediaType.APPLICATION_JSON)
              .content(BODY))
          .andExpect(status().isServiceUnavailable())
          .andReturn();

      JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
      assertThat(body.has("error_code")).isFalse();
      assertThat(body.get("message").asText())
          .isEqualTo("Calculator authentication is not configured.");
    }
  }
}
