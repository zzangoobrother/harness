package com.example.ecommerce.config.security;

/**
 * 인증된 사용자 식별 정보. SecurityContext 의 principal 로 저장되어
 * 이후 도메인 컨트롤러가 @AuthenticationPrincipal 로 userId 를 얻는 데 사용한다.
 */
public record AuthPrincipal(Long userId, String email, String role) {
}
