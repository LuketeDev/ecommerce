package com.lukete.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.lukete.ecommerce.domain.Order;
import com.lukete.ecommerce.domain.OrderItem;
import com.lukete.ecommerce.dto.OrderItemResponse;
import com.lukete.ecommerce.dto.OrderResponse;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(this::toResponse).toList(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    public OrderItemResponse toResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getProduct().getId(),
                orderItem.getProduct().getName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getSubtotal());
    }
}
