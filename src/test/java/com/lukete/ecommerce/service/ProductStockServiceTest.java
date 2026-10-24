
package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.ProductStockResponse;
import com.lukete.ecommerce.dto.UpdateProductStockRequest;
import com.lukete.ecommerce.enums.OperationType;
import com.lukete.ecommerce.exception.InactiveProductException;
import com.lukete.ecommerce.exception.InvalidStockQuantityException;
import com.lukete.ecommerce.exception.ProductNotFoundException;
import com.lukete.ecommerce.mapper.ProductMapper;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Periféricos");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Teclado");
        product.setPrice(new BigDecimal("199.90"));
        product.setCategory(category);
        product.setActive(true);
        product.setStockQuantity(10);
    }

    @Test
    void shouldReturnCurrentStock() {
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        ProductStockResponse response = productService.getProductStock(product.getId());

        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.amount()).isEqualTo(10);
    }

    @Test
    void shouldThrowWhenGettingStockOfNonexistentProduct() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductStock(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldAddStock() {
        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductStockResponse response = productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(5, OperationType.ADD));

        assertThat(response.amount()).isEqualTo(15);
        assertThat(product.getStockQuantity()).isEqualTo(15);
        verify(productRepository).save(product);
    }

    @Test
    void shouldReduceStock() {
        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductStockResponse response = productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(4, OperationType.REDUCE));

        assertThat(response.amount()).isEqualTo(6);
        assertThat(product.getStockQuantity()).isEqualTo(6);
        verify(productRepository).save(product);
    }

    @Test
    void shouldAllowReducingStockToZero() {
        product.setStockQuantity(5);

        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));

        ProductStockResponse response = productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(5, OperationType.REDUCE));

        assertThat(response.amount()).isZero();
        assertThat(product.getStockQuantity()).isZero();
    }

    @Test
    void shouldRejectReductionGreaterThanAvailableStock() {
        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(11, OperationType.REDUCE)))
                .isInstanceOf(InvalidStockQuantityException.class);

        assertThat(product.getStockQuantity()).isEqualTo(10);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldAllowAddingStockToInactiveProduct() {
        product.setActive(false);

        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));

        ProductStockResponse response = productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(3, OperationType.ADD));

        assertThat(response.amount()).isEqualTo(13);
    }

    @Test
    void shouldRejectReducingStockOfInactiveProduct() {
        product.setActive(false);

        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(1, OperationType.REDUCE)))
                .isInstanceOf(InactiveProductException.class);

        assertThat(product.getStockQuantity()).isEqualTo(10);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectStockAdditionThatOverflowsInteger() {
        product.setStockQuantity(Integer.MAX_VALUE);

        when(productRepository.findByIdWithLock(product.getId()))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(1, OperationType.ADD)))
                .isInstanceOf(InvalidStockQuantityException.class);

        assertThat(product.getStockQuantity()).isEqualTo(Integer.MAX_VALUE);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowWhenUpdatingStockOfNonexistentProduct() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findByIdWithLock(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProductStock(
                productId,
                new UpdateProductStockRequest(1, OperationType.ADD)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any(Product.class));
    }
}
