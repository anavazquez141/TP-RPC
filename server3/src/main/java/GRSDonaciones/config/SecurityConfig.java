package GRSDonaciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

    private final String jwtSecret;

    // Inyecta el valor de jwt.secret desde application.properties
    public SecurityConfig(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/graphql").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtSecret), UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    public class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final String jwtSecret;

        public JwtAuthenticationFilter(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                try {
                    Jwts.parser()
                        .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)))
                        .parseClaimsJws(token);
                    SecurityContextHolder.getContext().setAuthentication(new JwtAuthentication(token));
                    System.out.println("Token JWT válido: " + token);
                } catch (Exception e) {
                    System.out.println("Error validando token JWT: " + e.getMessage());
                }
            } else {
                System.out.println("No se encontró encabezado Authorization");
            }
            filterChain.doFilter(request, response);
        }
    }

    public class JwtAuthentication implements Authentication {
        private final String token;
        private boolean authenticated = true;

        public JwtAuthentication(String token) {
            this.token = token;
        }

        @Override
        public String getPrincipal() { return token; }
        @Override
        public String getCredentials() { return token; }
        @Override
        public String getName() { return token; }
        @Override
        public Object getDetails() { return null; }
        @Override
        public boolean isAuthenticated() { return authenticated; }
        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            this.authenticated = isAuthenticated;
        }
        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return java.util.Collections.emptyList();
        }
    }
}