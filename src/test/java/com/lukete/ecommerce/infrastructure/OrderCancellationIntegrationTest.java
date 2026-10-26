package com.lukete.ecommerce.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.domain.OrderItem;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.InvalidOrderStatusException;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.OrderRepository;
import com.lukete.ecommerce.repository.ProductRepository;
import com.lukete.ecommerce.service.OrderService;

@SpringBootTest
@Testcontainers
class OrderCancellationIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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

    private Category category;

    @BeforeEach
    void setUp() {
        // Não use @Transactional nesta classe:
        // cada chamada concorrente precisa de sua própria transação.
        jdbcTemplate.execute(
                "TRUNCATE TABLE order_items, orders, products, categories CASCADE");

        category = new Category();
        category.setName("Testes");
        category = categoryRepository.save(category);
    }

    @Test
    void shouldRestoreStockOnlyOnceWhenTwoRequestsCancelSameOrder()
            throws Exception {

        Product product = createProduct("Teclado", 8);
        Order order = createPendingOrder(product, 2);

        UUID orderId = order.getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    orderService.cancelOrder(orderId);
                    return true;
                } catch (InvalidOrderStatusException exception) {
                    return false;
                }
            });

            Future<Boolean> second = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    orderService.cancelOrder(orderId);
                    return true;
                } catch (InvalidOrderStatusException exception) {
                    return false;
                }
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean firstSucceeded = first.get(15, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(15, TimeUnit.SECONDS);

            assertThat(List.of(firstSucceeded, secondSucceeded))
                    .containsExactlyInAnyOrder(true, false);

        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        Product persistedProduct = productRepository.findById(product.getId())
                .orElseThrow();

        Order persistedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        // O estoque começa em 8, pois as duas unidades já foram reservadas.
        assertThat(persistedProduct.getStockQuantity())
                .as("Estoque após duas tentativas concorrentes de cancelamento")
                .isEqualTo(10); // assertionfailederror
        assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldRollbackStockRestorationWhenRestoringStockFails() {
        Product productA = createProduct("Produto A", 5);
        Product productB = createProduct("Produto B", 10);

        // O serviço restaura os produtos em ordem crescente de UUID.
        Product firstProduct;
        Product secondProduct;

        if (productA.getId().compareTo(productB.getId()) < 0) {
            firstProduct = productA;
            secondProduct = productB;
        } else {
            firstProduct = productB;
            secondProduct = productA;
        }

        Order order = createPendingOrder(firstProduct, 2);
        addOrderItem(order, secondProduct, 1);
        order = orderRepository.saveAndFlush(order);

        UUID orderId = order.getId();

        // O segundo produto será restaurado por último e causará overflow.
        secondProduct.setStockQuantity(Integer.MAX_VALUE);
        int firstStockBeforeCancellation = firstProduct.getStockQuantity();

        int secondStockBeforeCancellation = secondProduct.getStockQuantity();
        productRepository.saveAndFlush(secondProduct);

        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(ArithmeticException.class);

        Product persistedFirst = productRepository
                .findById(firstProduct.getId())
                .orElseThrow();

        Product persistedSecond = productRepository
                .findById(secondProduct.getId())
                .orElseThrow();

        Order persistedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        assertThat(persistedFirst.getStockQuantity())
                .isEqualTo(firstStockBeforeCancellation);

        assertThat(persistedSecond.getStockQuantity())
                .isEqualTo(secondStockBeforeCancellation);

        assertThat(persistedOrder.getStatus())
                .isEqualTo(OrderStatus.PENDING);
    }

    private Product createProduct(String name, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal("10.00"));
        product.setActive(true);
        product.setStockQuantity(stock);
        product.setCategory(category);

        return productRepository.saveAndFlush(product);
    }

    private Order createPendingOrder(Product product, int quantity) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(product.getPrice()
                .multiply(BigDecimal.valueOf(quantity)));
        order.setItems(new ArrayList<>());

        addOrderItem(order, product, quantity);

        return orderRepository.saveAndFlush(order);
    }

    private void addOrderItem(
            Order order, Product product, int quantity) {

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());

        order.getItems().add(item);
    }
}
