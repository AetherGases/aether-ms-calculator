package com.aether.ms_calculator.calculator.config;

import com.aether.ms_calculator.calculator.security.CalculatorAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CalculatorAuthenticationProperties.class)
public class CalculatorAuthenticationConfiguration {

  @Bean
  CalculatorAuthenticationFilter calculatorAuthenticationFilter(
      CalculatorAuthenticationProperties properties,
      ObjectMapper objectMapper) {
    return new CalculatorAuthenticationFilter(properties, objectMapper);
  }

  @Bean
  FilterRegistrationBean<CalculatorAuthenticationFilter> calculatorAuthenticationFilterRegistration(
      CalculatorAuthenticationFilter calculatorAuthenticationFilter) {
    FilterRegistrationBean<CalculatorAuthenticationFilter> registration =
        new FilterRegistrationBean<>(calculatorAuthenticationFilter);
    registration.setEnabled(false);
    return registration;
  }
}
