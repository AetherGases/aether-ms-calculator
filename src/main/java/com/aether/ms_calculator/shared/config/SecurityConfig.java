package com.aether.ms_calculator.shared.config;

import com.aether.ms_calculator.calculator.security.CalculatorAuthenticationFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtTokenProvider tokenProvider;
  private final HandlerExceptionResolver handlerExceptionResolver;
  private final CalculatorAuthenticationFilter calculatorAuthenticationFilter;
  public static final String SECURITY = "BearerAuth";

  @Bean
  PasswordEncoder passwordEncoder(){
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
    return configuration.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http){
    JwtTokenFilter customFilter = new JwtTokenFilter(tokenProvider, handlerExceptionResolver);

    return http
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(calculatorAuthenticationFilter, JwtTokenFilter.class)
        .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(
            authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh/**",
                    "/api/auth/register",
                    "/api/auth/reset-password/send-code",
                    "/api/auth/reset-password/change-password",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/calculator/**").permitAll()
                .requestMatchers(
                    "/api/**"
                ).authenticated()
                .requestMatchers("/users").denyAll()

        ).exceptionHandling(exception -> exception
            .authenticationEntryPoint(
                (request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            )
        )
        .cors(cors -> {})
        .build();

  }
}
