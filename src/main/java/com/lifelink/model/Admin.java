package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Domain model for the Admin role — extends {@link User}.
 *
 * <p>Admins do not have a separate profile table; all data lives in {@code users}.
 * The getDisplayName() simply returns the username.
 *
 * <p><b>Package:</b> com.lifelink.model
 */
public class Admin extends User {

    public Admin() {
        super();
    }

    @Override
    public String getDisplayName() {
        return "Admin: " + getUsername();
    }

    @Override
    public String toString() {
        return "Admin{userId=" + getUserId() + ", username='" + getUsername() + "'}";
    }
}
