package com.example.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 계약(api-spec.md)이 정의한 표준 에러 코드와 HTTP 상태의 매핑.
 * 이후 상품/장바구니/주문/결제 도메인도 이 enum을 재사용한다.
 */
public enum ErrorCode {

    // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN),
    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND),
    // 409
    EMAIL_DUPLICATED(HttpStatus.CONFLICT),
    OUT_OF_STOCK(HttpStatus.CONFLICT),
    ALREADY_PAID(HttpStatus.CONFLICT),
    CART_EMPTY(HttpStatus.CONFLICT),
    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** JSON 응답의 code 값(enum 이름과 동일). */
    public String getCode() {
        return name();
    }
}
