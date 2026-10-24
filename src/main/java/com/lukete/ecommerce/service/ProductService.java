package com.lukete.ecommerce.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lukete.ecommerce.domain.Category;
import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.domain.ProductSpecification;
import com.lukete.ecommerce.dto.CreateProductRequest;
import com.lukete.ecommerce.dto.ProductResponse;
import com.lukete.ecommerce.dto.ProductStockResponse;
import com.lukete.ecommerce.dto.UpdateProductRequest;
import com.lukete.ecommerce.dto.UpdateProductStockRequest;
import com.lukete.ecommerce.exception.CategoryNotFoundException;
import com.lukete.ecommerce.exception.InactiveProductException;
import com.lukete.ecommerce.exception.InvalidStockOperationException;
import com.lukete.ecommerce.exception.InvalidStockQuantityException;
import com.lukete.ecommerce.exception.ProductNotFoundException;
import com.lukete.ecommerce.mapper.ProductMapper;
import com.lukete.ecommerce.repository.CategoryRepository;
import com.lukete.ecommerce.repository.ProductRepository;

import jakarta.transaction.Transactional;
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

    public Page<ProductResponse> listProducts(Pageable pageable, ProductSpecification specification) {
        Page<Product> productsPage = productRepository.findAll(specification, pageable);
        return productsPage.map(productMapper::toResponse);
    }

    public ProductResponse findById(UUID id) {
        Product result = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        return productMapper.toResponse(result);
    }

    public ProductStockResponse getProductStock(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId));
        return new ProductStockResponse(product.getId(), product.getStockQuantity());
    }

    @Transactional
    public ProductStockResponse updateProductStock(UUID productId, UpdateProductStockRequest request) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + productId));

        switch (request.operationType()) {
            case ADD:
                try {
                    product.setStockQuantity(
                            Math.addExact(product.getStockQuantity(), request.amount()));
                } catch (ArithmeticException exception) {
                    throw new InvalidStockQuantityException(
                            "Stock quantity exceeds the maximum allowed value");
                }
                break;
            case REDUCE:
                if (request.amount() > product.getStockQuantity()) {
                    throw new InvalidStockQuantityException(
                            "Amount for reduction (%d) is greater than current stock (%d)"
                                    .formatted(request.amount(), product.getStockQuantity()));
                }
                if (!product.isActive()) {
                    throw new InactiveProductException("Product is inactive: " + product.getId());
                }
                product.setStockQuantity(product.getStockQuantity() - request.amount());
                break;
            default:
                throw new InvalidStockOperationException("Invalid stock operation");
        }

        productRepository.save(product);
        return new ProductStockResponse(product.getId(), product.getStockQuantity());
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