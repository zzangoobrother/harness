// 주문 상세 + 모의결제 페이지 — GET /api/orders/{id}, POST /api/orders/{id}/payment.
//
// ⚠️ 가장 틀리기 쉬운 지점 (계약 5.1 / 가정 E):
//   모의결제 "실패"는 예외가 아니다. simulateSuccess=false로 요청해도 서버는 HTTP 200을 반환하고
//   본문의 status="FAILED", paidAt=null로 결과를 알린다. 즉 try/catch로는 실패를 감지할 수 없고,
//   반드시 응답 객체의 status 필드로 분기해야 한다.
//   catch로 잡아야 하는 진짜 에러는 401(UNAUTHORIZED) / 403(FORBIDDEN, 타인 주문) /
//   404(NOT_FOUND) / 409(ALREADY_PAID, 이미 결제 성공한 주문 재결제)뿐이다.

import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { OrderResponse, PaymentResponse } from "../types/contract";
import { getOrder, payOrder } from "../api/orders";
import { ApiError, toErrorMessage } from "../api/client";
import { OrderStatusBadge, PaymentStatusBadge } from "../components/OrderStatusBadge";
import { formatDateTime, formatPrice } from "../utils/format";

/** 결제 요청이 "진짜 에러"로 실패했을 때(=예외) 계약 에러 코드를 사용자 메시지로 변환한다. */
function toPaymentErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.code === "ALREADY_PAID") return "이미 결제가 완료된 주문입니다.";
    if (err.code === "FORBIDDEN") return "다른 사용자의 주문은 결제할 수 없습니다.";
    if (err.code === "NOT_FOUND") return "주문을 찾을 수 없습니다.";
  }
  return toErrorMessage(err);
}

export function OrderCompletePage() {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const invalidId = id === undefined || id === "" || Number.isNaN(orderId);

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(!invalidId);
  const [notFound, setNotFound] = useState(false);
  const [forbidden, setForbidden] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 모의결제 시뮬레이션 선택값(계약 PaymentRequest.simulateSuccess).
  const [simulateSuccess, setSimulateSuccess] = useState(true);
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<string | null>(null);
  const [paymentResult, setPaymentResult] = useState<PaymentResponse | null>(null);

  useEffect(() => {
    if (invalidId) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotFound(false);
    setForbidden(false);
    getOrder(orderId)
      .then((res) => {
        if (!cancelled) setOrder(res);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.code === "NOT_FOUND") {
          setNotFound(true);
        } else if (err instanceof ApiError && err.code === "FORBIDDEN") {
          setForbidden(true);
        } else {
          setError(toErrorMessage(err));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [orderId, invalidId]);

  const handlePay = async () => {
    setPaying(true);
    setPayError(null);
    setPaymentResult(null);
    try {
      const payment = await payOrder(orderId, simulateSuccess);

      // ★ 성공/실패 분기는 예외가 아니라 응답 본문의 status로 한다(계약 5.1: 실패도 HTTP 200).
      setPaymentResult(payment);

      // 결제 후 주문 상태(PENDING → PAID | FAILED)는 서버가 확정하므로 상세를 다시 읽어 반영한다.
      // 재조회가 실패하더라도 위에서 저장한 결제 결과는 그대로 사용자에게 보여준다.
      try {
        const refreshed = await getOrder(orderId);
        setOrder(refreshed);
      } catch {
        // 주문 재조회 실패는 결제 결과 표시를 막지 않는다.
      }
    } catch (err) {
      // 여기 들어오는 것은 401/403/404/409 같은 진짜 에러뿐이다.
      setPayError(toPaymentErrorMessage(err));
    } finally {
      setPaying(false);
    }
  };

  if (invalidId) {
    return (
      <div className="order-detail-page">
        <div className="alert alert-error" role="alert">
          잘못된 주문 ID입니다.
        </div>
        <Link to="/orders">주문 내역으로 돌아가기</Link>
      </div>
    );
  }

  if (loading) {
    return <p className="muted">불러오는 중...</p>;
  }

  if (notFound || forbidden) {
    return (
      <div className="order-detail-page">
        <div className="alert alert-error" role="alert">
          {notFound ? "주문을 찾을 수 없습니다." : "이 주문을 조회할 권한이 없습니다."}
        </div>
        <Link to="/orders">주문 내역으로 돌아가기</Link>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="order-detail-page">
        <div className="alert alert-error" role="alert">
          {error ?? "주문 정보를 불러오지 못했습니다."}
        </div>
        <Link to="/orders">주문 내역으로 돌아가기</Link>
      </div>
    );
  }

  // 결제 가능 상태: 아직 결제 전(PENDING)이거나 결제에 실패(FAILED)해 재시도가 필요한 주문.
  // PAID는 재결제 시 서버가 409 ALREADY_PAID를 반환하므로 UI에서 먼저 막는다.
  const payable = order.status === "PENDING" || order.status === "FAILED";
  // 결제 응답의 status는 SUCCESS/FAILED/PENDING이며, SUCCESS만 성공으로 간주한다.
  const paymentSucceeded = paymentResult?.status === "SUCCESS";

  return (
    <div className="order-detail-page">
      <Link to="/orders" className="product-detail-back">
        ← 주문 내역으로
      </Link>

      <div className="order-detail-head">
        <h1>주문번호 {order.id}</h1>
        <OrderStatusBadge status={order.status} />
      </div>
      <p className="muted">{formatDateTime(order.createdAt)} 주문</p>

      <section className="order-section">
        <h2 className="order-section-title">주문 상품</h2>
        <ul className="summary-list">
          {order.items.map((item) => (
            <li key={item.id} className="summary-item">
              <span className="summary-item-name">{item.productName}</span>
              <span className="summary-item-detail muted">
                주문가 {formatPrice(item.priceAtOrder)} × {item.quantity}개
              </span>
              <span className="summary-item-subtotal">{formatPrice(item.subtotal)}</span>
            </li>
          ))}
        </ul>
        <div className="cart-summary">
          <span className="cart-summary-label">주문 총액</span>
          <span className="cart-summary-amount">{formatPrice(order.totalAmount)}</span>
        </div>
      </section>

      <section className="order-section">
        <h2 className="order-section-title">결제</h2>

        {order.payment && (
          <dl className="payment-info">
            <div className="payment-info-row">
              <dt>결제 상태</dt>
              <dd>
                <PaymentStatusBadge status={order.payment.status} />
              </dd>
            </div>
            <div className="payment-info-row">
              <dt>결제 금액</dt>
              <dd>{formatPrice(order.payment.amount)}</dd>
            </div>
            <div className="payment-info-row">
              <dt>결제 수단</dt>
              <dd>{order.payment.method === "MOCK" ? "모의결제" : order.payment.method}</dd>
            </div>
            <div className="payment-info-row">
              <dt>결제 시각</dt>
              <dd>{order.payment.paidAt ? formatDateTime(order.payment.paidAt) : "-"}</dd>
            </div>
          </dl>
        )}

        {/* 결제 실행 직후 피드백. 실패도 정상 응답이므로 에러 알림이 아니라 결과 표시로 다룬다. */}
        {paymentResult && (
          <p
            className={paymentSucceeded ? "field-success" : "field-error"}
            role="status"
          >
            {paymentSucceeded
              ? "모의결제가 완료되었습니다."
              : "모의결제가 실패했습니다. 아래에서 다시 시도할 수 있습니다."}
          </p>
        )}

        {payError && (
          <div className="alert alert-error" role="alert">
            {payError}
          </div>
        )}

        {payable ? (
          <div className="payment-actions">
            <p className="muted">
              모의결제입니다. 성공/실패를 선택해 결제 결과를 시뮬레이션할 수 있습니다.
            </p>
            <div className="payment-simulate-options">
              <label className="payment-simulate-option">
                <input
                  type="radio"
                  name="simulateSuccess"
                  checked={simulateSuccess}
                  disabled={paying}
                  onChange={() => setSimulateSuccess(true)}
                />
                결제 성공 시뮬레이션
              </label>
              <label className="payment-simulate-option">
                <input
                  type="radio"
                  name="simulateSuccess"
                  checked={!simulateSuccess}
                  disabled={paying}
                  onChange={() => setSimulateSuccess(false)}
                />
                결제 실패 시뮬레이션
              </label>
            </div>
            <button type="button" className="btn btn-primary" disabled={paying} onClick={handlePay}>
              {paying ? "결제 처리 중..." : "결제하기"}
            </button>
          </div>
        ) : (
          <p className="muted">
            {order.status === "PAID"
              ? "결제가 완료된 주문입니다."
              : "결제를 진행할 수 없는 주문입니다."}
          </p>
        )}
      </section>
    </div>
  );
}
