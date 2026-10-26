package com.lukete.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lukete.ecommerce.dto.CategoryResponse;
import com.lukete.ecommerce.service.CategoryService;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private CategoryService categoryService;

        @Test
        void shouldCreateCategory() throws Exception {
                UUID id = UUID.randomUUID();

                CategoryResponse response = new CategoryResponse(
                                id,
                                "Periféricos",
                                "Acessórios para computadores");

                when(categoryService.create(any()))
                                .thenReturn(response);

                String request = """
                                {
                                    "name": "Periféricos",
                                    "description": "Acessórios para computadores"
                                }
                                """;

                mockMvc.perform(post("/api/v1/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.name").value("Periféricos"))
                                .andExpect(jsonPath("$.description")
                                                .value("Acessórios para computadores"));
        }

        @Test
        void shouldListCategories() throws Exception {
                CategoryResponse category = new CategoryResponse(
                                UUID.randomUUID(),
                                "Monitores",
                                "Telas e monitores");

                when(categoryService.listCategories())
                                .thenReturn(List.of(category));

                mockMvc.perform(get("/api/v1/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].name").value("Monitores"));
        }

        @Test
        void shouldRejectBlankCategoryName() throws Exception {
                String request = """
                                {
                                    "name": "   ",
                                    "description": "Categoria inválida"
                                }
                                """;

                mockMvc.perform(post("/api/v1/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldRejectMissingCategoryName() throws Exception {
                String request = """
                                {
                                    "description": "Categoria sem nome"
                                }
                                """;

                mockMvc.perform(post("/api/v1/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldRejectMalformedJson() throws Exception {
                mockMvc.perform(post("/api/v1/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name":
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldRejectUnsupportedContentType() throws Exception {
                mockMvc.perform(post("/api/v1/categories")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("name=Periféricos"))
                                .andExpect(status().isUnsupportedMediaType())
                                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldReturnMethodNotAllowedForUnsupportedMethod() throws Exception {
                mockMvc.perform(patch("/api/v1/categories"))
                                .andExpect(status().isMethodNotAllowed())
                                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldReturnGenericErrorWhenUnexpectedExceptionOccurs() throws Exception {
                doThrow(new IllegalStateException("Internal database details"))
                                .when(categoryService).listCategories();

                mockMvc.perform(get("/api/v1/categories"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("Internal database details"))))
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldReturnEmptyArrayWhenNoCategoriesExist() throws Exception {
                when(categoryService.listCategories())
                                .thenReturn(List.of());

                mockMvc.perform(get("/api/v1/categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));
        }
}