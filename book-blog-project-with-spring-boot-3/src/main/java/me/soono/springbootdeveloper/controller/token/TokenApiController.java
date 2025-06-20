package me.soono.springbootdeveloper.controller.token;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.dto.token.requeset.CreateAccessTokenRequest;
import me.soono.springbootdeveloper.dto.token.response.CreateAccessTokenResponse;
import me.soono.springbootdeveloper.service.token.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/token")
@RequiredArgsConstructor
public class TokenApiController {

    private final TokenService tokenService;

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAccessTokenResponse createNewAccessToken(@RequestBody CreateAccessTokenRequest request) {
        return new CreateAccessTokenResponse(tokenService.createNewAccessToken(request.getRefreshToken()));
    }
}
