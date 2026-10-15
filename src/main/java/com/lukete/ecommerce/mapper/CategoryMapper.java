package com.lukete.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.dto.CategoryResponse;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
