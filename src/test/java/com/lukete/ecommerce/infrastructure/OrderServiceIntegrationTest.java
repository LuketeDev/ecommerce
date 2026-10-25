package com.lukete.ecommerce.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.CreateOrderItemRequest;
import com.lukete.ecommerce.dto.CreateOrderRequest;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.InactiveProductException;
import com.lukete.ecommerce.exception.InsufficientProductStockException;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.OrderRepository;
import com.lukete.ecommerce.repository.ProductRepository;
import com.lukete.ecommerce.service.OrderService;

class OrderServiceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product keyboard;
    private Product mouse;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Categoria " + UUID.randomUUID());
        category.setDescription("Categoria para testes de pedidos");
        category = categoryRepository.saveAndFlush(category);

        keyboard = createProduct(category, "Teclado", new BigDecimal("199.90"), 10);
        mouse = createProduct(category, "Mouse", new BigDecimal("79.90"), 5);
    }

    @Test
    void shouldPersistOrderItemsTotalAndReservedStock() {
        OrderResponse response = orderService.createOrder(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(keyboard.getId(), 2),
                new CreateOrderItemRequest(mouse.getId(), 1))));

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("479.70");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting(item -> item.subtotal())
                .containsExactly(new BigDecimal("399.80"), new BigDecimal("79.90"));

        Order savedOrder = orderRepository.findById(response.id()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("479.70");
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, response.id());
        assertThat(itemCount).isEqualTo(2);

        List<BigDecimal> persistedPrices = jdbcTemplate.queryForList(
                "SELECT unit_price FROM order_items WHERE order_id = ? ORDER BY unit_price DESC",
                BigDecimal.class, response.id());
        assertThat(persistedPrices).containsExactly(new BigDecimal("199.90"), new BigDecimal("79.90"));

        assertThat(productRepository.findById(keyboard.getId()).orElseThrow().getStockQuantity()).isEqualTo(8);
        assertThat(productRepository.findById(mouse.getId()).orElseThrow().getStockQuantity()).isEqualTo(4);
    }

    @Test
    void shouldRollbackOrderAndAllStockChangesWhenAnyProductHasInsufficientStock() {
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(keyboard.getId(), 2),
                new CreateOrderItemRequest(mouse.getId(), 6)))))
                .isInstanceOf(InsufficientProductStockException.class);

        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.findById(keyboard.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
        assertThat(productRepository.findById(mouse.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectInactiveProductWithoutCreatingOrderOrChangingStock() {
        mouse.setActive(false);
        productRepository.saveAndFlush(mouse);

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(keyboard.getId(), 2),
                new CreateOrderItemRequest(mouse.getId(), 1)))))
                .isInstanceOf(InactiveProductException.class);

        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.findById(keyboard.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
        assertThat(productRepository.findById(mouse.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldPreventConcurrentOrdersFromOversellingLastUnit() throws Exception {
        keyboard.setStockQuantity(1);
        productRepository.saveAndFlush(keyboard);
        UUID productId = keyboard.getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> createOrder = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent start");
            }

            try {
                orderService.createOrder(new CreateOrderRequest(
                        List.of(new CreateOrderItemRequest(productId, 1))));
                return true;
            } catch (InsufficientProductStockException exception) {
                return false;
            }
        };

        try {
            Future<Boolean> first = executor.submit(createOrder);
            Future<Boolean> second = executor.submit(createOrder);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(orderRepository.count()).isEqualTo(1);
            assertThat(productRepository.findById(productId).orElseThrow().getStockQuantity()).isZero();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Product createProduct(Category category, String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Produto de teste");
        product.setPrice(price);
        product.setCategory(category);
        product.setActive(true);
        product.setStockQuantity(0);
        product = productRepository.saveAndFlush(product);
        product.setStockQuantity(stock);
        return productRepository.saveAndFlush(product);
    }
}
