package com.aether.ms_calculator.shared.security.jwt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.aether.ms_calculator.shared.exceptions.UnauthorizedException;
import com.aether.ms_calculator.shared.security.UserService;
import com.aether.ms_calculator.shared.services.MessageService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    provider = new JwtTokenProvider(mock(UserService.class), mock(MessageService.class));
    setField("secretKey", "unit-test-secret");
    setField("validityInMilliseconds", 3_600_000);
    provider.init();
  }

  @Test
  void validateToken_wrongSignature_throwsUnauthorized() {
    String token =
        JWT.create()
            .withSubject("gas@aether.test")
            .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
            .sign(Algorithm.HMAC256("other-secret"));

    assertThatThrownBy(() -> provider.validateToken(token))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void validateToken_algorithmMismatch_throwsUnauthorized() {
    String token =
        JWT.create()
            .withSubject("gas@aether.test")
            .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
            .sign(Algorithm.HMAC512("unit-test-secret"));

    assertThatThrownBy(() -> provider.validateToken(token))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void validateToken_malformed_throwsUnauthorized() {
    assertThatThrownBy(() -> provider.validateToken("not-a-jwt"))
        .isInstanceOf(UnauthorizedException.class);
  }

  private void setField(String name, Object value) throws Exception {
    Field field = JwtTokenProvider.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(provider, value);
  }
}
