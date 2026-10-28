package com.tallermecanico.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Valida JWT antes de que se ejecute cualquier operación protegida. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    /** Inicializa el filtro que valida credenciales Bearer. */
    public JwtAuthenticationFilter(JwtService jwtService) { this.jwtService = jwtService; }
    /** Valida el JWT y establece la autenticación antes de continuar por la cadena de filtros. */
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = jwtService.claims(header.substring(7));
                Object roleClaim = claims.get("roles");
                List<GrantedAuthority> authorities = roleClaim instanceof List<?> roles
                        ? roles.stream().filter(String.class::isInstance).map(String.class::cast)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT)))
                            .map(GrantedAuthority.class::cast).toList()
                        : List.of();
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request, response);
    }
}
