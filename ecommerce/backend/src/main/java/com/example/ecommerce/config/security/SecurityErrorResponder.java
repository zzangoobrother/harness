package com.example.ecommerce.config.security;

import com.example.ecommerce.common.dto.ApiErrorResponse;
import com.example.ecommerce.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 보안 필터 체인(EntryPoint/AccessDeniedHandler)에서 발생하는 인증/인가 실패를
 * GlobalExceptionHandler 와 동일한 표준 에러 shape 으로 직렬화하는 헬퍼.
 * 컨트롤러 진입 전 필터 단계에서 예외가 나므로 @RestControllerAdvice 가 잡지 못하기 때문에 필요하다.
 */
final class SecurityErrorResponder {

    private SecurityErrorResponder() {
    }

    static void write(HttpServletResponse response, HttpServletRequest request,
                      ObjectMapper objectMapper, ErrorCode errorCode, String message) throws IOException {
        int status = errorCode.getStatus().value();
        ApiErrorResponse body = ApiErrorResponse.of(
                errorCode.getCode(), message, null, status, request.getRequestURI());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
