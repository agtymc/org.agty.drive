package org.agty.drive.config;

import org.agty.drive.dto.UserDto;
import org.agty.drive.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    @Bean
    public ApplicationRunner bootstrapAdminRunner(UserService userService, PasswordEncoder passwordEncoder) {
        return args -> {
            String login = LocalConfig.getString("bootstrap.admin.login", "admin").trim();
            String password = LocalConfig.getString("bootstrap.admin.password", "admin");
            String displayName = LocalConfig.getString("bootstrap.admin.display_name", "Administrator");

            if (userService.findByLogin(login) != null) {
                return;
            }

            UserDto userDto = new UserDto();
            userDto.setLogin(login);
            userDto.setPasswordHash(passwordEncoder.encode(password));
            userDto.setRoleCode("ROLE_ADMIN");
            userDto.setStatusCode("ACTIVE");
            userDto.setDisplayName(displayName);
            userDto.setStorageQuotaBytes(100L * 1024L * 1024L);

            UserDto saved = userService.save(userDto);
            if (saved != null && saved.getId() != null) {
                log.info("Created bootstrap admin user login={}", login);
            } else {
                log.error("Failed to create bootstrap admin user login={}", login);
            }
        };
    }
}
