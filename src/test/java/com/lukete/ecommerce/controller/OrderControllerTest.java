
package com.lukete.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lukete.ecommerce.dto.CreateOrderRequest;
import com.lukete.ecommerce.dto.OrderItemResponse;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.enums.OrderStatus;
import com.lukete.ecommerce.exception.OrderNotFoundException;
import com.lukete.ecommerce.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-25T12:00:00Z");

        OrderResponse response = new OrderResponse(
                orderId,
                OrderStatus.PENDING,
                List.of(new OrderItemResponse(
                        productId,
                        "Teclado mecânico",
                        2,
                        new BigDecimal("150.00"),
                        new BigDecimal("300.00"))),
                new BigDecimal("300.00"),
                createdAt);

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(response);

        String request = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Teclado mecânico"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(150.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void shouldRejectEmptyItems() throws Exception {
        String request = """
                {
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingItems() throws Exception {
        String request = """
                {}
                """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectItemWithoutProductId() throws Exception {
        String request = """
                {
                    "items": [
                        {
                            "quantity": 2
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectItemWithZeroQuantity() throws Exception {
        String request = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "quantity": 0
                        }
                    ]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectItemWithNegativeQuantity() throws Exception {
        String request = """
                {
                    "items": [
                        {
                            "productId": "%s",
                            "quantity": -1
                        }
                    ]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "items":
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-25T12:00:00Z");

        OrderResponse response = new OrderResponse(
                orderId,
                OrderStatus.PENDING,
                List.of(new OrderItemResponse(
                        productId,
                        "Teclado mecânico",
                        2,
                        new BigDecimal("150.00"),
                        new BigDecimal("300.00"))),
                new BigDecimal("300.00"),
                createdAt);

        when(orderService.findById(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Teclado mecânico"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(150.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

        verify(orderService).findById(orderId);
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(orderService.findById(orderId))
                .thenThrow(new OrderNotFoundException(
                        "Order not found with id: " + orderId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNotFound());

        verify(orderService).findById(orderId);
    }

    @Test
    void shouldReturnPageOfOrders() throws Exception {
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-25T12:00:00Z");

        OrderResponse response = new OrderResponse(
                orderId,
                OrderStatus.PENDING,
                List.of(),
                new BigDecimal("0.00"),
                createdAt);

        Page<OrderResponse> page = new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 5),
                11);

        when(orderService.listOrders(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(orderService).listOrders(any(Pageable.class));
    }

    @Test
    void shouldUseDefaultPaginationWhenNoParametersAreProvided()
            throws Exception {
        Page<OrderResponse> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0);

        when(orderService.listOrders(any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(orderService).listOrders(any(Pageable.class));
    }

}
