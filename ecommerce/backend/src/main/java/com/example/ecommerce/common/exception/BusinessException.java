package com.example.ecommerce.common.exception;

/**
 * 도메인 비즈니스 예외의 기반 클래스.
 * ErrorCode 를 담아 GlobalExceptionHandler 가 표준 에러 응답으로 변환한다.
 * 각 도메인은 이 클래스를 상속하거나 직접 throw 하여 사용한다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
