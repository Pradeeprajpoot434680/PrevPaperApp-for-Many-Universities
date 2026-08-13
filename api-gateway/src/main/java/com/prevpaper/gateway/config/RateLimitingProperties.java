package com.prevpaper.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {

    /**
     * Enable or disable rate limiting globally
     */
    private boolean enabled = true;

    /**
     * IP-based rate limits
     */
    private Map<String, LimitConfig> ip = Map.of(
            "default", new LimitConfig(100, 60),
            "auth", new LimitConfig(10, 60),
            "upload", new LimitConfig(20, 60)
    );

    /**
     * User-based rate limits (applied only to authenticated requests)
     */
    private Map<String, LimitConfig> user = Map.of(
            "default", new LimitConfig(1000, 60),
            "auth", new LimitConfig(50, 60),
            "upload", new LimitConfig(100, 60)
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, LimitConfig> getIp() {
        return ip;
    }

    public void setIp(Map<String, LimitConfig> ip) {
        this.ip = ip;
    }

    public Map<String, LimitConfig> getUser() {
        return user;
    }

    public void setUser(Map<String, LimitConfig> user) {
        this.user = user;
    }

    /**
     * 🟢 Configuration holder for individual rate limit parameters
     */
    public static class LimitConfig {
        /**
         * Maximum number of requests allowed in the window
         */
        private int limit;

        /**
         * Time window in seconds
         */
        private int windowSeconds;

        public LimitConfig() {
        }

        public LimitConfig(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
