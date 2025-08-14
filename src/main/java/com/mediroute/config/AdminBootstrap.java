package com.mediroute.config;

import com.mediroute.entity.AppUser;
import com.mediroute.repository.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class AdminBootstrap {
    @Bean CommandLineRunner seedAdmin(UserRepo users, PasswordEncoder enc) {
        return args -> {
            if (!users.existsByEmail("admin@mediroute.com")) {
                var u = new AppUser();
                u.setEmail("admin@mediroute.com");
                u.setPasswordHash(enc.encode("admin123"));
                u.setRoleList(List.of("ADMIN","DISPATCHER"));
                u.setActive(true);
                users.save(u);
            }
        };
    }
}
