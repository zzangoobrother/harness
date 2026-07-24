// 상품 API 호출 함수. 경로/쿼리·응답 타입은 계약(api-spec.md 2장, types.ts)과 100% 일치한다.

import { client } from "./client";
import type { PageResponse, Product } from "../types/contract";

/** GET /api/products 쿼리 파라미터 (api-spec.md 2.1) */
export interface ListProductsParams {
  /** 0-base 페이지 번호. 생략 시 서버 기본값(0) */
  page?: number;
  /** 페이지 크기. 생략 시 서버 기본값(20) */
  size?: number;
  /** 카테고리 필터 (선택) */
  category?: string;
}

/**
 * GET /api/products — 상품 목록(페이지네이션).
 * axios는 params 객체에서 undefined 값을 가진 키를 자동으로 생략하므로
 * 선택 파라미터를 그대로 넘겨도 불필요한 쿼리스트링이 붙지 않는다.
 */
export async function listProducts(
  params?: ListProductsParams,
): Promise<PageResponse<Product>> {
  const res = await client.get<PageResponse<Product>>("/api/products", { params });
  return res.data;
}

/** GET /api/products/{id} — 상품 상세 (없으면 404 NOT_FOUND) */
export async function getProduct(id: number): Promise<Product> {
  const res = await client.get<Product>(`/api/products/${id}`);
  return res.data;
}
