package com.example.ecommerce.product.mapper;

import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.entity.Product;

/**
 * Product 엔티티 → ProductResponse 변환.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getStock(),
                product.getCategory(),
                product.getCreatedAt()
        );
    }
}
