package com.lukete.ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lukete.ecommerce.dto.ApiErrorResponse;
import com.lukete.ecommerce.dto.CategoryResponse;
import com.lukete.ecommerce.dto.CreateCategoryRequest;
import com.lukete.ecommerce.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Categories", description = "Operations for managing categories")
@RestController
@RequestMapping(path = "/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "Create a category", description = "Creates and returns a category")

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "409", description = "Category already registered", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })

    @PostMapping()
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories listed")
    })

    @Operation(summary = "List categories", description = "Returns the details of all categories.")
    @GetMapping()
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        List<CategoryResponse> response = categoryService.listCategories();
        return ResponseEntity.ok(response);
    }

}