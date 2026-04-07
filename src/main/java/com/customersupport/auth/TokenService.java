package com.customersupport.auth;

import com.customersupport.user.Role;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    private static final Duration TOKEN_DURATION = Duration.ofHours(24);

    private final PrivateKey privateKey;

    public TokenService() {
        try {
            String pem = Files.readString(Paths.get("keys/private.pem"));
            this.privateKey = KeyUtils.decodePrivateKey(pem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWT signing key", e);
        }
    }

    public String generateToken(Long userId, Role role) {
        return Jwt.issuer("customer-support-api")
                .subject(String.valueOf(userId))
                .groups(Set.of(role.name()))
                .expiresIn(TOKEN_DURATION)
                .sign(privateKey);
    }
}
