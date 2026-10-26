
package com.lukete.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lukete.ecommerce.dto.CreateOrderRequest;
import com.lukete.ecommerce.dto.OrderItemResponse;
import com.lukete.ecommerce.dto.OrderResponse;
import com.lukete.ecommerce.enums.OrderStatus;
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
}
