package com.lukete.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.infrastructure.PostgresIntegrationTest;

@Transactional
class CategoryRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldFindCategoryIgnoringCase() {
        Category category = new Category();
        category.setName("Periféricos");
        category.setDescription("Acessórios");

        categoryRepository.saveAndFlush(category);

        assertThat(categoryRepository.existsByNameIgnoreCase("periféricos"))
                .isTrue();
    }

    @Test
    void shouldReturnFalseWhenCategoryDoesNotExist() {
        assertThat(categoryRepository.existsByNameIgnoreCase("Inexistente"))
                .isFalse();
    }
}