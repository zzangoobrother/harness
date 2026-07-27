package com.example.ecommerce.order.dto;

/**
 * 주문 아이템 응답 DTO. 계약 types.ts 의 OrderItemResponse 와 필드명·타입·순서 일치.
 * priceAtOrder 는 주문 시점 단가 스냅샷, subtotal 은 priceAtOrder * quantity(정수, 원).
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        Integer priceAtOrder,
        Integer subtotal
) {
}
