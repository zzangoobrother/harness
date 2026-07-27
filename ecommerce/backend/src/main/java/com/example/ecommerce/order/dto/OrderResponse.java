package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.entity.OrderStatus;
import com.example.ecommerce.payment.dto.PaymentResponse;

import java.time.Instant;
import java.util.List;

/**
 * 주문 전체 응답 DTO. 계약 types.ts 의 OrderResponse 와 필드명·타입·순서 일치.
 * - payment 는 아직 결제 시도 전이면 null (application.yml 의 default-property-inclusion: always
 *   설정 덕분에 null 필드도 응답 JSON 에 그대로 포함된다).
 * - createdAt 은 Instant 로 두고 Jackson 이 ISO 8601 문자열로 직렬화한다.
 */
public record OrderResponse(
        Long id,
        OrderStatus status,
        Integer totalAmount,
        List<OrderItemResponse> items,
        PaymentResponse payment,
        Instant createdAt
) {
}
