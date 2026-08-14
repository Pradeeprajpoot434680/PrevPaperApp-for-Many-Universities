package com.prevpaper.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {

    /**
     * Enable or disable rate limiting globally
     */
    private boolean enabled = true;

    /**
     * Per-route rate limit configurations.
     * Keys are Ant-style path patterns (e.g. /api/v1/auth/login, /api/v1/upload/**).
     * The first matching pattern wins; order matters (LinkedHashMap preserves insertion order).
     */
    private final Map<String, RouteLimitConfig> routes = new LinkedHashMap<>();

    /**
     * Default fallback configuration when no route pattern matches.
     */
    private final RouteLimitConfig defaultConfig = new RouteLimitConfig(200, 60, 1000, 60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, RouteLimitConfig> getRoutes() {
        return routes;
    }

    public RouteLimitConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Configuration holder for per-route rate limit parameters.
     */
    public static class RouteLimitConfig {
        /**
         * Maximum number of IP-based requests allowed in the window
         */
        private int ipLimit;

        /**
         * IP-based time window in seconds
         */
        private int ipWindowSeconds;

        /**
         * Maximum number of user-based requests allowed in the window
         */
        private int userLimit;

        /**
         * User-based time window in seconds
         */
        private int userWindowSeconds;

        public RouteLimitConfig() {
        }

        public RouteLimitConfig(int ipLimit, int ipWindowSeconds, int userLimit, int userWindowSeconds) {
            this.ipLimit = ipLimit;
            this.ipWindowSeconds = ipWindowSeconds;
            this.userLimit = userLimit;
            this.userWindowSeconds = userWindowSeconds;
        }

        public int getIpLimit() {
            return ipLimit;
        }

        public void setIpLimit(int ipLimit) {
            this.ipLimit = ipLimit;
        }

        public int getIpWindowSeconds() {
            return ipWindowSeconds;
        }

        public void setIpWindowSeconds(int ipWindowSeconds) {
            this.ipWindowSeconds = ipWindowSeconds;
        }

        public int getUserLimit() {
            return userLimit;
        }

        public void setUserLimit(int userLimit) {
            this.userLimit = userLimit;
        }

        public int getUserWindowSeconds() {
            return userWindowSeconds;
        }

        public void setUserWindowSeconds(int userWindowSeconds) {
            this.userWindowSeconds = userWindowSeconds;
        }
    }
}
