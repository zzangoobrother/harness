package com.example.ecommerce.common.exception;

/**
 * 요청한 id 의 주문이 존재하지 않는 경우. 404 NOT_FOUND.
 */
public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
