package com.campus.news.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String plainPassword = "admin123";
        String hashedPassword = encoder.encode(plainPassword);
        
        System.out.println("Plain password: " + plainPassword);
        System.out.println("Hashed password: " + hashedPassword);
        System.out.println("Hash length: " + hashedPassword.length());
        
        // 验证
        boolean matches = encoder.matches(plainPassword, hashedPassword);
        System.out.println("Password matches: " + matches);
        
        // 测试已有的哈希
        String existingHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        boolean existingMatches = encoder.matches(plainPassword, existingHash);
        System.out.println("Existing hash matches: " + existingMatches);
    }
}
