package com.lukete.ecommerce.mapper;

import org.springframework.stereotype.Component;

import com.lukete.ecommerce.domain.Product;
import com.lukete.ecommerce.dto.ProductResponse;

@Component
public class ProductMapper {
    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.isActive());
    }
}
