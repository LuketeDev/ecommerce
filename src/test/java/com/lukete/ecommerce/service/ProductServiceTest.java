package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.domain.ProductSpecification;
import com.lukete.ecommerce.dto.CreateProductRequest;
import com.lukete.ecommerce.dto.ProductResponse;
import com.lukete.ecommerce.dto.UpdateProductRequest;
import com.lukete.ecommerce.exception.CategoryNotFoundException;
import com.lukete.ecommerce.exception.ProductNotFoundException;
import com.lukete.ecommerce.mapper.ProductMapper;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
        @Mock
        private ProductMapper productMapper;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private CategoryRepository categoryRepository;
        @InjectMocks
        private ProductService productService;

        @Test
        void shouldCreateProduct() {
                UUID categoryId = UUID.randomUUID();
                CreateProductRequest request = new CreateProductRequest("Mouse", "Mouse gamer com LED RGB",
                                new BigDecimal("49.90"), categoryId);
                Category category = createCategoryWithId(categoryId);

                when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.of(category));
                when(productRepository.save(any(Product.class)))
                                .thenAnswer(invocation -> {
                                        Product product = invocation.getArgument(0);
                                        product.setId(UUID.randomUUID());
                                        return product;
                                });
                when(productMapper.toResponse(any(Product.class)))
                                .thenAnswer(invocation -> {
                                        Product product = invocation.getArgument(0);
                                        return new ProductResponse(
                                                        product.getId(),
                                                        product.getName(),
                                                        product.getDescription(),
                                                        product.getPrice(),
                                                        product.getCategory().getId(),
                                                        product.getCategory().getName(),
                                                        product.isActive());
                                });

                ProductResponse response = productService.create(request);

                assertThat(response.id()).isNotNull();
                assertThat(response.name()).isEqualTo("Mouse");
                assertThat(response.description())
                                .isEqualTo("Mouse gamer com LED RGB");
                assertThat(response.price()).isEqualByComparingTo(new BigDecimal("49.90"));
                assertThat(response.categoryId()).isEqualTo(categoryId);

                verify(categoryRepository)
                                .findById(categoryId);
                verify(productRepository).save(any(Product.class));
                verify(productMapper).toResponse(any(Product.class));
        }

        @Test
        void shouldRejectProductWithInexistentCategory() {
                UUID categoryId = UUID.randomUUID();
                CreateProductRequest request = new CreateProductRequest("Mouse", "Mouse gamer com LED RGB",
                                new BigDecimal("49.90"), categoryId);

                when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> productService.create(request))
                                .isInstanceOf(CategoryNotFoundException.class);

                verify(productRepository, never()).save(any(Product.class));
                verify(productMapper, never()).toResponse(any(Product.class));
        }

        @Test
        void shouldReturnNotFoundWhenProductIsInexistent() {
                UUID id = UUID.randomUUID();

                when(productRepository.findById(id)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> productService.findById(id))
                                .isInstanceOf(ProductNotFoundException.class);
                verify(productMapper, never()).toResponse(any());
        }

        @Test
        void shouldUpdateProduct() {
                UUID categoryId = UUID.randomUUID();
                UUID newCategoryId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();
                Product existingProduct = createProductWithId(productId, categoryId);
                Category newCategory = createCategoryWithId(newCategoryId);
                UpdateProductRequest request = new UpdateProductRequest("Monitor", "Monitor 4k OLED curvado",
                                new BigDecimal("129.90"), newCategoryId, false);

                when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
                when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.of(newCategory));
                when(productMapper.toResponse(any(Product.class)))
                                .thenAnswer(invocation -> {
                                        Product product = invocation.getArgument(0);
                                        return new ProductResponse(
                                                        product.getId(),
                                                        product.getName(),
                                                        product.getDescription(),
                                                        product.getPrice(),
                                                        product.getCategory().getId(),
                                                        product.getCategory().getName(),
                                                        product.isActive());
                                });
                when(productRepository.save(any(Product.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ProductResponse response = productService.updateProduct(productId, request);

                ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

                verify(productRepository).save(captor.capture());

                Product savedProduct = captor.getValue();

                assertThat(savedProduct.getName()).isEqualTo("Monitor");
                assertThat(savedProduct.getDescription())
                                .isEqualTo("Monitor 4k OLED curvado");
                assertThat(savedProduct.getPrice())
                                .isEqualByComparingTo("129.90");
                assertThat(savedProduct.getCategory()).isEqualTo(newCategory);
                assertThat(savedProduct.isActive()).isFalse();

                assertThat(response.id()).isEqualTo(productId);
                assertThat(response.name()).isEqualTo("Monitor");
                assertThat(response.description())
                                .isEqualTo("Monitor 4k OLED curvado");
                assertThat(response.price()).isEqualByComparingTo(new BigDecimal("129.90"));
                assertThat(response.categoryId()).isEqualTo(newCategoryId);

                verify(categoryRepository)
                                .findById(newCategoryId);
                verify(productMapper).toResponse(any(Product.class));
        }

        @Test
        void shouldRejectUpdateWithInexistentCategory() {
                UUID categoryId = UUID.randomUUID();
                UUID newCategoryId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();
                Product existingProduct = createProductWithId(productId, categoryId);

                UpdateProductRequest request = new UpdateProductRequest("Monitor", "Monitor 4k OLED curvado",
                                new BigDecimal("129.90"), newCategoryId, false);

                when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
                when(categoryRepository.findById(request.categoryId())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> productService.updateProduct(productId, request))
                                .isInstanceOf(CategoryNotFoundException.class);

                verify(productRepository, never()).save(any());
                verify(productRepository).findById(productId);
                verify(categoryRepository).findById(newCategoryId);
                verify(productMapper, never()).toResponse(any());
        }

        @Test
        void shouldRejectUpdateWithInexistentProduct() {
                UUID newCategoryId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();

                UpdateProductRequest request = new UpdateProductRequest("Monitor", "Monitor 4k OLED curvado",
                                new BigDecimal("129.90"), newCategoryId, false);

                when(productRepository.findById(productId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> productService.updateProduct(productId, request))
                                .isInstanceOf(ProductNotFoundException.class);

                verify(productRepository).findById(productId);
                verify(productRepository, never()).save(any());
                verify(productMapper, never()).toResponse(any());
        }

        @Test
        void shouldReturnPaginatedProducts() {
                UUID categoryId = UUID.randomUUID();
                Product firstProduct = createProductWithId(UUID.randomUUID(), categoryId);
                Product secondProduct = createProductWithId(UUID.randomUUID(), categoryId);

                Pageable pageable = PageRequest.of(0, 2);

                when(productRepository.findAll(
                                any(ProductSpecification.class),
                                eq(pageable)))
                                .thenReturn(new PageImpl<>(
                                                List.of(firstProduct, secondProduct),
                                                pageable,
                                                2));

                when(productMapper.toResponse(firstProduct))
                                .thenReturn(new ProductResponse(
                                                firstProduct.getId(),
                                                firstProduct.getName(),
                                                firstProduct.getDescription(),
                                                firstProduct.getPrice(),
                                                categoryId,
                                                firstProduct.getCategory().getName(),
                                                firstProduct.isActive()));

                when(productMapper.toResponse(secondProduct))
                                .thenReturn(new ProductResponse(
                                                secondProduct.getId(),
                                                secondProduct.getName(),
                                                secondProduct.getDescription(),
                                                secondProduct.getPrice(),
                                                categoryId,
                                                secondProduct.getCategory().getName(),
                                                secondProduct.isActive()));

                ProductSpecification specification = new ProductSpecification(null, null);
                Page<ProductResponse> result = productService.listProducts(pageable, specification);

                assertThat(result.getContent()).hasSize(2);
                assertThat(result.getContent())
                                .extracting(ProductResponse::id)
                                .containsExactly(firstProduct.getId(), secondProduct.getId());

                assertThat(result.getNumber()).isZero();
                assertThat(result.getSize()).isEqualTo(2);
                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getTotalPages()).isEqualTo(1);

                verify(productRepository).findAll(specification, pageable);
                verify(productMapper).toResponse(firstProduct);
                verify(productMapper).toResponse(secondProduct);
        }

        @Test
        void shouldPreservePaginationAndSorting() {
                Pageable pageable = PageRequest.of(
                                0,
                                5,
                                Sort.by(Sort.Direction.ASC, "price"));

                when(productRepository.findAll(
                                any(ProductSpecification.class),
                                eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

                ProductSpecification specification = new ProductSpecification(null, null);
                Page<ProductResponse> result = productService.listProducts(pageable, specification);

                assertThat(result.getSort().getOrderFor("price"))
                                .isNotNull()
                                .extracting(Sort.Order::getDirection)
                                .isEqualTo(Sort.Direction.ASC);

                verify(productRepository).findAll(specification, pageable);
        }

        @Test
        void shouldReturnEmptyPageWhenNoProductsExist() {
                Pageable pageable = PageRequest.of(0, 10);

                when(productRepository.findAll(
                                any(ProductSpecification.class),
                                eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

                ProductSpecification specification = new ProductSpecification(null, null);
                Page<ProductResponse> result = productService.listProducts(pageable, specification);

                assertThat(result.getContent()).isEmpty();
                assertThat(result.getTotalElements()).isZero();
                assertThat(result.getTotalPages()).isZero();

                verify(productRepository).findAll(specification, pageable);
                verify(productMapper, never()).toResponse(any(Product.class));
        }

        @Test
        void shouldPreservePaginationMetadata() {
                UUID categoryId = UUID.randomUUID();
                Product product = createProductWithId(UUID.randomUUID(), categoryId);
                Pageable pageable = PageRequest.of(1, 2);

                when(productRepository.findAll(
                                any(ProductSpecification.class),
                                eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(product), pageable, 5));

                ProductResponse response = new ProductResponse(
                                product.getId(),
                                product.getName(),
                                product.getDescription(),
                                product.getPrice(),
                                categoryId,
                                product.getCategory().getName(),
                                product.isActive());

                when(productMapper.toResponse(product)).thenReturn(response);
                ProductSpecification specification = new ProductSpecification(null, null);
                Page<ProductResponse> result = productService.listProducts(pageable, specification);

                assertThat(result.getNumber()).isEqualTo(1);
                assertThat(result.getSize()).isEqualTo(2);
                assertThat(result.getNumberOfElements()).isEqualTo(1);
                assertThat(result.getTotalElements()).isEqualTo(5);
                assertThat(result.getTotalPages()).isEqualTo(3);
                assertThat(result.getContent()).containsExactly(response);

                verify(productRepository).findAll(specification, pageable);
                verify(productMapper).toResponse(product);
        }

        private Category createCategoryWithId(UUID id) {
                Category category = new Category();
                category.setId(id);
                category.setName("Periféricos");
                category.setDescription("Acessórios para computadores");
                return category;
        }

        private Product createProductWithId(UUID id, UUID categoryId) {
                Product product = new Product();
                product.setId(id);
                product.setName("Teclado");
                product.setDescription("Teclado gamer mecânico");
                product.setPrice(new BigDecimal("59.90"));
                product.setCategory(createCategoryWithId(categoryId));
                product.setActive(true);

                return product;
        }
}
