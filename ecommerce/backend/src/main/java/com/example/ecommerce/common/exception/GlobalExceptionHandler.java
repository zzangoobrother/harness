package com.example.ecommerce.common.exception;

import com.example.ecommerce.common.dto.ApiErrorDetail;
import com.example.ecommerce.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 모든 예외를 계약(api-spec.md)의 표준 에러 스키마
 * { code, message, details, timestamp, status, path } 로 변환하는 전역 핸들러.
 *
 * 4단계 구성:
 * 1) Bean Validation 실패(MethodArgumentNotValidException) → 400 VALIDATION_ERROR (+ details)
 * 2) 요청 파라미터 타입 불일치/필수값 누락(MethodArgumentTypeMismatchException,
 *    MissingServletRequestParameterException) → 400 VALIDATION_ERROR (+ details).
 *    도메인(상품/장바구니/주문 등) 구분 없이 모든 @RequestParam·@PathVariable 바인딩에 공통 적용된다.
 * 3) 도메인 비즈니스 예외(BusinessException) → 각 ErrorCode 의 상태/코드
 * 4) 그 외 모든 예외(fallback) → 500 INTERNAL_ERROR
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

    /**
     * @RequestParam/@PathVariable 값의 타입 변환 실패(예: page=abc, /api/products/abc) → 400.
     * 컨트롤러가 어디든(상품/장바구니/주문 공통) 적용되는 전역 처리이므로 개별 컨트롤러에
     * 방어 코드를 두지 않는다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                              HttpServletRequest request) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "값";
        String reason = "'" + ex.getValue() + "'은(는) 올바른 " + requiredType + " 형식이 아닙니다.";
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail(ex.getName(), reason));
        return build(ErrorCode.VALIDATION_ERROR, "요청 파라미터 타입이 올바르지 않습니다.", details, request);
    }

    /** 필수 @RequestParam 누락 → 400 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                  HttpServletRequest request) {
        List<ApiErrorDetail> details = List.of(
                new ApiErrorDetail(ex.getParameterName(), "필수 파라미터가 누락되었습니다."));
        return build(ErrorCode.VALIDATION_ERROR, "요청 파라미터가 누락되었습니다.", details, request);
    }

    /** 3) 도메인 비즈니스 예외 → 대응 ErrorCode */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex,
                                                          HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), null, request);
    }

    /** 4) fallback → 500 */
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
