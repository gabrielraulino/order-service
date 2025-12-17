package com.ms.order.config;

import com.ms.order.auth.GatewayAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança simplificada.
 * A autenticação é delegada ao API Gateway que valida o JWT e injeta headers HTTP.
 * Este serviço apenas lê os headers e configura o SecurityContext para autorização.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (para acesso via Swagger UI local)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("POST", "/orders/create").permitAll() // Para comunicação entre serviços
                        
                        // User endpoints
                        .requestMatchers("GET", "/orders/user").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("POST", "/orders/{id}/cancel").hasAnyRole("USER", "ADMIN")
                        
                        // Admin endpoints
                        .requestMatchers("GET", "/orders").hasRole("ADMIN")
                        .requestMatchers("GET", "/orders/{id}").hasRole("ADMIN")
                        .requestMatchers("/orders/**").hasRole("ADMIN")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
