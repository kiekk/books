package com.manning.ch06.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    @NotEmpty(message = "Enter your firstname")
    private String firstName;

    @NotEmpty(message = "Enter your lastname")
    private String lastName;

    @NotEmpty(message = "Enter a username")
    private String username;

    @NotEmpty(message = "Enter an email")
    private String email;

    @NotEmpty(message = "Enter a password")
    private String password;

    @NotEmpty(message = "Confirm your password")
    private String confirmPassword;

}
