package io.k48.fortyeightid.shared.config;

import io.k48.fortyeightid.auth.ApiKeyAuthFilter;
import io.k48.fortyeightid.auth.ApiKeyManagementPort;
import io.k48.fortyeightid.auth.JwtAuthenticationFilter;
import io.k48.fortyeightid.auth.internal.JwtTokenService;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.PermissionsPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${fortyeightid.api.prefix}")
    private String apiPrefix;

    @Value("${fortyeightid.cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;
    private final JwtTokenService jwtTokenService;
    private final ApiKeyManagementPort apiKeyManagementPort;
    private final RateLimitConfig rateLimitConfig;
    private final io.github.bucket4j.BucketConfiguration loginRateLimit;
    private final io.github.bucket4j.BucketConfiguration forgotPasswordRateLimit;
    private final io.github.bucket4j.BucketConfiguration globalIpRateLimit;

    public SecurityConfig(ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
                          ProblemDetailAccessDeniedHandler accessDeniedHandler,
                          JwtTokenService jwtTokenService,
                          ApiKeyManagementPort apiKeyManagementPort,
                          RateLimitConfig rateLimitConfig,
                          io.github.bucket4j.BucketConfiguration loginRateLimit,
                          io.github.bucket4j.BucketConfiguration forgotPasswordRateLimit,
                          io.github.bucket4j.BucketConfiguration globalIpRateLimit) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtTokenService = jwtTokenService;
        this.apiKeyManagementPort = apiKeyManagementPort;
        this.rateLimitConfig = rateLimitConfig;
        this.loginRateLimit = loginRateLimit;
        this.forgotPasswordRateLimit = forgotPasswordRateLimit;
        this.globalIpRateLimit = globalIpRateLimit;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(apiKeyManagementPort);
    }

    // Not @Bean — prevents Tomcat from registering these as servlet filters
    // independently, which causes GenericFilterBean logger NPE on double-init
    private RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter(rateLimitConfig, loginRateLimit, forgotPasswordRateLimit, globalIpRateLimit);
    }

    private CacheControlFilter cacheControlFilter() {
        return new CacheControlFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(
                        // SpringDoc — custom swagger-ui.path redirects here
                        "/api/v1/docs",
                        "/api/v1/swagger-ui/**",
                        // SpringDoc — api-docs path
                        "/api-docs", "/api-docs/**",
                        // SpringDoc — standard fallback paths
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs", "/v3/api-docs/**",
                        "/webjars/**"
                ).permitAll()
                .requestMatchers("/.well-known/jwks.json").permitAll()
                .requestMatchers(
                        apiPrefix + "/auth/login",
                        apiPrefix + "/auth/refresh",
                        apiPrefix + "/auth/forgot-password",
                        apiPrefix + "/auth/reset-password",
                        apiPrefix + "/auth/activate-account"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter(new PermissionsPolicyHeaderWriter("camera=(), microphone=(), geolocation=(), payment=()"))
            )
            .addFilterBefore(cacheControlFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-API-Key"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
