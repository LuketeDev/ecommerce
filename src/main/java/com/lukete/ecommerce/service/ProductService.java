package com.lukete.ecommerce.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.CreateProductRequest;
import com.lukete.ecommerce.dto.ProductResponse;
import com.lukete.ecommerce.dto.UpdateProductRequest;
import com.lukete.ecommerce.exception.CategoryNotFoundException;
import com.lukete.ecommerce.exception.ProductNotFoundException;
import com.lukete.ecommerce.mapper.ProductMapper;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponse create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(
                        () -> new CategoryNotFoundException("Category not found with id " + request.categoryId()));
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    public List<ProductResponse> listProducts() {
        List<Product> result = productRepository.findAll();
        return result.stream().map(productMapper::toResponse).toList();
    }

    public ProductResponse findById(UUID id) {
        Product result = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        return productMapper.toResponse(result);
    }

    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId));
        Category category = categoryRepository.findById(request.categoryId()).orElseThrow(
                () -> new CategoryNotFoundException("Category not found with id " + request.categoryId()));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);
        product.setActive(request.active());
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }
}