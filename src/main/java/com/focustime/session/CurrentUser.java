package com.focustime.session;


import com.focustime.model.UserModel;

public class CurrentUser {
    private static UserModel currentUser;

    public static void login(UserModel user) {
        currentUser = user;
    }

    public static UserModel get() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }
}
