package com.complaintmanagementservice.adapters.out.config;

import org.h2.server.web.JakartaWebServlet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class H2ConsoleConfigurationTest {

    private final H2ConsoleConfiguration h2ConsoleConfiguration = new H2ConsoleConfiguration();

    @Test
    void shouldCreateServletRegistrationForConfiguredPath() {
        H2ConsoleProperties properties = new H2ConsoleProperties(true, "/h2-console");

        ServletRegistrationBean<JakartaWebServlet> registration =
                h2ConsoleConfiguration.h2ConsoleServletRegistration(properties);

        assertThat(registration.getServlet()).isInstanceOf(JakartaWebServlet.class);
        assertThat(registration.getUrlMappings()).containsExactly("/h2-console/*");
        assertThat(registration.getInitParameters())
                .containsEntry("trace", "false")
                .containsEntry("webAllowOthers", "false");
    }

    @Test
    void shouldNormalizeServletMappings() {
        assertThat(new H2ConsoleProperties(true, null).servletMapping()).isEqualTo("/h2-console/*");
        assertThat(new H2ConsoleProperties(true, " ").servletMapping()).isEqualTo("/h2-console/*");
        assertThat(new H2ConsoleProperties(true, "custom-console").servletMapping()).isEqualTo("/custom-console/*");
        assertThat(new H2ConsoleProperties(true, "/custom-console/").servletMapping()).isEqualTo("/custom-console/*");
    }
}
