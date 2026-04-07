package com.customersupport.auth;

import com.customersupport.user.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Inject
    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public Optional<String> authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> BcryptUtil.matches(password, user.passwordHash))
                .map(user -> {
                    LOG.infof("User %s authenticated successfully", email);
                    return tokenService.generateToken(user.id, user.role);
                });
    }
}
