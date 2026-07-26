// 장바구니 API 호출 함수. 경로/응답 타입은 계약(api-spec.md 3장, types.ts)과 100% 일치한다.
// 모든 엔드포인트가 인증 필요이며, 응답은 항상 장바구니 전체(CartResponse)를 반환한다.
// 따라서 각 함수는 응답 그대로를 반환하고, 호출자는 재조회 없이 그 값으로 상태를 교체하면 된다.

import { client } from "./client";
import type { AddCartItemRequest, CartResponse, UpdateCartItemRequest } from "../types/contract";

/** GET /api/cart — 내 장바구니 조회 */
export async function getCart(): Promise<CartResponse> {
  const res = await client.get<CartResponse>("/api/cart");
  return res.data;
}

/** POST /api/cart/items — 아이템 추가 (동일 productId면 서버가 수량을 합산한다) */
export async function addCartItem(body: AddCartItemRequest): Promise<CartResponse> {
  const res = await client.post<CartResponse>("/api/cart/items", body);
  return res.data;
}

/** PATCH /api/cart/items/{id} — 수량 변경 */
export async function updateCartItem(
  id: number,
  body: UpdateCartItemRequest,
): Promise<CartResponse> {
  const res = await client.patch<CartResponse>(`/api/cart/items/${id}`, body);
  return res.data;
}

/** DELETE /api/cart/items/{id} — 아이템 제거 */
export async function removeCartItem(id: number): Promise<CartResponse> {
  const res = await client.delete<CartResponse>(`/api/cart/items/${id}`);
  return res.data;
}
