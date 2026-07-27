// 주문/모의결제 API 호출 함수. 경로/응답 타입은 계약(api-spec.md 4·5장, types.ts)과 100% 일치한다.
// 4개 엔드포인트 모두 인증이 필요하며, client.ts의 요청 인터셉터가 Bearer 토큰을 자동 첨부한다.
//
// 주의(계약 4.2): GET /api/orders는 배열(OrderResponse[])을 그대로 반환한다.
//   상품 목록과 달리 PageResponse 래퍼가 아니므로 `.items`를 참조하면 안 된다.
// 주의(계약 5.1): 모의결제 실패는 에러가 아니라 HTTP 200 + 본문 status="FAILED"다.
//   따라서 payOrder는 실패 시뮬레이션에서도 정상 resolve 되며, 호출자가 반환값의
//   `status` 필드로 성공/실패를 분기해야 한다. throw 되는 것은 401/403/404/409뿐이다.

import { client } from "./client";
import type {
  CheckoutRequest,
  OrderResponse,
  PaymentRequest,
  PaymentResponse,
} from "../types/contract";

/**
 * POST /api/orders — 체크아웃(장바구니 → 주문 전환).
 * 계약상 요청 본문은 빈 객체이며, 서버가 현재 사용자의 장바구니 전체를 주문으로 전환한다.
 * 에러: 401 UNAUTHORIZED, 409 CART_EMPTY, 409 OUT_OF_STOCK.
 */
export async function createOrder(): Promise<OrderResponse> {
  const body: CheckoutRequest = {};
  const res = await client.post<OrderResponse>("/api/orders", body);
  return res.data;
}

/** GET /api/orders — 내 주문 목록. 응답은 배열이다(PageResponse 아님). */
export async function listOrders(): Promise<OrderResponse[]> {
  const res = await client.get<OrderResponse[]>("/api/orders");
  return res.data;
}

/** GET /api/orders/{id} — 주문 상세. 에러: 401, 403 FORBIDDEN(타인 주문), 404 NOT_FOUND. */
export async function getOrder(id: number): Promise<OrderResponse> {
  const res = await client.get<OrderResponse>(`/api/orders/${id}`);
  return res.data;
}

/**
 * POST /api/orders/{id}/payment — 모의결제 실행.
 * `simulateSuccess`를 생략하면 서버가 true(성공)로 간주한다.
 * 실패 시뮬레이션도 HTTP 200으로 응답하므로 반환된 PaymentResponse.status로 결과를 판단한다.
 * 에러: 401, 403 FORBIDDEN, 404 NOT_FOUND, 409 ALREADY_PAID(이미 결제 성공한 주문 재결제).
 */
export async function payOrder(id: number, simulateSuccess?: boolean): Promise<PaymentResponse> {
  const body: PaymentRequest = { simulateSuccess };
  const res = await client.post<PaymentResponse>(`/api/orders/${id}/payment`, body);
  return res.data;
}
