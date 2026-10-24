
package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.domain.ProductSpecification;
import com.lukete.ecommerce.dto.ProductResponse;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;

@Testcontainers
@SpringBootTest
class ProductPaginationIntegrationTest {

        @Container
        static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

        @Autowired
        private ProductService productService;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        private Category category;

        @BeforeEach
        void setUp() {
                productRepository.deleteAll();
                categoryRepository.deleteAll();

                category = new Category();
                category.setName("Test category");
                category = categoryRepository.save(category);

                // Prices descend while names ascend, allowing us to test both
                // pagination by name and sorting by price.
                for (int i = 1; i <= 12; i++) {
                        Product product = new Product();
                        product.setName("Product %02d".formatted(i));
                        product.setDescription("Description %02d".formatted(i));
                        product.setPrice(BigDecimal.valueOf(13 - i));
                        product.setCategory(category);
                        product.setActive(true);

                        productRepository.save(product);
                }
        }

        @Test
        void shouldReturnFirstPageWithFiveProducts() {
                Page<ProductResponse> result = productService.listProducts(
                                PageRequest.of(0, 5, Sort.by("name").ascending()),
                                new ProductSpecification(null, null));

                assertThat(result.getContent()).hasSize(5);
                assertThat(result.getNumber()).isZero();
                assertThat(result.getSize()).isEqualTo(5);
                assertThat(result.getTotalElements()).isEqualTo(12);
                assertThat(result.getTotalPages()).isEqualTo(3);
                assertThat(result.getContent())
                                .extracting(ProductResponse::name)
                                .containsExactly(
                                                "Product 01",
                                                "Product 02",
                                                "Product 03",
                                                "Product 04",
                                                "Product 05");
        }

        @Test
        void shouldReturnSecondPageWithoutRepeatingProducts() {
                Page<ProductResponse> firstPage = productService.listProducts(
                                PageRequest.of(0, 5, Sort.by("name").ascending()),
                                new ProductSpecification(null, null));

                Page<ProductResponse> secondPage = productService.listProducts(
                                PageRequest.of(1, 5, Sort.by("name").ascending()),
                                new ProductSpecification(null, null));

                assertThat(secondPage.getContent()).hasSize(5);
                assertThat(secondPage.getNumber()).isEqualTo(1);
                assertThat(secondPage.getTotalElements()).isEqualTo(12);

                Set<UUID> firstPageIds = new HashSet<>(
                                firstPage.getContent().stream()
                                                .map(ProductResponse::id)
                                                .toList());

                Set<UUID> secondPageIds = new HashSet<>(
                                secondPage.getContent().stream()
                                                .map(ProductResponse::id)
                                                .toList());

                assertThat(firstPageIds).isNotEmpty().doesNotContainAnyElementsOf(secondPageIds);

                assertThat(secondPage.getContent())
                                .extracting(ProductResponse::name)
                                .containsExactly(
                                                "Product 06",
                                                "Product 07",
                                                "Product 08",
                                                "Product 09",
                                                "Product 10");
        }

        @Test
        void shouldReturnRemainingProductsOnLastPage() {
                Page<ProductResponse> result = productService.listProducts(
                                PageRequest.of(2, 5, Sort.by("name").ascending()),
                                new ProductSpecification(null, null));

                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getNumber()).isEqualTo(2);
                assertThat(result.getTotalElements()).isEqualTo(12);
                assertThat(result.getTotalPages()).isEqualTo(3);

                assertThat(result.getContent())
                                .extracting(ProductResponse::name)
                                .containsExactly("Product 11", "Product 12");
        }

        @Test
        void shouldReturnEmptyPageWhenRequestedPageIsBeyondEnd() {
                Page<ProductResponse> result = productService.listProducts(
                                PageRequest.of(3, 5), new ProductSpecification(null, null));

                assertThat(result.getContent()).isEmpty();
                assertThat(result.getNumber()).isEqualTo(3);
                assertThat(result.getTotalElements()).isEqualTo(12);
                assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        void shouldReturnProductsSortedByPriceAscending() {
                Page<ProductResponse> result = productService.listProducts(
                                PageRequest.of(0, 5, Sort.by("price").ascending()),
                                new ProductSpecification(null, null));
                assertThat(result.getContent())
                                .extracting(ProductResponse::price)
                                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                                .containsExactly(
                                                BigDecimal.ONE,
                                                BigDecimal.valueOf(2),
                                                BigDecimal.valueOf(3),
                                                BigDecimal.valueOf(4),
                                                BigDecimal.valueOf(5));
        }

        @Test
        void shouldReturnEmptyPageWhenThereAreNoProducts() {
                productRepository.deleteAll();

                Page<ProductResponse> result = productService.listProducts(
                                PageRequest.of(0, 5), new ProductSpecification(null, null));

                assertThat(result.getContent()).isEmpty();
                assertThat(result.getTotalElements()).isZero();
                assertThat(result.getTotalPages()).isZero();
        }
}
