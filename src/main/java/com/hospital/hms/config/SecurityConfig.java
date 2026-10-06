package com.hospital.hms.config;

import com.hospital.hms.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // Uses CorsConfig settings
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/reset-password",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/admin/departments/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/doctors/**").authenticated()
                        .requestMatchers("/api/doctors/**").hasAnyRole("ADMIN", "DOCTOR")

                        .requestMatchers("/api/medical-records/**").hasAnyRole("DOCTOR", "ADMIN")


                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/medicines/**").hasAnyRole("PHARMACIST", "DOCTOR", "ADMIN")
                        .requestMatchers("/api/medicines/**").hasAnyRole("PHARMACIST", "ADMIN")
                        .requestMatchers("/api/prescriptions/**").hasAnyRole("PHARMACIST", "DOCTOR", "ADMIN")


                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/lab/tests").hasAnyRole("DOCTOR", "LAB_TECHNICIAN", "ADMIN")
                        .requestMatchers("/api/lab/**").hasAnyRole("LAB_TECHNICIAN", "ADMIN")


                        .requestMatchers("/api/bills/**", "/api/payments/**").hasAnyRole("FINANCE_OFFICER", "ADMIN")

                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/appointments")
                        .hasAnyRole("PATIENT", "RECEPTIONIST", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/appointments/**")
                        .hasAnyRole("PATIENT", "DOCTOR", "RECEPTIONIST", "ADMIN")
                        .requestMatchers("/api/appointments/**").hasAnyRole("RECEPTIONIST", "ADMIN")


                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/patients/**").authenticated()
                        .requestMatchers("/api/patients/**").hasAnyRole("RECEPTIONIST", "ADMIN")


                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}