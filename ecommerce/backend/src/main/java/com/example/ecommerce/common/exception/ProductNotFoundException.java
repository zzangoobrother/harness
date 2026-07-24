package com.example.ecommerce.common.exception;

/**
 * 요청한 id 의 상품이 존재하지 않는 경우. 404 NOT_FOUND.
 */
public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
