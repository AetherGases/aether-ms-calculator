package com.aether.ms_calculator.shared.config;

import com.aether.ms_calculator.shared.exceptions.ForbiddenException;
import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import com.aether.ms_calculator.shared.security.AuthenticationKeyFilter;
import com.aether.ms_calculator.shared.security.CalculatorPaths;
import com.aether.ms_calculator.shared.security.PermissionAuthorizationManager;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenFilter;
import com.aether.ms_calculator.shared.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class SecurityConfig {
  private final JwtTokenProvider tokenProvider;
  private final HandlerExceptionResolver handlerExceptionResolver;
  private final AuthenticationKeyFilter authenticationKeyFilter;

  public SecurityConfig(
      JwtTokenProvider tokenProvider,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
      AuthenticationKeyFilter authenticationKeyFilter) {
    this.tokenProvider = tokenProvider;
    this.handlerExceptionResolver = handlerExceptionResolver;
    this.authenticationKeyFilter = authenticationKeyFilter;
  }
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
  SecurityFilterChain securityFilterChain(HttpSecurity http, PermissionAuthorizationManager permissionManager){
    JwtTokenFilter customFilter = new JwtTokenFilter(tokenProvider, handlerExceptionResolver);

    return http
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(authenticationKeyFilter, JwtTokenFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(
            authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(
                    CalculatorPaths.ROI_PATH,
                    CalculatorPaths.PAYBACK_PATH
                ).hasAuthority(AuthenticationKeyFilter.INTERNAL_CLIENT_AUTHORITY)
                .anyRequest().access(permissionManager)

        ).exceptionHandling(exception -> exception
            .authenticationEntryPoint(
                (request, response, authException) ->
                    handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        new UnauthorizedException("exception.authentication.required")
                    )
            )
            .accessDeniedHandler(
                (request, response, accessDeniedException) ->
                    handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        new ForbiddenException("exception.permission.denied")
                    )
            )
        )
        .cors(cors -> {})
        .build();

  }
}
