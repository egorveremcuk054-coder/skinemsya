package ru.skinemsya.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ru.skinemsya.user.TokenResponse;
import ru.skinemsya.user.User;

import java.time.Duration;
import java.time.Instant;

/**
 * Выпускает JWT-токены для вошедших пользователей и гостей.
 */
@Service
public class TokenService {

    private static final String ISSUER = "skinemsya";

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public TokenResponse issue(User user) {
        Duration ttl = user.isGuest() ? properties.guestTtl() : properties.ttl();
        Instant now = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("name", user.getDisplayName())
                .claim("guest", user.isGuest());
        if (user.getEmail() != null) {
            claims.claim("email", user.getEmail());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }
}
