package com.example.ecommerce.payment.dto;

import com.example.ecommerce.payment.entity.PaymentMethod;
import com.example.ecommerce.payment.entity.PaymentStatus;

import java.time.Instant;

/**
 * 결제 응답 DTO. 계약 types.ts 의 PaymentResponse 와 필드명·타입·순서 일치.
 * paidAt 은 결제 성공 시각이며 실패 시 null 로 내려간다(계약 5절).
 */
public record PaymentResponse(
        Long id,
        Long orderId,
        Integer amount,
        PaymentStatus status,
        PaymentMethod method,
        Instant paidAt
) {
}
