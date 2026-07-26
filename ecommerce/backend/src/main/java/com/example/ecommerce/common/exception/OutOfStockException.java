package com.example.ecommerce.common.exception;

/**
 * 요청 수량이 상품 재고를 초과하는 경우. 409 OUT_OF_STOCK.
 */
public class OutOfStockException extends BusinessException {

    public OutOfStockException(String message) {
        super(ErrorCode.OUT_OF_STOCK, message);
    }
}
