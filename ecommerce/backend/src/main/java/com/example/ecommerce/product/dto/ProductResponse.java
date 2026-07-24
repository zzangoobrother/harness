package com.example.ecommerce.product.dto;

import java.time.Instant;

/**
 * 상품 응답 DTO. 계약 types.ts 의 Product 와 필드명·타입·순서 일치.
 * createdAt 은 Instant 로 두고 Jackson 이 ISO 8601 문자열로 직렬화한다.
 */
public record ProductResponse(
        Long id,
        String name,
        String description,
        Integer price,
        String imageUrl,
        Integer stock,
        String category,
        Instant createdAt
) {
}
