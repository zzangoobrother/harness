package com.example.ecommerce.cart.dto;

/**
 * 장바구니 아이템 응답 DTO. 계약 types.ts 의 CartItemResponse 와 필드명·타입·순서 일치.
 * id 는 CartItem id (Product id 가 아님)에 주의.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        Integer price,
        Integer quantity,
        Integer subtotal
) {
}
