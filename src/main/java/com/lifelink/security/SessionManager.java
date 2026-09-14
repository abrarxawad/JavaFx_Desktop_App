package com.lifelink.security;

import com.lifelink.model.User;
import com.lifelink.model.UserRole;

/**
 * Manages the currently logged-in user session for the application.
 *
 * <p><b>Design Pattern — Singleton:</b> Only one session can be active in this
 * desktop application at a time. The Singleton ensures all controllers read
 * from the same session object without passing it through constructors.
 *
 * <p>When a user logs in, {@link #login(User)} is called by AuthService.
 * When the user logs out or the app exits, {@link #logout()} is called.
 * All FXML controllers call {@link #getCurrentUser()} to determine what to display.
 *
 * <p><b>Thread Safety:</b> The {@code currentUser} field is {@code volatile}
 * because background threads (notification poller, matching engine) may check
 * {@link #isLoggedIn()} from non-UI threads.
 *
 * <p><b>Package:</b> com.lifelink.security
 * <p><b>Used by:</b> AuthService, Every FXML controller
 */
public class SessionManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile SessionManager instance;

    // ── Session state ─────────────────────────────────────────────────────────
    private volatile User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    /**
     * Called by AuthService immediately after successful authentication.
     *
     * @param user the authenticated {@link User} object
     */
    public void login(User user) {
        this.currentUser = user;
    }

    /**
     * Clears the session. Called on logout or application exit.
     */
    public void logout() {
        this.currentUser = null;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the currently logged-in {@link User}, or null if not logged in. */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Returns the user ID of the current session, or -1 if not logged in. */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : -1;
    }

    /** Returns the role of the current user, or null if not logged in. */
    public UserRole getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    /** Returns true if a user is currently authenticated. */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks whether the current user has a given role.
     *
     * @param role the role to check against
     * @return true if the current user has the specified role
     */
    public boolean hasRole(UserRole role) {
        return isLoggedIn() && currentUser.getRole() == role;
    }
}
