package com.hospital.hms.config;

import com.hospital.hms.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF for local testing
                .csrf(csrf -> csrf.disable())

                // Enable CORS
                .cors(cors -> {})

                // Stateless session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**",
                                "/api/auth/login", "/api/auth/register", "/api/doctor-applications",
                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/departments", "/api/admin/departments/**")
                                .hasAnyRole("ADMIN", "DOCTOR_RECORDS_MANAGER")
                        .requestMatchers("/api/auth/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/doctor-applications/**")
                                .hasAnyRole("ADMIN", "DOCTOR_RECORDS_MANAGER")
                        .requestMatchers("/api/audit/**", "/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/lab/**").hasAnyRole("ADMIN", "LAB_TECHNICIAN")
                        .requestMatchers("/api/medicines/**", "/api/prescriptions/**")
                                .hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers("/api/bills/**", "/api/payments/**")
                                .hasAnyRole("ADMIN", "FINANCE_OFFICER")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )

                // Keep JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}