
package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.domain.OrderItem;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.InvalidOrderStatusException;
import com.lukete.ecommerce.exception.OrderNotFoundException;
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

        private Order order;
        private Product product;
        private OrderItem orderItem;

        @BeforeEach
        void setUp() {
                UUID productId = UUID.randomUUID();

                product = new Product();
                product.setId(productId);
                product.setName("Teclado");
                product.setPrice(new BigDecimal("199.90"));
                product.setActive(true);
                product.setStockQuantity(7);

                order = new Order();
                order.setId(UUID.randomUUID());
                order.setStatus(OrderStatus.PENDING);
                order.setTotalAmount(new BigDecimal("399.80"));
                order.setCreatedAt(Instant.now());
                order.setUpdatedAt(Instant.now());
                order.setItems(new ArrayList<>());

                orderItem = new OrderItem();
                orderItem.setId(UUID.randomUUID());
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(2);
                orderItem.setUnitPrice(new BigDecimal("199.90"));

                order.getItems().add(orderItem);
        }

        @Test
        void shouldConfirmPendingOrder() {
                when(orderRepository.findByIdWithLock(order.getId()))
                                .thenReturn(Optional.of(order));

                var response = orderService.confirmOrder(order.getId());

                assertThat(response.id()).isEqualTo(order.getId());
                assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

                // The stock was already reserved during order creation.
                assertThat(product.getStockQuantity()).isEqualTo(7);

                verify(orderRepository).findByIdWithLock(order.getId());
                verify(productRepository, never()).findByIdWithLock(product.getId());
        }

        @Test
        void shouldThrowWhenConfirmingNonexistentOrder() {
                UUID orderId = UUID.randomUUID();

                when(orderRepository.findByIdWithLock(orderId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.confirmOrder(orderId))
                                .isInstanceOf(OrderNotFoundException.class);

                verify(orderRepository).findByIdWithLock(orderId);
                verify(productRepository, never()).findByIdWithLock(product.getId());
        }

        @Test
        void shouldRejectConfirmingNonPendingOrder() {
                order.setStatus(OrderStatus.CONFIRMED);

                when(orderRepository.findByIdWithLock(order.getId()))
                                .thenReturn(Optional.of(order));

                assertThatThrownBy(() -> orderService.confirmOrder(order.getId()))
                                .isInstanceOf(InvalidOrderStatusException.class);

                assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                assertThat(product.getStockQuantity()).isEqualTo(7);

                verify(productRepository, never()).findByIdWithLock(product.getId());
        }

        @Test
        void shouldCancelPendingOrderAndRestoreStock() {
                when(orderRepository.findByIdWithLock(order.getId()))
                                .thenReturn(Optional.of(order));
                when(productRepository.findByIdWithLock(product.getId()))
                                .thenReturn(Optional.of(product));

                var response = orderService.cancelOrder(order.getId());

                assertThat(response.id()).isEqualTo(order.getId());
                assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                assertThat(product.getStockQuantity()).isEqualTo(9);

                verify(orderRepository).findByIdWithLock(order.getId());
                verify(productRepository).findByIdWithLock(product.getId());
        }

        @Test
        void shouldThrowWhenCancellingNonexistentOrder() {
                UUID orderId = UUID.randomUUID();

                when(orderRepository.findByIdWithLock(orderId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                                .isInstanceOf(OrderNotFoundException.class);

                verify(orderRepository).findByIdWithLock(orderId);
                verify(productRepository, never()).findByIdWithLock(product.getId());
        }

        @Test
        void shouldRejectCancellingNonPendingOrder() {
                order.setStatus(OrderStatus.CONFIRMED);

                when(orderRepository.findByIdWithLock(order.getId()))
                                .thenReturn(Optional.of(order));

                assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                                .isInstanceOf(InvalidOrderStatusException.class);

                assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                assertThat(product.getStockQuantity()).isEqualTo(7);

                verify(productRepository, never()).findByIdWithLock(product.getId());
        }

        @Test
        void shouldRejectCancellingAlreadyCancelledOrder() {
                order.setStatus(OrderStatus.CANCELLED);

                when(orderRepository.findByIdWithLock(order.getId()))
                                .thenReturn(Optional.of(order));

                assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                                .isInstanceOf(InvalidOrderStatusException.class);

                assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                assertThat(product.getStockQuantity()).isEqualTo(7);

                verify(productRepository, never()).findByIdWithLock(product.getId());
        }

        @Test
        void shouldThrowWhenProductOfOrderNoLongerExists() {
                when(orderRepository.findByIdWithLock(order.getId()))
                                .thenReturn(Optional.of(order));
                when(productRepository.findByIdWithLock(product.getId()))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                                .isInstanceOf(
                                                com.lukete.ecommerce.exception.ProductNotFoundException.class);

                assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                assertThat(product.getStockQuantity()).isEqualTo(7);
        }
}
