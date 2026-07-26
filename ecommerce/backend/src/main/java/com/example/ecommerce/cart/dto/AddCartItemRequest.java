package com.example.ecommerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 장바구니 아이템 추가 요청. 계약 types.ts 의 AddCartItemRequest 와 일치.
 */
public record AddCartItemRequest(
        @NotNull(message = "productId는 필수입니다.")
        Long productId,

        @NotNull(message = "quantity는 필수입니다.")
        @Min(value = 1, message = "quantity는 1 이상이어야 합니다.")
        Integer quantity
) {
}
