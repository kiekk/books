package com.manning.ch06.service;

import com.manning.ch06.dto.UserDto;
import com.manning.ch06.model.ApplicationUser;

public interface UserService {

    ApplicationUser createUser(UserDto userDto);

    ApplicationUser findByUsername(String username);

}
