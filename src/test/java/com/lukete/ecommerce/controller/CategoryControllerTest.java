package com.lukete.ecommerce.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lukete.ecommerce.infrastructure.PostgresIntegrationTest;
import com.lukete.ecommerce.repository.CategoryRepository;

@AutoConfigureMockMvc
class CategoryControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void cleanDatabase() {
        categoryRepository.deleteAll();
    }

    @Test
    void shouldCreateCategory() throws Exception {
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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Periféricos"))
                .andExpect(jsonPath("$.description")
                        .value("Acessórios para computadores"));
    }

    @Test
    void shouldListCategories() throws Exception {
        String request = """
                {
                    "name": "Monitores",
                    "description": "Telas e monitores"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());

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
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}