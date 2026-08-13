package com.forge.config;

import com.forge.auth.service.CustomUserDetailsService;
import com.forge.security.JwtAuthenticationFilter;
import com.forge.security.JwtTokenProvider;
import com.forge.security.RateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    /**
     * The JWT filter is intentionally a plain bean (no @Component) so it is registered exactly
     * once — inside the Spring Security chain via SecurityConfig.addFilterBefore — and not also
     * auto-registered as a servlet-level filter.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                                           CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    /**
     * Registers the rate limiter at order -101, ahead of the Spring Security filter chain
     * (default -100), so /api/auth/** is throttled before any auth work happens.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration() {
        FilterRegistrationBean<RateLimitingFilter> registration =
                new FilterRegistrationBean<>(new RateLimitingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(-101);
        return registration;
    }
}
