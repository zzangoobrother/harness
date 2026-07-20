package com.example.ecommerce.common.dto;

/**
 * 필드 단위 검증 오류 항목. 계약 types.ts 의 ApiErrorDetail 과 shape 일치.
 *
 * @param field  오류가 발생한 필드명 (예: "email")
 * @param reason 해당 필드의 오류 사유
 */
public record ApiErrorDetail(String field, String reason) {
}
