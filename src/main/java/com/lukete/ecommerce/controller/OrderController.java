package com.lukete.ecommerce.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lukete.ecommerce.dto.ApiErrorResponse;
import com.lukete.ecommerce.dto.CreateOrderRequest;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Orders", description = "Operation for managing orders")
@RestController
@RequestMapping(path = "/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "Create order", description = "Creates an order with order items and return its details")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One or more products not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate product, inactive product, or insufficient stock", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List orders", description = "Lists all orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            Pageable pageable) {

        Page<OrderResponse> response = orderService.listOrders(pageable);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get order", description = "Details an order by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.findById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirm order", description = "Confirms an order and returns its details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order confirmed successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Order cannot be confirmed because it is not pending", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.confirmOrder(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel order", description = "Cancels an order and returns its details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Order or associated product not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Order cannot be cancelled because it is not pending", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

}