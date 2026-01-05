package com.evorsio.mybox.auth;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
}
