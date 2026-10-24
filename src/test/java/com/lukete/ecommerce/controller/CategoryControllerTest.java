package com.lukete.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name":
                        """))
                .andExpect(status().isBadRequest());
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