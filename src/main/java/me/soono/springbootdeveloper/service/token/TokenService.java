package me.soono.springbootdeveloper.service.token;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.config.TokenProvider;
import me.soono.springbootdeveloper.domain.user.User;
import me.soono.springbootdeveloper.service.user.UserService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public String createNewAccessToken(String refreshToken) {
        // 토크 유효성 검사
        if (!tokenProvider.validToken(refreshToken)) {
            throw new IllegalArgumentException("Unexpected token");
        }

        Long userId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();
        User user = userService.findById(userId);

        return tokenProvider.generateToken(user, Duration.ofHours(2));
    }
}
