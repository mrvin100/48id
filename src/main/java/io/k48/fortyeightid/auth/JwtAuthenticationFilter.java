package io.k48.fortyeightid.auth;

import io.k48.fortyeightid.shared.JwtAuthenticationDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtValidationPort jwtValidationPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = header.substring(7);

        try {
            var jwt = jwtValidationPort.validateToken(token);
            var subject = jwt.getSubject();
            var rolesStr = (String) jwt.getClaim("role");
            var matricule = (String) jwt.getClaim("matricule");

            List<SimpleGrantedAuthority> authorities = List.of();
            if (rolesStr != null && !rolesStr.isBlank()) {
                authorities = Arrays.stream(rolesStr.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .toList();
            }

            var authentication = new UsernamePasswordAuthenticationToken(subject, null, authorities);
            // Store matricule in details so AuditContextAspect can read it without importing identity
            authentication.setDetails(new JwtAuthenticationDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request), matricule));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            log.debug("JWT authentication failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
