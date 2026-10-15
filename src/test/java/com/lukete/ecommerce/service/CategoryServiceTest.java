package com.lukete.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.dto.CategoryResponse;
import com.lukete.ecommerce.dto.CreateCategoryRequest;
import com.lukete.ecommerce.exception.CategoryAlreadyExistsException;
import com.lukete.ecommerce.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        // O Mockito inicializa os mocks por meio da extensão JUnit.
    }

    @Test
    void shouldCreateCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Periféricos",
                "Acessórios para computadores");

        when(categoryRepository.existsByNameIgnoreCase("Periféricos"))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(UUID.randomUUID());
                    return category;
                });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Periféricos");
        assertThat(response.description())
                .isEqualTo("Acessórios para computadores");

        verify(categoryRepository)
                .existsByNameIgnoreCase("Periféricos");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldRejectDuplicateCategoryNameIgnoringCase() {
        CreateCategoryRequest request = new CreateCategoryRequest("PERIFÉRICOS", null);

        when(categoryRepository.existsByNameIgnoreCase("PERIFÉRICOS"))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryAlreadyExistsException.class);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void shouldListCategories() {
        Category first = new Category();
        first.setId(UUID.randomUUID());
        first.setName("Periféricos");
        first.setDescription("Acessórios");

        Category second = new Category();
        second.setId(UUID.randomUUID());
        second.setName("Monitores");
        second.setDescription(null);

        when(categoryRepository.findAll())
                .thenReturn(List.of(first, second));

        List<CategoryResponse> result = categoryService.listCategories();

        assertThat(result)
                .extracting(CategoryResponse::name)
                .containsExactly("Periféricos", "Monitores");

        assertThat(result.get(1).description()).isNull();
    }

    @Test
    void shouldReturnEmptyListWhenNoCategoriesExist() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThat(categoryService.listCategories()).isEmpty();
    }
}