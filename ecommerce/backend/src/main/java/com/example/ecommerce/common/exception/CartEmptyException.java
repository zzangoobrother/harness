package com.example.ecommerce.common.exception;

/**
 * 빈 장바구니로 체크아웃을 시도한 경우. 409 CART_EMPTY.
 */
public class CartEmptyException extends BusinessException {

    public CartEmptyException(String message) {
        super(ErrorCode.CART_EMPTY, message);
    }
}
