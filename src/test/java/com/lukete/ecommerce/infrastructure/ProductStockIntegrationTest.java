
package com.lukete.ecommerce.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.ProductStockResponse;
import com.lukete.ecommerce.dto.UpdateProductStockRequest;
import com.lukete.ecommerce.enums.OperationType;
import com.lukete.ecommerce.exception.InvalidStockQuantityException;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;
import com.lukete.ecommerce.service.ProductService;

class ProductStockIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Categoria " + UUID.randomUUID());
        category.setDescription("Categoria de teste");
        category = categoryRepository.saveAndFlush(category);

        product = new Product();
        product.setName("Produto de teste");
        product.setDescription("Produto para testar o estoque");
        product.setPrice(new BigDecimal("99.90"));
        product.setCategory(category);
        product.setActive(true);

        // O estoque inicial deve ser zero.
        product = productRepository.saveAndFlush(product);
    }

    @Test
    void shouldInitializeStockAtZero() {
        ProductStockResponse response = productService.getProductStock(product.getId());

        assertThat(response.productId()).isEqualTo(product.getId());
        assertThat(response.amount()).isZero();
    }

    @Test
    void shouldPersistStockAddition() {
        productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(10, OperationType.ADD));

        Product reloaded = productRepository.findById(product.getId())
                .orElseThrow();

        assertThat(reloaded.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void shouldPersistStockReduction() {
        productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(10, OperationType.ADD));

        productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(3, OperationType.REDUCE));

        Product reloaded = productRepository.findById(product.getId())
                .orElseThrow();

        assertThat(reloaded.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void shouldNotChangeStockWhenReductionIsRejected() {
        productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(2, OperationType.ADD));

        assertThatThrownBy(() -> productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(3, OperationType.REDUCE)))
                .isInstanceOf(InvalidStockQuantityException.class);

        Product reloaded = productRepository.findById(product.getId())
                .orElseThrow();

        assertThat(reloaded.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void shouldPreventConcurrentReductionsFromOverselling()
            throws Exception {
        productService.updateProductStock(
                product.getId(),
                new UpdateProductStockRequest(5, OperationType.ADD));

        UUID productId = product.getId();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> reduction = () -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for concurrent start");
            }

            try {
                productService.updateProductStock(
                        productId,
                        new UpdateProductStockRequest(4, OperationType.REDUCE));
                return true;
            } catch (InvalidStockQuantityException exception) {
                return false;
            }
        };

        try {
            Future<Boolean> first = executor.submit(reduction);
            Future<Boolean> second = executor.submit(reduction);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean firstSucceeded = first.get(20, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(20, TimeUnit.SECONDS);

            AtomicInteger successes = new AtomicInteger();
            if (firstSucceeded) {
                successes.incrementAndGet();
            }
            if (secondSucceeded) {
                successes.incrementAndGet();
            }

            assertThat(successes.get()).isEqualTo(1);

            Product reloaded = productRepository.findById(productId)
                    .orElseThrow();

            assertThat(reloaded.getStockQuantity()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .isTrue();
        }
    }
}
