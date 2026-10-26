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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lukete.ecommerce.domain.ProductSpecification;
import com.lukete.ecommerce.dto.ApiErrorResponse;
import com.lukete.ecommerce.dto.CreateProductRequest;
import com.lukete.ecommerce.dto.ProductResponse;
import com.lukete.ecommerce.dto.ProductStockResponse;
import com.lukete.ecommerce.dto.UpdateProductRequest;
import com.lukete.ecommerce.dto.UpdateProductStockRequest;
import com.lukete.ecommerce.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Products", description = "Operations for managing products")
@RestController
@RequestMapping(path = "/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
        private final ProductService productService;

        @Operation(summary = "Create product", description = "Creates a product and returns its details")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Product created"),
                        @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @PostMapping
        public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
                ProductResponse response = productService.create(request);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @Operation(summary = "List products", description = "Returns the details of all products")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Products listed")
        })
        @GetMapping
        public ResponseEntity<Page<ProductResponse>> listProducts(
                        Pageable pageable,
                        @RequestParam(required = false) UUID categoryId,
                        @RequestParam(required = false) Boolean active) {

                ProductSpecification specification = new ProductSpecification(categoryId, active);

                Page<ProductResponse> response = productService.listProducts(pageable, specification);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get product", description = "Returns the details of a product by its ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Product found"),
                        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
                ProductResponse response = productService.findById(id);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get product stock", description = "Returns the stock of a product by its ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Product found"),
                        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @GetMapping("/{id}/stock")
        public ResponseEntity<ProductStockResponse> getProductStock(@PathVariable UUID id) {
                ProductStockResponse response = productService.getProductStock(id);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Update product stock", description = "Updates a product stock and returns its details by its ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Product stock updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid stock operation or quantity", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "409", description = "Cannot reduce stock of an inactive product", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @PatchMapping("/{id}/stock")
        public ResponseEntity<ProductStockResponse> updateProductStock(@PathVariable UUID id,
                        @Valid @RequestBody UpdateProductStockRequest request) {
                ProductStockResponse response = productService.updateProductStock(id, request);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Update product", description = "Updates a product and returns its details by its ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid product data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Product or category not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))

        })

        @PutMapping("/{id}")
        public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id,
                        @Valid @RequestBody UpdateProductRequest request) {
                ProductResponse response = productService.updateProduct(id, request);
                return ResponseEntity.ok(response);
        }
}