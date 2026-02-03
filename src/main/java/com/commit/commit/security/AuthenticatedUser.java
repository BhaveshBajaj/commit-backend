package com.commit.commit.security;

import com.commit.commit.entity.User;

/**
 * Holds the authenticated user for the current request.
 * This is set by the FirebaseAuthFilter after token verification.
 */
public class AuthenticatedUser {
    
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void set(User user) {
        currentUser.set(user);
    }

    public static User get() {
        return currentUser.get();
    }

    public static Long getUserId() {
        User user = currentUser.get();
        return user != null ? user.getId() : null;
    }

    public static void clear() {
        currentUser.remove();
    }
}
