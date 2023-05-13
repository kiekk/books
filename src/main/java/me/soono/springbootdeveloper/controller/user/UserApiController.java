package me.soono.springbootdeveloper.controller.user;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.dto.user.request.AddUserRequest;
import me.soono.springbootdeveloper.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @PostMapping("user")
    public String signup(AddUserRequest request) {
        userService.save(request);
        return "redirect:/login";
    }
}
