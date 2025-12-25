package com.evorsio.mybox.auth.service;

import com.evorsio.mybox.auth.domain.User;

public interface AuthService {
    User register(String username,String email,String rawPassword);
    User login(String username,String rawPassword);
    boolean validateUser(String username,String password);
}
