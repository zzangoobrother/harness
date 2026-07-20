package com.example.ecommerce.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * 표준 에러 응답. 계약 types.ts 의 ApiErrorResponse 와 필드명·타입·순서까지 일치시킨다.
 * 모든 4xx/5xx 응답은 이 shape 을 반환하며, GlobalExceptionHandler 와
 * 보안 필터(EntryPoint/AccessDeniedHandler)가 생성한다.
 *
 * @param code      기계 판독용 에러 코드 (예: "VALIDATION_ERROR")
 * @param message   사용자 노출용 한국어 메시지
 * @param details   필드 단위 상세 오류. 없으면 null
 * @param timestamp 발생 시각 (ISO 8601)
 * @param status    HTTP 상태 코드
 * @param path      요청 경로
 */
public record ApiErrorResponse(
        String code,
        String message,
        List<ApiErrorDetail> details,
        Instant timestamp,
        int status,
        String path
) {
    public static ApiErrorResponse of(String code, String message, List<ApiErrorDetail> details,
                                      int status, String path) {
        return new ApiErrorResponse(code, message, details, Instant.now(), status, path);
    }
}
