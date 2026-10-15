package com.lukete.ecommerce.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.dto.CategoryResponse;
import com.lukete.ecommerce.dto.CreateCategoryRequest;
import com.lukete.ecommerce.exception.CategoryAlreadyExistsException;
import com.lukete.ecommerce.mapper.CategoryMapper;
import com.lukete.ecommerce.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new CategoryAlreadyExistsException("Category already registered with name " + request.name());
        }

        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    public List<CategoryResponse> listCategories() {
        List<Category> result = categoryRepository.findAll();
        return result.stream().map(categoryMapper::toResponse).toList();
    }
}