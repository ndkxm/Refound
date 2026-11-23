package com.refound.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Dùng cho mọi test {@code *IT} / {@code *AccessTest}. Cùng một bộ cấu hình nên Spring dùng lại một context,
 * tức một container Postgres cho cả lượt chạy. Thêm cấu hình riêng ở từng test sẽ sinh context (và container) mới.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestDatabase.class, TestUsers.class, TestItems.class, FakeStorage.class, FakeFirebase.class})
public @interface IntegrationTest {}
