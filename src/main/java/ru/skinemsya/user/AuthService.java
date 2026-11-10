package ru.skinemsya.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skinemsya.common.ApiException;
import ru.skinemsya.security.TokenService;

import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private static final String DEFAULT_GUEST_NAME = "Я";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Пользователь с таким email уже зарегистрирован");
        }
        User user = users.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Неверный email или пароль"));
        return tokenService.issue(user);
    }

    /** Создаёт гостя без регистрации и сразу выдаёт ему токен. */
    public TokenResponse guest(GuestRequest request) {
        String name = request == null || request.displayName() == null || request.displayName().isBlank()
                ? DEFAULT_GUEST_NAME
                : request.displayName().trim();
        User user = users.save(User.guest(name));
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
