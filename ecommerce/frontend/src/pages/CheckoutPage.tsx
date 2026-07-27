// 체크아웃 페이지 — GET /api/cart로 주문할 내용을 확인시키고, POST /api/orders로 주문을 생성한다.
// 주문이 생성되면 서버가 장바구니를 비우고 재고를 차감하므로(계약 4.1), 성공 즉시
// 주문 상세/결제 화면(/orders/{id}/complete)으로 이동시켜 결제로 이어지게 한다.

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import type { CartResponse } from "../types/contract";
import { getCart } from "../api/cart";
import { createOrder } from "../api/orders";
import { ApiError, toErrorMessage } from "../api/client";
import { formatPrice } from "../utils/format";

/** 체크아웃에서 발생 가능한 계약 에러 코드(409)를 사용자 안내 메시지로 변환한다. */
function toCheckoutErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.code === "CART_EMPTY") {
      return "장바구니가 비어 있어 주문할 수 없습니다. 상품을 담은 뒤 다시 시도해 주세요.";
    }
    if (err.code === "OUT_OF_STOCK") {
      return "재고가 부족한 상품이 있어 주문할 수 없습니다. 장바구니에서 수량을 조정해 주세요.";
    }
  }
  return toErrorMessage(err);
}

export function CheckoutPage() {
  const navigate = useNavigate();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getCart()
      .then((res) => {
        if (!cancelled) setCart(res);
      })
      .catch((err) => {
        if (!cancelled) setError(toErrorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSubmit = async () => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const order = await createOrder();
      // 주문 생성 성공 → 결제 화면으로. 뒤로가기로 체크아웃에 돌아와도 장바구니는 이미 비어 있다.
      navigate(`/orders/${order.id}/complete`, { replace: true });
    } catch (err) {
      setSubmitError(toCheckoutErrorMessage(err));
      setSubmitting(false);
    }
  };

  if (loading) {
    return <p className="muted">불러오는 중...</p>;
  }

  if (error || !cart) {
    return (
      <div className="checkout-page">
        <div className="alert alert-error" role="alert">
          {error ?? "장바구니를 불러오지 못했습니다."}
        </div>
        <Link to="/cart">장바구니로 돌아가기</Link>
      </div>
    );
  }

  if (cart.items.length === 0) {
    return (
      <div className="checkout-page cart-empty">
        <h1>주문서</h1>
        <p className="muted">장바구니가 비어 있어 주문할 수 없습니다.</p>
        <Link to="/products" className="btn btn-primary">
          상품 보러 가기
        </Link>
      </div>
    );
  }

  return (
    <div className="checkout-page">
      <h1>주문서</h1>
      <p className="muted checkout-notice">
        아래 상품으로 주문이 생성됩니다. 주문 생성 후 결제 화면에서 모의결제를 진행합니다.
      </p>

      <ul className="summary-list">
        {cart.items.map((item) => (
          <li key={item.id} className="summary-item">
            <span className="summary-item-name">{item.productName}</span>
            <span className="summary-item-detail muted">
              {formatPrice(item.price)} × {item.quantity}개
            </span>
            <span className="summary-item-subtotal">{formatPrice(item.subtotal)}</span>
          </li>
        ))}
      </ul>

      <div className="cart-summary">
        <span className="cart-summary-label">총 결제금액</span>
        <span className="cart-summary-amount">{formatPrice(cart.totalAmount)}</span>
      </div>

      {submitError && (
        <div className="alert alert-error checkout-error" role="alert">
          {submitError}
        </div>
      )}

      <div className="checkout-actions">
        <Link to="/cart" className="btn btn-ghost">
          장바구니 수정
        </Link>
        <button type="button" className="btn btn-primary" disabled={submitting} onClick={handleSubmit}>
          {submitting ? "주문 생성 중..." : "주문하기"}
        </button>
      </div>
    </div>
  );
}
