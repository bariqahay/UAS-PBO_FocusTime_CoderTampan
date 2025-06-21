package com.focustime.session;

import com.focustime.model.UserModel;

public class CurrentUser {
    private static UserModel currentUser;

    public static void login(UserModel user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static UserModel get() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}
