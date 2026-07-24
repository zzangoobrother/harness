package com.example.ecommerce.common.exception;

/**
 * 목록 조회 시 page/size 쿼리 파라미터가 유효하지 않은 경우(음수 page, 0 이하 size 등).
 * 400 VALIDATION_ERROR.
 */
public class InvalidPageRequestException extends BusinessException {

    public InvalidPageRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
