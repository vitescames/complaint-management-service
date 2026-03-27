package com.complaintmanagementservice.adapters.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.h2.console")
public record H2ConsoleProperties(boolean enabled, String path) {

    public String servletMapping() {
        String normalizedPath = normalizePath();
        return normalizedPath.endsWith("/") ? normalizedPath + "*" : normalizedPath + "/*";
    }

    private String normalizePath() {
        if (path == null || path.isBlank()) {
            return "/h2-console";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
