// 주문/결제 상태 배지. 계약의 OrderStatus·PaymentStatus 리터럴 유니온을 그대로 키로 쓰므로
// 계약에 상태 값이 추가되면 Record 타입 때문에 컴파일 단계에서 누락이 드러난다.

import type { OrderStatus, PaymentStatus } from "../types/contract";

const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제대기",
  PAID: "결제완료",
  FAILED: "결제실패",
  CANCELLED: "취소됨",
};

const ORDER_STATUS_CLASS: Record<OrderStatus, string> = {
  PENDING: "badge-pending",
  PAID: "badge-success",
  FAILED: "badge-failed",
  CANCELLED: "badge-neutral",
};

const PAYMENT_STATUS_LABEL: Record<PaymentStatus, string> = {
  PENDING: "결제대기",
  SUCCESS: "결제성공",
  FAILED: "결제실패",
};

const PAYMENT_STATUS_CLASS: Record<PaymentStatus, string> = {
  PENDING: "badge-pending",
  SUCCESS: "badge-success",
  FAILED: "badge-failed",
};

/** 주문 상태 배지 */
export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`badge ${ORDER_STATUS_CLASS[status]}`}>{ORDER_STATUS_LABEL[status]}</span>;
}

/** 결제 상태 배지 */
export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <span className={`badge ${PAYMENT_STATUS_CLASS[status]}`}>{PAYMENT_STATUS_LABEL[status]}</span>
  );
}
