
package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.domain.OrderItem;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.InvalidOrderStatusException;
import com.lukete.ecommerce.exception.OrderNotFoundException;
import com.lukete.ecommerce.mapper.OrderMapper;
import com.lukete.ecommerce.repository.OrderRepository;
import com.lukete.ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Spy
        private OrderMapper orderMapper = new OrderMapper();

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

                var response = orderService.confirmOrder(order.getId()); // NPE

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

                var response = orderService.cancelOrder(order.getId()); // NPE

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

        @Test
        void shouldFindOrderByIdAndMapResponse() {
                OrderResponse expected = new OrderResponse(
                                order.getId(),
                                order.getStatus(),
                                List.of(),
                                order.getTotalAmount(),
                                order.getCreatedAt());

                when(orderRepository.findById(order.getId()))
                                .thenReturn(Optional.of(order));
                doReturn(expected).when(orderMapper).toResponse(order);

                OrderResponse actual = orderService.findById(order.getId());

                assertThat(actual).isSameAs(expected);

                verify(orderRepository).findById(order.getId());
                verify(orderMapper).toResponse(order); // TooManyActualInvocations
        }

        @Test
        void shouldThrowWhenFindingNonexistentOrder() {
                UUID orderId = UUID.randomUUID();

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.findById(orderId))
                                .isInstanceOf(OrderNotFoundException.class);

                verify(orderRepository).findById(orderId);
                verify(orderMapper, never()).toResponse(
                                org.mockito.ArgumentMatchers.any(Order.class));
        }

        @Test
        void shouldListOrdersAndMapEachResult() {
                Order secondOrder = new Order();
                secondOrder.setId(UUID.randomUUID());
                secondOrder.setStatus(OrderStatus.PENDING);
                secondOrder.setTotalAmount(new BigDecimal("50.00"));
                secondOrder.setCreatedAt(Instant.now());
                secondOrder.setUpdatedAt(Instant.now());
                secondOrder.setItems(new ArrayList<>());

                Pageable pageable = PageRequest.of(0, 5);
                Page<Order> orderPage = new PageImpl<>(
                                List.of(order, secondOrder),
                                pageable,
                                2);

                OrderResponse firstResponse = new OrderResponse(
                                order.getId(),
                                order.getStatus(),
                                List.of(),
                                order.getTotalAmount(),
                                order.getCreatedAt());

                OrderResponse secondResponse = new OrderResponse(
                                secondOrder.getId(),
                                secondOrder.getStatus(),
                                List.of(),
                                secondOrder.getTotalAmount(),
                                secondOrder.getCreatedAt());

                when(orderRepository.findAll(pageable)).thenReturn(orderPage);
                doReturn(firstResponse).when(orderMapper).toResponse(order);
                doReturn(secondResponse).when(orderMapper).toResponse(secondOrder);

                Page<OrderResponse> result = orderService.listOrders(pageable);

                assertThat(result.getContent())
                                .containsExactly(firstResponse, secondResponse);
                assertThat(result.getNumber()).isZero();
                assertThat(result.getSize()).isEqualTo(5);
                assertThat(result.getTotalElements()).isEqualTo(2);

                verify(orderRepository).findAll(pageable);
                verify(orderMapper, times(2))
                                .toResponse(org.mockito.ArgumentMatchers.any(Order.class)); // TooManyActualInvocations
        }

}
