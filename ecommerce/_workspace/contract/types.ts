// ============================================================================
// 이커머스 MVP 계약 타입 정의 (single source of truth)
// ----------------------------------------------------------------------------
// 이 파일은 프론트엔드(React+TS)가 그대로 import 하여 사용하는 계약 타입이다.
// 백엔드(Spring Boot) 응답 JSON은 반드시 아래 인터페이스 shape과 100% 일치해야 한다.
//
// 규약:
//  - 모든 필드명은 camelCase.
//  - 날짜/시간은 ISO 8601 문자열(예: "2026-07-20T10:00:00Z"). TS 타입은 string.
//  - 금액(price, amount, totalAmount, subtotal, priceAtOrder)은 정수(원, KRW). 소수/문자열 금지.
//  - 엔티티 식별자 id는 number(백엔드 bigint). TS에서는 number로 취급한다.
//  - 요청 DTO와 응답 DTO는 별도 타입으로 분리한다. 엔티티를 그대로 직렬화하지 않는다.
//  - 응답에는 passwordHash 등 민감 필드를 절대 포함하지 않는다.
// ============================================================================

// ----------------------------------------------------------------------------
// Enum (문자열 리터럴 유니온) — 세 계약 파일에서 철자·값이 동일해야 한다.
// ----------------------------------------------------------------------------

/** 사용자 권한 */
export type UserRole = "USER" | "ADMIN";

/** 주문 상태: PENDING(결제대기) → PAID(결제완료) | FAILED(결제실패) | CANCELLED(취소) */
export type OrderStatus = "PENDING" | "PAID" | "FAILED" | "CANCELLED";

/** 결제 상태: PENDING(대기) → SUCCESS(성공) | FAILED(실패) */
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED";

/** 결제 수단: MVP에서는 모의결제(MOCK)만 존재 */
export type PaymentMethod = "MOCK";

// ----------------------------------------------------------------------------
// 공통 에러 응답 (모든 실패 응답이 반환하는 단일 스키마)
// ----------------------------------------------------------------------------

/**
 * 표준 에러 응답.
 * 핵심 3필드(code, message, details)는 애플리케이션 레벨 계약이며,
 * 부가 메타데이터(timestamp, status, path)는 디버깅/로깅용으로 함께 제공한다.
 * 백엔드 GlobalExceptionHandler가 생성하고, 프론트 axios 인터셉터가 소비한다.
 */
export interface ApiErrorResponse {
  /** 기계 판독용 에러 코드 (예: "VALIDATION_ERROR", "EMAIL_DUPLICATED") */
  code: string;
  /** 사용자에게 노출 가능한 한국어 메시지 */
  message: string;
  /** 필드 단위 상세 오류. 없으면 null */
  details: ApiErrorDetail[] | null;
  /** 발생 시각 (ISO 8601) */
  timestamp: string;
  /** HTTP 상태 코드 */
  status: number;
  /** 요청 경로 */
  path: string;
}

/** 필드 단위 검증 오류 항목 */
export interface ApiErrorDetail {
  /** 오류가 발생한 필드명 (예: "email") */
  field: string;
  /** 해당 필드의 오류 사유 */
  reason: string;
}

// ----------------------------------------------------------------------------
// 공통 페이지네이션 응답 래퍼
// ----------------------------------------------------------------------------

/** 페이지네이션이 적용된 목록 응답 (예: 상품 목록) */
export interface PageResponse<T> {
  items: T[];
  /** 0-base 현재 페이지 번호 */
  page: number;
  /** 페이지 크기 */
  size: number;
  /** 전체 요소 수 */
  totalElements: number;
  /** 전체 페이지 수 */
  totalPages: number;
}

// ----------------------------------------------------------------------------
// 도메인 응답 엔티티 DTO
// ----------------------------------------------------------------------------

/** 사용자 응답 (passwordHash 미포함) */
export interface UserResponse {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  createdAt: string; // ISO 8601
}

/** 상품 응답 */
export interface Product {
  id: number;
  name: string;
  description: string | null;
  /** 판매가 (정수, 원) */
  price: number;
  imageUrl: string | null;
  /** 재고 수량 (>= 0) */
  stock: number;
  category: string | null;
  createdAt: string; // ISO 8601
}

/** 장바구니 아이템 응답 (상품 정보 일부를 비정규화하여 포함) */
export interface CartItemResponse {
  /** CartItem id */
  id: number;
  productId: number;
  productName: string;
  productImageUrl: string | null;
  /** 현재 상품 단가 (정수, 원) */
  price: number;
  quantity: number;
  /** price * quantity (정수, 원) */
  subtotal: number;
}

/** 장바구니 전체 응답 */
export interface CartResponse {
  /** Cart id */
  id: number;
  items: CartItemResponse[];
  /** 모든 아이템 subtotal 합계 (정수, 원) */
  totalAmount: number;
}

/** 주문 아이템 응답 (주문 시점 가격 스냅샷 포함) */
export interface OrderItemResponse {
  /** OrderItem id */
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  /** 주문 시점의 단가 스냅샷 (정수, 원) */
  priceAtOrder: number;
  /** priceAtOrder * quantity (정수, 원) */
  subtotal: number;
}

/** 결제 응답 */
export interface PaymentResponse {
  /** Payment id */
  id: number;
  orderId: number;
  /** 결제 금액 (정수, 원) */
  amount: number;
  status: PaymentStatus;
  method: PaymentMethod;
  /** 결제 성공 시각 (ISO 8601). 성공 전이면 null */
  paidAt: string | null;
}

/** 주문 전체 응답 */
export interface OrderResponse {
  id: number;
  status: OrderStatus;
  /** 주문 총액 (정수, 원) */
  totalAmount: number;
  items: OrderItemResponse[];
  /** 결제 정보. 아직 결제 시도 전이면 null */
  payment: PaymentResponse | null;
  createdAt: string; // ISO 8601
}

// ----------------------------------------------------------------------------
// 요청 DTO
// ----------------------------------------------------------------------------

/** 회원가입 요청 */
export interface SignupRequest {
  email: string;
  /** 평문 비밀번호 (백엔드에서 해시 저장) */
  password: string;
  name: string;
}

/** 로그인 요청 */
export interface LoginRequest {
  email: string;
  password: string;
}

/** 인증 응답 (회원가입/로그인 공통): JWT + 사용자 정보 */
export interface AuthResponse {
  /** JWT 액세스 토큰. Authorization: Bearer <token> 로 사용 */
  token: string;
  user: UserResponse;
}

/** 장바구니 아이템 추가 요청 */
export interface AddCartItemRequest {
  productId: number;
  /** 추가 수량 (>= 1) */
  quantity: number;
}

/** 장바구니 아이템 수량 변경 요청 */
export interface UpdateCartItemRequest {
  /** 변경 후 수량 (>= 1) */
  quantity: number;
}

/**
 * 체크아웃(주문 생성) 요청.
 * MVP에서는 서버에 저장된 현재 장바구니 전체를 주문으로 전환하므로
 * 본문 필드가 필요 없다(빈 객체 허용). 배송지 등 확장 필드는 이후 추가한다.
 */
export interface CheckoutRequest {
  // 현재 없음. 확장 대비 빈 객체.
}

/** 모의결제 실행 요청 */
export interface PaymentRequest {
  /**
   * 모의결제 성공/실패 시뮬레이션 플래그. 생략 시 true(성공)로 간주.
   * true  → PaymentStatus=SUCCESS, OrderStatus=PAID
   * false → PaymentStatus=FAILED,  OrderStatus=FAILED
   */
  simulateSuccess?: boolean;
}
