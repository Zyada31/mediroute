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
            if (!users.existsByEmail("admin@mediroute.local")) {
                var u = new AppUser();
                u.setEmail("admin@mediroute.local");
                u.setPasswordHash(enc.encode("changeMe123!"));
                u.setRoles(String.valueOf(List.of("ADMIN","DISPATCHER")));
                users.save(u);
            }
        };
    }
}
