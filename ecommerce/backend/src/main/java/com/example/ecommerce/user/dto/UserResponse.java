package com.example.ecommerce.user.dto;

import com.example.ecommerce.user.entity.UserRole;

import java.time.Instant;

/**
 * 사용자 응답 DTO. 계약 types.ts 의 UserResponse 와 필드명·타입·순서 일치.
 * passwordHash 는 절대 포함하지 않는다.
 *
 * createdAt 은 Instant 로 두고 Jackson 이 ISO 8601 문자열(예: 2026-07-20T10:00:00Z)로 직렬화한다
 * (spring.jackson.serialization.write-dates-as-timestamps=false).
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        Instant createdAt
) {
}
