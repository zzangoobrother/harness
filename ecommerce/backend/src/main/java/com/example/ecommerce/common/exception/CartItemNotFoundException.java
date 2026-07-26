package com.example.ecommerce.common.exception;

/**
 * 요청한 id 의 장바구니 아이템이 존재하지 않는 경우. 404 NOT_FOUND.
 */
public class CartItemNotFoundException extends BusinessException {

    public CartItemNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
