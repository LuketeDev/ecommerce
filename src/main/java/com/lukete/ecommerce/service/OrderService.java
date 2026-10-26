package com.lukete.ecommerce.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.domain.OrderItem;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.CreateOrderItemRequest;
import com.lukete.ecommerce.dto.CreateOrderRequest;
import com.lukete.ecommerce.dto.OrderItemResponse;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.DuplicateOrderItemException;
import com.lukete.ecommerce.exception.InactiveProductException;
import com.lukete.ecommerce.exception.InsufficientProductStockException;
import com.lukete.ecommerce.exception.InvalidOrderStatusException;
import com.lukete.ecommerce.exception.OrderNotFoundException;
import com.lukete.ecommerce.exception.ProductNotFoundException;
import com.lukete.ecommerce.repository.OrderRepository;
import com.lukete.ecommerce.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Map<UUID, Product> products = new HashMap<>();

        // removes duplicates
        Set<UUID> productIds = new HashSet<>();
        for (CreateOrderItemRequest item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new DuplicateOrderItemException(
                        "Product already requested for order with id: " + item.productId());
            }
        }

        // gets locks always in same order
        List<UUID> sortedProductIds = productIds.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        for (UUID productId : sortedProductIds) {
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product not found with id: " + productId));

            products.put(productId, product);
        }

        // validates all products before updating stock
        for (CreateOrderItemRequest item : request.items()) {
            Product product = products.get(item.productId());

            if (!product.isActive()) {
                throw new InactiveProductException(
                        "Product inactive with id: " + product.getId());
            }

            if (product.getStockQuantity() < item.quantity()) {
                throw new InsufficientProductStockException(
                        "Product stock not enough with id: " + product.getId()
                                + " and quantity: " + item.quantity());
            }
        }

        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        for (CreateOrderItemRequest item : request.items()) {
            Product product = products.get(item.productId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setUnitPrice(product.getPrice());

            order.getItems().add(orderItem);

            order.setTotalAmount(order.getTotalAmount().add(orderItem.getSubtotal()));
        }
        for (CreateOrderItemRequest item : request.items()) {
            Product product = products.get(item.productId());
            product.reduceStock(item.quantity());
        }

        Order savedOrder = orderRepository.save(order);

        return createResponse(savedOrder);
    }

    @Transactional
    public OrderResponse confirmOrder(UUID orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Order is not pending with id: " + orderId);
        }

        order.setStatus(OrderStatus.CONFIRMED);

        return createResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    "Order is not pending with id: " + orderId);
        }

        Map<UUID, Product> products = new HashMap<>();

        // Collect product IDs from order items
        Set<UUID> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(java.util.stream.Collectors.toSet());

        // Acquire locks in a deterministic order
        List<UUID> sortedProductIds = productIds.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        for (UUID productId : sortedProductIds) {
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductNotFoundException(
                            "Product not found with id: " + productId));

            products.put(productId, product);
        }

        // Restore the quantity reserved by each order item
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProduct().getId());
            product.restoreStock(item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);

        return createResponse(order);
    }

    private OrderResponse createResponse(Order order) {
        return new OrderResponse(order.getId(),
                order.getStatus(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()))
                        .toList(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
