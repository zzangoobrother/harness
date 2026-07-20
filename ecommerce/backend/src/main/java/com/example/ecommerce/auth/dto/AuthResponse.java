package com.example.ecommerce.auth.dto;

import com.example.ecommerce.user.dto.UserResponse;

/**
 * 인증 응답(회원가입/로그인 공통). 계약 types.ts 의 AuthResponse 와 일치.
 *
 * @param token JWT 액세스 토큰 (Authorization: Bearer &lt;token&gt;)
 * @param user  사용자 정보(passwordHash 미포함)
 */
public record AuthResponse(
        String token,
        UserResponse user
) {
}
