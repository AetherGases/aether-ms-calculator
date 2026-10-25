package com.aether.ms_calculator.calculator.web;

import com.aether.ms_calculator.calculator.support.CalculatorMockMvcSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalculatorHttpContractTest {

  private static final String AUTH_HEADER = "authentication-key";
  private static final String KEY = CalculatorMockMvcSupport.TEST_INTERNAL_KEY;

  private final MockMvc mockMvc = CalculatorMockMvcSupport.mockMvc(KEY);

  @Test
  void roi_workedExample() throws Exception {
    String body = "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capex").value(150))
        .andExpect(jsonPath("$.wacc_monthly").value(0.25))
        .andExpect(jsonPath("$.months").value(60))
        .andExpect(jsonPath("$.vp_total").value(200.0))
        .andExpect(jsonPath("$.vpl").value(50.0))
        .andExpect(jsonPath("$.roi_percent").value(33.33))
        .andExpect(jsonPath("$.viable").value(true));
  }

  @Test
  void roi_validationBeforeAuth() throws Exception {
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, "wrong")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error_code").value("unauthorized"));
  }

  @Test
  void roi_extraFields_sorted() throws Exception {
    String body =
        "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125],\"discount_rate\":1,\"tax\":2}";
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value(
            "The request takes no discount_rate, tax. It accepts: capex, wacc_monthly, cash_flows, monthly_cash_flow."));
  }

  @Test
  void roi_missingCapex() throws Exception {
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("\"capex\" must be a number, got None."));
  }

  @Test
  void roi_unviableCapex250() throws Exception {
    String body = "{\"capex\":250,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vpl").value(-50.0))
        .andExpect(jsonPath("$.roi_percent").value(-20.0))
        .andExpect(jsonPath("$.viable").value(false));
  }

  @Test
  void roi_exactRecoveryMonthOne() throws Exception {
    String body = "{\"capex\":100,\"wacc_monthly\":0.25,\"cash_flows\":[125]}";
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vp_total").value(100.0))
        .andExpect(jsonPath("$.roi_percent").value(0.0))
        .andExpect(jsonPath("$.viable").value(true));
  }

  @Test
  void roi_roundingCase() throws Exception {
    String body = "{\"capex\":1000,\"wacc_monthly\":0.01,\"cash_flows\":[333.333]}";
    mockMvc.perform(post("/calculator/roi")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vp_total").value(330.03))
        .andExpect(jsonPath("$.vpl").value(-669.97));
  }

  @Test
  void payback_workedExample() throws Exception {
    String body = "{\"capex\":150,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payback_months").value(2))
        .andExpect(jsonPath("$.viable").value(true));
  }

  @Test
  void payback_unrecovered() throws Exception {
    String body = "{\"capex\":250,\"wacc_monthly\":0.25,\"cash_flows\":[125,156.25]}";
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payback_months").value(-1))
        .andExpect(jsonPath("$.viable").value(false));
  }

  @Test
  void payback_waccZero_accumulated() throws Exception {
    String body = "{\"capex\":290,\"wacc_monthly\":0,\"cash_flows\":[100,100,100]}";
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payback_months").value(3));
  }

  @Test
  void payback_paybackThenUnviable() throws Exception {
    StringBuilder flows = new StringBuilder("[");
    for (int i = 0; i < 30; i++) {
      flows.append("200,");
    }
    flows.append("-6000,");
    for (int i = 0; i < 29; i++) {
      flows.append("200,");
    }
    flows.setLength(flows.length() - 1);
    flows.append("]");
    String body = "{\"capex\":5900,\"wacc_monthly\":0,\"cash_flows\":" + flows + "}";
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payback_months").value(30))
        .andExpect(jsonPath("$.vp_total").value(5800.0))
        .andExpect(jsonPath("$.viable").value(false));
  }

  @Test
  void payback_validationMatchesRoi() throws Exception {
    mockMvc.perform(post("/calculator/payback")
            .header(AUTH_HEADER, KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_code").value("validation_error"))
        .andExpect(jsonPath("$.message").value("\"capex\" must be a number, got None."));
  }
}
