package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.CreateOrderItemRequest;
import com.lukete.ecommerce.dto.CreateOrderRequest;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.DuplicateOrderItemException;
import com.lukete.ecommerce.exception.InactiveProductException;
import com.lukete.ecommerce.exception.InsufficientProductStockException;
import com.lukete.ecommerce.exception.ProductNotFoundException;
import com.lukete.ecommerce.repository.OrderRepository;
import com.lukete.ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Product keyboard;
    private Product mouse;

    @BeforeEach
    void setUp() {
        keyboard = createProduct(UUID.randomUUID(), "Teclado", new BigDecimal("199.90"), 10);
        mouse = createProduct(UUID.randomUUID(), "Mouse", new BigDecimal("79.90"), 5);
    }

    @Test
    void shouldCreateOrderAndReserveStock() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(keyboard.getId(), 2),
                new CreateOrderItemRequest(mouse.getId(), 1)));

        when(productRepository.findByIdWithLock(keyboard.getId())).thenReturn(Optional.of(keyboard));
        when(productRepository.findByIdWithLock(mouse.getId())).thenReturn(Optional.of(mouse));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            order.setCreatedAt(Instant.now());
            return order;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("479.70");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting(item -> item.productId())
                .containsExactly(keyboard.getId(), mouse.getId());
        assertThat(response.items())
                .extracting(item -> item.subtotal())
                .containsExactly(new BigDecimal("399.80"), new BigDecimal("79.90"));
        assertThat(response.items())
                .extracting(item -> item.unitPrice())
                .containsExactly(new BigDecimal("199.90"), new BigDecimal("79.90"));

        assertThat(keyboard.getStockQuantity()).isEqualTo(8);
        assertThat(mouse.getStockQuantity()).isEqualTo(4);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("479.70");
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(savedOrder));
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, 1)));

        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldRejectOrderContainingInactiveProduct() {
        keyboard.setActive(false);
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(keyboard.getId(), 1)));
        when(productRepository.findByIdWithLock(keyboard.getId())).thenReturn(Optional.of(keyboard));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InactiveProductException.class);

        assertThat(keyboard.getStockQuantity()).isEqualTo(10);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(keyboard.getId(), 11)));
        when(productRepository.findByIdWithLock(keyboard.getId())).thenReturn(Optional.of(keyboard));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientProductStockException.class);

        assertThat(keyboard.getStockQuantity()).isEqualTo(10);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldValidateAllStockBeforeReservingAnyProduct() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(keyboard.getId(), 2),
                new CreateOrderItemRequest(mouse.getId(), 6)));
        when(productRepository.findByIdWithLock(keyboard.getId())).thenReturn(Optional.of(keyboard));
        when(productRepository.findByIdWithLock(mouse.getId())).thenReturn(Optional.of(mouse));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientProductStockException.class);

        assertThat(keyboard.getStockQuantity()).isEqualTo(10);
        assertThat(mouse.getStockQuantity()).isEqualTo(5);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldRejectDuplicateProductItems() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(keyboard.getId(), 1),
                new CreateOrderItemRequest(keyboard.getId(), 2)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(DuplicateOrderItemException.class);

        verify(productRepository, never()).findByIdWithLock(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Product createProduct(UUID id, String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setActive(true);
        product.setStockQuantity(stock);
        return product;
    }
}
