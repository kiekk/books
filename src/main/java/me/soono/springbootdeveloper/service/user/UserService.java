package me.soono.springbootdeveloper.service.user;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.domain.user.User;
import me.soono.springbootdeveloper.dto.user.request.AddUserRequest;
import me.soono.springbootdeveloper.repository.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public Long save(AddUserRequest request) {
        return userRepository.save(User.builder()
                .email(request.getEmail())
                .password(bCryptPasswordEncoder.encode(request.getPassword()))
                .build()).getId();
    }
}
