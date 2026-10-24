package com.lukete.ecommerce.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ProductSpecificationTest {

        @Mock
        private Root<Product> root;

        @Mock
        private CriteriaQuery<?> query;

        @Mock
        private CriteriaBuilder criteriaBuilder;

        @Mock
        private Path<Object> categoryPath;

        @Mock
        private Path<Object> categoryIdPath;

        @Mock
        private Path<Object> activePath;

        @Mock
        private Predicate categoryPredicate;

        @Mock
        private Predicate activePredicate;

        @Mock
        private Predicate combinedPredicate;

        private UUID categoryId;

        @BeforeEach
        void setUp() {
                categoryId = UUID.randomUUID();
                when(root.get("active")).thenReturn(activePath);
        }

        @Test
        void shouldReturnPredicateWithoutFilters() {
                when(criteriaBuilder.equal(activePath, true))
                                .thenReturn(activePredicate);
                doReturn(combinedPredicate)
                                .when(criteriaBuilder)
                                .and(any(Predicate[].class));

                ProductSpecification specification = new ProductSpecification(null, null);

                Predicate result = specification.toPredicate(root, query, criteriaBuilder);

                assertThat(result).isSameAs(combinedPredicate);
                verify(criteriaBuilder).equal(activePath, true);
                verify(criteriaBuilder, never()).equal(categoryIdPath, categoryId);
        }

        @ParameterizedTest
        @MethodSource("activeFilterCases")
        void shouldFilterByActiveStatus(Boolean active) {
                when(criteriaBuilder.equal(activePath, active))
                                .thenReturn(activePredicate);
                doReturn(combinedPredicate)
                                .when(criteriaBuilder)
                                .and(any(Predicate[].class));

                ProductSpecification specification = new ProductSpecification(null, active);

                Predicate result = specification.toPredicate(root, query, criteriaBuilder);

                assertThat(result).isSameAs(combinedPredicate);
                verify(criteriaBuilder).equal(activePath, active);
                verify(criteriaBuilder, never()).equal(categoryIdPath, categoryId);
        }

        static Stream<Boolean> activeFilterCases() {
                return Stream.of(true, false);
        }

        @Test
        void shouldFilterByCategory() {
                when(root.get("category")).thenReturn(categoryPath);
                when(categoryPath.get("id")).thenReturn(categoryIdPath);
                when(criteriaBuilder.equal(categoryIdPath, categoryId))
                                .thenReturn(categoryPredicate);
                when(criteriaBuilder.equal(activePath, true))
                                .thenReturn(activePredicate);
                doReturn(combinedPredicate)
                                .when(criteriaBuilder)
                                .and(any(Predicate[].class));

                ProductSpecification specification = new ProductSpecification(categoryId, null);

                Predicate result = specification.toPredicate(root, query, criteriaBuilder);

                assertThat(result).isSameAs(combinedPredicate);
                verify(criteriaBuilder).equal(categoryIdPath, categoryId);
                verify(criteriaBuilder).equal(activePath, true);
                verify(criteriaBuilder, never()).equal(activePath, false);
        }

        @ParameterizedTest
        @MethodSource("combinedFilterCases")
        void shouldFilterByCategoryAndActiveStatus(Boolean active) {
                when(root.get("category")).thenReturn(categoryPath);
                when(categoryPath.get("id")).thenReturn(categoryIdPath);
                when(criteriaBuilder.equal(categoryIdPath, categoryId))
                                .thenReturn(categoryPredicate);
                when(criteriaBuilder.equal(activePath, active))
                                .thenReturn(activePredicate);
                doReturn(combinedPredicate)
                                .when(criteriaBuilder)
                                .and(any(Predicate[].class));

                ProductSpecification specification = new ProductSpecification(categoryId, active);

                Predicate result = specification.toPredicate(root, query, criteriaBuilder);

                assertThat(result).isSameAs(combinedPredicate);
                verify(criteriaBuilder).equal(categoryIdPath, categoryId);
                verify(criteriaBuilder).equal(activePath, active);
        }

        static Stream<Boolean> combinedFilterCases() {
                return Stream.of(true, false);
        }
}