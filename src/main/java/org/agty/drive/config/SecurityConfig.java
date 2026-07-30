package org.agty.drive.config;

import org.agty.drive.security.service.DriveUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private DaoAuthenticationProvider authenticationProvider(
            DriveUserDetailsService driveUserDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(driveUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DriveUserDetailsService driveUserDetailsService,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        http
                .authenticationProvider(authenticationProvider(driveUserDetailsService, passwordEncoder))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/invite/**", "/static/**", "/css/**", "/js/**", "/s/**").permitAll()
                        .requestMatchers("/control/**").hasRole("ADMIN")
                        .requestMatchers("/cabinet/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/cabinet", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?expired")
                )
                .rememberMe(Customizer.withDefaults());

        return http.build();
    }
}
