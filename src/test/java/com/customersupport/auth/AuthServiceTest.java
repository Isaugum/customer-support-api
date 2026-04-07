package com.customersupport.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.customersupport.user.Role;
import com.customersupport.user.User;
import com.customersupport.user.UserRepository;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuthServiceTest {

    @Inject
    AuthService authService;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    TokenService tokenService;

    // authenticate

    @Test
    void should_returnToken_when_credentialsAreValid() {
        // given

        User user = new User();
        user.id = 1L;
        user.role = Role.USER;
        user.passwordHash = BcryptUtil.bcryptHash("testpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(user.id, user.role)).thenReturn("jwt-token");

        // when
        Optional<String> result = authService.authenticate("test@example.com", "testpassword");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("jwt-token");
    }

    @Test
    void should_returnEmpty_when_passwordDoesNotMatch() {
        // given
        User user = new User();
        user.id = 1L;
        user.role = Role.USER;
        user.passwordHash = BcryptUtil.bcryptHash("testpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // when
        Optional<String> result = authService.authenticate("test@example.com", "wrongpassword");

        // then
        assertThat(result).isEmpty();
        verify(tokenService, never()).generateToken(any(), any());
    }

    @Test
    void should_returnEmpty_when_emailDoesNotExist() {
        // given
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // when
        Optional<String> result = authService.authenticate("unknown@example.com", "password");

        // then
        assertThat(result).isEmpty();
        verify(tokenService, never()).generateToken(any(), any());
    }
}
