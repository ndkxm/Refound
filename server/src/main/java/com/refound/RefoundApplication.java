package com.refound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Không dùng user/mật khẩu của Spring; đăng nhập qua Firebase + JWT riêng (ADR-05, việc P0-02).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class RefoundApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefoundApplication.class, args);
    }
}
