package com.example.ecommerce.order.dto;

/**
 * 체크아웃(주문 생성) 요청. 계약 types.ts 의 CheckoutRequest 와 일치한다.
 * MVP 에서는 서버에 저장된 현재 장바구니 전체를 주문으로 전환하므로 본문 필드가 없다(빈 객체 `{}`).
 * 배송지 등 확장 필드는 이후 추가한다.
 */
public record CheckoutRequest() {
}
