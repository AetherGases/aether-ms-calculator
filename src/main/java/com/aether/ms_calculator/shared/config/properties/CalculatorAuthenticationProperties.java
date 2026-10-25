package com.aether.ms_calculator.shared.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "calculator.authentication")
public class CalculatorAuthenticationProperties {

  private String internalKey = "";
}
