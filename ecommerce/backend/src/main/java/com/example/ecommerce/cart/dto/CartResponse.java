package com.example.ecommerce.cart.dto;

import java.util.List;

/**
 * 장바구니 전체 응답 DTO. 계약 types.ts 의 CartResponse 와 필드명·타입·순서 일치.
 * 조회/추가/수정/삭제 모든 엔드포인트가 이 shape 전체를 반환한다(계약 3절).
 */
public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        Integer totalAmount
) {
}
