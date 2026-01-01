package com.evorsio.mybox.user;

import java.util.UUID;

public interface UserService {
    UserInfoResponse getUserInfo(UUID id);
}
