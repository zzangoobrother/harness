package com.example.ecommerce.common.exception;

/**
 * 회원가입 시 이미 존재하는 이메일인 경우. 409 EMAIL_DUPLICATED.
 */
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String message) {
        super(ErrorCode.EMAIL_DUPLICATED, message);
    }
}
