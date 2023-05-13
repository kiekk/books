package me.soono.springbootdeveloper.config.properties;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties("jwt")
public class JwtProperties {
    private String issuer;
    private String secretKey;

    public JwtProperties(String issuer, String secretKey) {
        this.issuer = issuer;
        this.secretKey = secretKey;
    }
}
