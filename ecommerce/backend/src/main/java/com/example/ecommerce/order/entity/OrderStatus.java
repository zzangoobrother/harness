package com.example.ecommerce.order.entity;

/**
 * 주문 상태. 계약 types.ts 의 OrderStatus 와 철자·값이 동일해야 한다.
 * 전이: PENDING → (결제성공) PAID / (결제실패) FAILED / (취소) CANCELLED.
 * MVP 에는 주문 취소 API 가 없어 CANCELLED 는 값만 예약한다(계약 가정 6).
 */
public enum OrderStatus {

    PENDING,
    PAID,
    FAILED,
    CANCELLED
}
