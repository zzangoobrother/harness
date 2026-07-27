package com.example.ecommerce.common.exception;

/**
 * 이미 결제(SUCCESS)가 완료된 주문에 재결제를 시도한 경우. 409 ALREADY_PAID.
 * 결제 "실패(FAILED)" 주문의 재시도는 계약상 허용되므로 이 예외를 던지지 않는다.
 */
public class AlreadyPaidException extends BusinessException {

    public AlreadyPaidException(String message) {
        super(ErrorCode.ALREADY_PAID, message);
    }
}
