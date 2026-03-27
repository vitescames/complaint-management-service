package com.complaintmanagementservice.adapters.out.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(JakartaWebServlet.class)
public class H2ConsoleConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServletRegistration(
            H2ConsoleProperties h2ConsoleProperties
    ) {
        ServletRegistrationBean<JakartaWebServlet> registration =
                new ServletRegistrationBean<>(new JakartaWebServlet(), h2ConsoleProperties.servletMapping());
        registration.setLoadOnStartup(1);
        registration.addInitParameter("trace", "false");
        registration.addInitParameter("webAllowOthers", "false");
        return registration;
    }
}
