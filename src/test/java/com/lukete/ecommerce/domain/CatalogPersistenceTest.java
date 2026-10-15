package com.lukete.ecommerce.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.lukete.ecommerce.infrastructure.PostgresIntegrationTest;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;

@Transactional
class CatalogPersistenceTest extends PostgresIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldPersistCategory() {
        Category category = new Category();
        category.setName("Periféricos");
        category.setDescription("Acessórios para computadores");

        Category saved = categoryRepository.saveAndFlush(category);

        assertThat(saved.getId()).isNotNull();

        Category found = categoryRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(found.getName()).isEqualTo("Periféricos");
        assertThat(found.getDescription())
                .isEqualTo("Acessórios para computadores");
    }

    @Test
    void shouldPersistProductWithCategory() {
        Category category = new Category();
        category.setName("Monitores");
        category.setDescription("Monitores e telas");

        Category savedCategory = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName("Monitor 24");
        product.setDescription("Monitor de teste");
        product.setPrice(new BigDecimal("799.90"));
        product.setCategory(savedCategory);
        product.setActive(true);

        Product savedProduct = productRepository.saveAndFlush(product);

        assertThat(savedProduct.getId()).isNotNull();

        Product found = productRepository.findById(savedProduct.getId())
                .orElseThrow();

        assertThat(found.getName()).isEqualTo("Monitor 24");
        assertThat(found.getPrice())
                .isEqualByComparingTo("799.90");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCategory().getId())
                .isEqualTo(savedCategory.getId());
    }

    @Test
    void shouldGenerateProductAuditTimestamps() {
        Category category = new Category();
        category.setName("Armazenamento");
        category.setDescription("Discos e SSDs");

        Category savedCategory = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName("SSD 1 TB");
        product.setPrice(new BigDecimal("499.90"));
        product.setCategory(savedCategory);
        product.setActive(true);

        Product saved = productRepository.saveAndFlush(product);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt())
                .isEqualTo(saved.getCreatedAt());
    }

    @Test
    void shouldFindCategoryById() {
        Category category = new Category();
        category.setName("Teclados");
        category.setDescription("Teclados para computador");

        Category saved = categoryRepository.saveAndFlush(category);

        assertThat(categoryRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(Category::getName)
                .isEqualTo("Teclados");
    }
}