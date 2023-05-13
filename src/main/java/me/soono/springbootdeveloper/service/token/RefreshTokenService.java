package me.soono.springbootdeveloper.service.token;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.token.RefreshToken;
import me.soono.springbootdeveloper.repository.tokoen.RefreshTokenRepository;
import org.hibernate.boot.model.naming.IllegalIdentifierException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken findByRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalIdentifierException("Unexpected token"));
    }
}
