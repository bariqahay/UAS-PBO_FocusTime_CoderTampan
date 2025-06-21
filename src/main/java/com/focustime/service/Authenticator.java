package com.focustime.service;

import com.focustime.model.UserModel;

public interface Authenticator {
    UserModel login(String username, String password);
}