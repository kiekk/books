package me.soono.springbootdeveloper.dto.token.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateAccessTokenResponse {
    private String accessToken;
}
