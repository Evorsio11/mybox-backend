package com.evorsio.mybox.auth;

public interface PasswordService {

    void sendPasswordResetToken(String email);

    void resetPassword(String token, String newPassword);
}
