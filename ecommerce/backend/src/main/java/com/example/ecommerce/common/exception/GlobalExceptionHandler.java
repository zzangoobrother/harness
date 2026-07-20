package com.example.ecommerce.common.exception;

import com.example.ecommerce.common.dto.ApiErrorDetail;
import com.example.ecommerce.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 모든 예외를 계약(api-spec.md)의 표준 에러 스키마
 * { code, message, details, timestamp, status, path } 로 변환하는 전역 핸들러.
 *
 * 3단계 구성:
 * 1) Bean Validation 실패(MethodArgumentNotValidException) → 400 VALIDATION_ERROR (+ details)
 * 2) 도메인 비즈니스 예외(BusinessException) → 각 ErrorCode 의 상태/코드
 * 3) 그 외 모든 예외(fallback) → 500 INTERNAL_ERROR
 *
 * 참고: 인증 실패(401)·인가 실패(403)는 Spring Security 필터 체인에서 발생하므로
 * 여기가 아니라 SecurityConfig 의 EntryPoint/AccessDeniedHandler 에서 동일 shape 으로 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 1) Bean Validation 실패 → 400 + 필드별 details */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, "요청 값 검증에 실패했습니다.", details, request);
    }

    /** 요청 본문 JSON 파싱 실패 등 → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_ERROR, "요청 본문을 해석할 수 없습니다.", null, request);
    }

    /** 2) 도메인 비즈니스 예외 → 대응 ErrorCode */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex,
                                                          HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), null, request);
    }

    /** 3) fallback → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex,
                                                           HttpServletRequest request) {
        return build(ErrorCode.INTERNAL_ERROR, "서버 내부 오류가 발생했습니다.", null, request);
    }

    private ApiErrorDetail toDetail(FieldError fieldError) {
        String reason = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "유효하지 않은 값입니다.";
        return new ApiErrorDetail(fieldError.getField(), reason);
    }

    private ResponseEntity<ApiErrorResponse> build(ErrorCode errorCode, String message,
                                                  List<ApiErrorDetail> details,
                                                  HttpServletRequest request) {
        HttpStatus status = errorCode.getStatus();
        ApiErrorResponse body = ApiErrorResponse.of(
                errorCode.getCode(), message, details, status.value(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
