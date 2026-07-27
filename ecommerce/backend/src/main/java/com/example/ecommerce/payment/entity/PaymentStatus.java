package com.example.ecommerce.payment.entity;

/**
 * 결제 상태. 계약 types.ts 의 PaymentStatus 와 철자·값이 동일해야 한다.
 * PENDING(대기) → SUCCESS(성공) | FAILED(실패).
 */
public enum PaymentStatus {

    PENDING,
    SUCCESS,
    FAILED
}
