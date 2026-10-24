package com.lukete.ecommerce.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lukete.ecommerce.domain.ProductSpecification;
import com.lukete.ecommerce.dto.ProductResponse;
import com.lukete.ecommerce.service.ProductService;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProductService productService;

        @Test
        void shouldReturnFirstPageOfProducts() throws Exception {
                Page<ProductResponse> page = new PageImpl<>(
                                List.of(
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 1", "Teste 1",
                                                                new BigDecimal("12.90"),
                                                                UUID.randomUUID(), "Periféricos", true)),
                                PageRequest.of(0, 5),
                                1);

                when(productService.listProducts(
                                any(Pageable.class),
                                any(ProductSpecification.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void shouldReturnSecondPageOfProducts() throws Exception {
                Page<ProductResponse> firstPage = new PageImpl<>(
                                List.of(
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 1", "Teste 1",
                                                                new BigDecimal("12.90"),
                                                                UUID.randomUUID(), "Periféricos", true),
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 2", "Teste 2",
                                                                new BigDecimal("13.90"),
                                                                UUID.randomUUID(), "Periféricos", true),
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 3", "Teste 3",
                                                                new BigDecimal("14.90"),
                                                                UUID.randomUUID(), "Periféricos", true),
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 4", "Teste 4",
                                                                new BigDecimal("15.90"),
                                                                UUID.randomUUID(), "Periféricos", true),
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 5", "Teste 5",
                                                                new BigDecimal("16.90"),
                                                                UUID.randomUUID(), "Periféricos", true)),
                                PageRequest.of(0, 5),
                                6);
                Page<ProductResponse> secondPage = new PageImpl<>(
                                List.of(
                                                new ProductResponse(UUID.randomUUID(), "Produto teste 6", "Teste 6",
                                                                new BigDecimal("17.90"), UUID.randomUUID(),
                                                                "Periféricos", true)),
                                PageRequest.of(1, 5),
                                6);

                when(productService.listProducts(any(Pageable.class), any(ProductSpecification.class)))
                                .thenAnswer(invocation -> {
                                        Pageable pageable = invocation.getArgument(0);
                                        return pageable.getPageNumber() == 0
                                                        ? firstPage
                                                        : secondPage;
                                });

                mockMvc.perform(get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(5))
                                .andExpect(jsonPath("$.totalElements").value(6))
                                .andExpect(jsonPath("$.totalPages").value(2));
                mockMvc.perform(get("/api/v1/products")
                                .param("page", "1")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.totalElements").value(6))
                                .andExpect(jsonPath("$.totalPages").value(2));
        }

        @ParameterizedTest(name = "Ordering {0} should map to {1}")
        @CsvSource({
                        "asc, ASC",
                        "desc, DESC"
        })
        void shouldOrderProductsByPrice(String sort, Sort.Direction expectedSort) throws Exception {
                Page<ProductResponse> page = new PageImpl<>(
                                List.of(
                                                new ProductResponse(
                                                                UUID.randomUUID(), "Produto teste 1", "Teste 1",
                                                                new BigDecimal("11.90"), UUID.randomUUID(),
                                                                "Periféricos", true),
                                                new ProductResponse(
                                                                UUID.randomUUID(), "Produto teste 2", "Teste 2",
                                                                new BigDecimal("12.90"), UUID.randomUUID(),
                                                                "Periféricos", true)),
                                PageRequest.of(0, 5),
                                2);

                when(productService.listProducts(
                                any(Pageable.class),
                                any(ProductSpecification.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "5")
                                .param("sort", "price," + sort))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1));

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

                verify(productService).listProducts(
                                pageableCaptor.capture(),
                                any(ProductSpecification.class));

                Pageable pageable = pageableCaptor.getValue();
                Sort.Order priceOrder = pageable.getSort().getOrderFor("price");

                assertThat(priceOrder).isNotNull();
                assertThat(priceOrder.getDirection()).isEqualTo(expectedSort);
                assertThat(pageable.getPageNumber()).isZero();
                assertThat(pageable.getPageSize()).isEqualTo(5);
        }

        @Test
        void shouldUseDefaultParametersWhenNoParametersUsed() throws Exception {
                Page<ProductResponse> page = new PageImpl<>(
                                List.of(
                                                new ProductResponse(
                                                                UUID.randomUUID(), "Produto teste 1", "Teste 1",
                                                                new BigDecimal("11.90"), UUID.randomUUID(),
                                                                "Periféricos", true),
                                                new ProductResponse(
                                                                UUID.randomUUID(), "Produto teste 2", "Teste 2",
                                                                new BigDecimal("12.90"), UUID.randomUUID(),
                                                                "Periféricos", true)),
                                PageRequest.of(0, 5),
                                2);

                when(productService.listProducts(
                                any(Pageable.class),
                                any(ProductSpecification.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/v1/products"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1));

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

                verify(productService).listProducts(
                                pageableCaptor.capture(),
                                any(ProductSpecification.class));

                Pageable pageable = pageableCaptor.getValue();

                assertThat(pageable.getPageNumber()).isZero();
                assertThat(pageable.getPageSize()).isEqualTo(20);
        }

        @Test
        void shouldReturnEmptyPageWhenPageHasNoResults() throws Exception {
                Page<ProductResponse> emptyPage = new PageImpl<>(
                                List.of(),
                                PageRequest.of(10, 5),
                                0);

                when(productService.listProducts(any(Pageable.class), any(ProductSpecification.class)))
                                .thenReturn(emptyPage);

                mockMvc.perform(get("/api/v1/products")
                                .param("page", "10")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty()) // IllegalArgumentException
                                .andExpect(jsonPath("$.number").value(10))
                                .andExpect(jsonPath("$.size").value(5))
                                .andExpect(jsonPath("$.totalElements").value(0))
                                .andExpect(jsonPath("$.totalPages").value(0));
        }

        @Test
        void findAllShouldReturnPaginationMetadata() throws Exception {
                Page<ProductResponse> page = new PageImpl<>(
                                List.of(
                                                new ProductResponse(
                                                                UUID.randomUUID(), "Produto teste 1", "Teste 1",
                                                                new BigDecimal("11.90"), UUID.randomUUID(),
                                                                "Periféricos", true)),
                                PageRequest.of(0, 5),
                                0);

                when(productService.listProducts(
                                any(Pageable.class),
                                any(ProductSpecification.class)))
                                .thenReturn(page);

                mockMvc.perform(get("/api/v1/products")
                                .param("page", "0")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.number").value(0)) // IllegalArgumentException
                                .andExpect(jsonPath("$.size").value(5))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.totalPages").value(1));
        }
}
