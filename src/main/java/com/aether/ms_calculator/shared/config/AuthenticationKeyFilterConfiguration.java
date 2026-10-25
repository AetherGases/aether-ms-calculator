package com.aether.ms_calculator.shared.config;

import com.aether.ms_calculator.shared.config.properties.CalculatorAuthenticationProperties;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(CalculatorAuthenticationProperties.class)
public class AuthenticationKeyFilterConfiguration {

  @Bean
  AuthenticationKeyFilter authenticationKeyFilter(
      CalculatorAuthenticationProperties properties,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
    return new AuthenticationKeyFilter(properties, handlerExceptionResolver);
  }

  @Bean
  FilterRegistrationBean<AuthenticationKeyFilter> authenticationKeyFilterRegistration(
      AuthenticationKeyFilter authenticationKeyFilter) {
    FilterRegistrationBean<AuthenticationKeyFilter> registration =
        new FilterRegistrationBean<>(authenticationKeyFilter);
    registration.setEnabled(false);
    return registration;
  }
}
