package com.example.ecommerce.common.exception;

/**
 * 로그인 시 이메일/비밀번호가 일치하지 않는 경우. 401 INVALID_CREDENTIALS.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
