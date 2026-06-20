package com.quocnva.easymall.config;

import com.quocnva.easymall.entity.RoleEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.repository.RoleRepository;
import com.quocnva.easymall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitConfig {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ADMIN_EMAIL = "admin@easymall.com";
    private static final String ADMIN_PASSWORD = "admin@123";
    private static final String ADMIN_FULL_NAME = "System Admin";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            initRoles();
            initAdminAccount();
        };
    }

    private void initRoles() {
        if (roleRepository.findByRoleName(ROLE_ADMIN).isEmpty()) {
            roleRepository.save(RoleEntity.builder().roleName(ROLE_ADMIN).build());
            log.info("Initialized role: {}", ROLE_ADMIN);
        }

        if (roleRepository.findByRoleName(ROLE_USER).isEmpty()) {
            roleRepository.save(RoleEntity.builder().roleName(ROLE_USER).build());
            log.info("Initialized role: {}", ROLE_USER);
        }
    }

    private void initAdminAccount() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        RoleEntity adminRole = roleRepository.findByRoleName(ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found after init"));

        UserEntity admin = UserEntity.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .fullName(ADMIN_FULL_NAME)
                .isActive(true)
                .role(adminRole)
                .build();

        userRepository.save(admin);
        log.warn("Created default admin account: {} — change the password immediately!", ADMIN_EMAIL);
    }
}
