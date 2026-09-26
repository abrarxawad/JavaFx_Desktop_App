package com.lifelink.util;

import javafx.scene.Scene;

import java.util.Objects;

/**
 * Applies the single dark theme used throughout the LifeLink application.
 */
public final class ThemeManager {

    private static final String DARK_THEME = "/css/styles.css";

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }

        scene.getStylesheets().removeIf(url ->
            url.endsWith("/css/styles.css") || url.endsWith("/css/styles-light.css")
        );
        scene.getStylesheets().add(
            Objects.requireNonNull(ThemeManager.class.getResource(DARK_THEME)).toExternalForm()
        );
    }
}
