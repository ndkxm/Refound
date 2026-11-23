package com.refound.item.presentation;

import com.refound.item.application.ItemDtos.CategoryDto;
import com.refound.item.infrastructure.CategoryRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {

    private final CategoryRepository categories;

    public CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    /** Danh mục cho form đăng tin và bộ lọc (06 §6.2). */
    @GetMapping("/api/v1/categories")
    public List<CategoryDto> all() {
        return categories.all().stream().map(c -> new CategoryDto(c.code(), c.nameVi(), c.sensitive())).toList();
    }
}
