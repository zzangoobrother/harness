package com.example.ecommerce.payment.dto;

/**
 * 모의결제 실행 요청. 계약 types.ts 의 PaymentRequest 와 일치한다.
 * simulateSuccess 는 선택 필드로, 생략(null)하면 true(성공)로 간주한다.
 * 따라서 원시 타입 boolean 이 아니라 래퍼 타입 Boolean 으로 선언해야 "생략"과 "false"를 구분할 수 있다.
 */
public record PaymentRequest(
        Boolean simulateSuccess
) {
    /** 생략 시 true(성공)로 간주하는 계약 규칙을 한 곳에서 처리한다. */
    public boolean resolveSimulateSuccess() {
        return simulateSuccess == null || simulateSuccess;
    }
}
