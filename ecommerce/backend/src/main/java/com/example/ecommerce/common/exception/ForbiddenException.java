package com.example.ecommerce.common.exception;

/**
 * 본인 소유가 아닌 리소스(장바구니 아이템, 주문 등)에 접근/조작하려는 경우. 403 FORBIDDEN.
 * 도메인 전반에서 재사용한다.
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
