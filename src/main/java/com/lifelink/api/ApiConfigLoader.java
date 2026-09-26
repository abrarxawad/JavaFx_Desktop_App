package com.lifelink.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ApiConfigLoader {
    private static final Properties PROPERTIES = loadProperties();

    private ApiConfigLoader() {
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = ApiConfigLoader.class.getResourceAsStream("/config/external-api.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing /config/external-api.properties on classpath.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load external API configuration.", e);
        }
        return properties;
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key, "");
    }

    public static String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }
}
