package com.campus.news.config;

import com.campus.news.entity.User;
import com.campus.news.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // 确保admin用户密码正确
        User admin = userMapper.findByUsername("admin");
        if (admin != null) {
            String newPassword = passwordEncoder.encode("admin123");
            admin.setPassword(newPassword);
            userMapper.updateById(admin);
            log.info("Admin password has been reset to: admin123");
            log.info("Encoded password: {}", newPassword);
        }
    }
}
