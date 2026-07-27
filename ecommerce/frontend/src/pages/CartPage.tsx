// 장바구니 페이지 — GET /api/cart로 조회하고, 수량 변경/삭제는 응답으로 받은
// CartResponse 전체로 상태를 교체한다(재조회하지 않음). 총액은 서버의 totalAmount를 그대로 쓴다.

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { CartResponse } from "../types/contract";
import { getCart, removeCartItem, updateCartItem } from "../api/cart";
import { ApiError, toErrorMessage } from "../api/client";

function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

/** 계약 에러 코드 중 장바구니에서 자주 발생하는 케이스만 사용자 메시지로 오버라이드한다. */
function toCartErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.code === "OUT_OF_STOCK") return "재고가 부족하여 수량을 변경할 수 없습니다.";
    if (err.code === "NOT_FOUND") return "이미 삭제된 상품이거나 장바구니 아이템입니다.";
  }
  return toErrorMessage(err);
}

export function CartPage() {
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 수량 변경/삭제 중인 아이템(중복 클릭 방지)과 아이템별 에러 메시지.
  const [busyItemId, setBusyItemId] = useState<number | null>(null);
  const [itemError, setItemError] = useState<{ id: number; message: string } | null>(null);

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

  const handleQuantityChange = async (itemId: number, nextQuantity: number) => {
    // 수량은 1 미만으로 내려갈 수 없다(서버도 400 VALIDATION_ERROR로 거부하지만 UI에서 먼저 막는다).
    if (nextQuantity < 1) return;
    setBusyItemId(itemId);
    setItemError(null);
    try {
      const updated = await updateCartItem(itemId, { quantity: nextQuantity });
      setCart(updated);
    } catch (err) {
      setItemError({ id: itemId, message: toCartErrorMessage(err) });
    } finally {
      setBusyItemId(null);
    }
  };

  const handleRemove = async (itemId: number) => {
    setBusyItemId(itemId);
    setItemError(null);
    try {
      const updated = await removeCartItem(itemId);
      setCart(updated);
    } catch (err) {
      setItemError({ id: itemId, message: toCartErrorMessage(err) });
    } finally {
      setBusyItemId(null);
    }
  };

  if (loading) {
    return <p className="muted">불러오는 중...</p>;
  }

  if (error || !cart) {
    return (
      <div className="cart-page">
        <div className="alert alert-error" role="alert">
          {error ?? "장바구니를 불러오지 못했습니다."}
        </div>
      </div>
    );
  }

  if (cart.items.length === 0) {
    return (
      <div className="cart-page cart-empty">
        <h1>장바구니</h1>
        <p className="muted">장바구니가 비어 있습니다.</p>
        <Link to="/products" className="btn btn-primary">
          상품 보러 가기
        </Link>
        {/* 빈 장바구니에서는 주문할 수 없다(서버도 409 CART_EMPTY로 거부). 비활성 버튼으로 표시한다. */}
        <button type="button" className="btn btn-ghost" disabled>
          주문하기
        </button>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <h1>장바구니</h1>
      <ul className="cart-list">
        {cart.items.map((item) => (
          <li key={item.id} className="cart-item">
            <div className="cart-item-image-wrap">
              {item.productImageUrl ? (
                <img
                  src={item.productImageUrl}
                  alt={item.productName}
                  className="cart-item-image"
                />
              ) : (
                <div className="product-card-image-placeholder cart-item-image-placeholder">
                  이미지 없음
                </div>
              )}
            </div>

            <div className="cart-item-body">
              <p className="cart-item-name">{item.productName}</p>
              <p className="cart-item-price muted">{formatPrice(item.price)}</p>
              {itemError?.id === item.id && (
                <p className="field-error" role="alert">
                  {itemError.message}
                </p>
              )}
            </div>

            <div className="cart-item-quantity">
              <button
                type="button"
                className="btn btn-ghost btn-icon"
                disabled={busyItemId === item.id || item.quantity <= 1}
                onClick={() => handleQuantityChange(item.id, item.quantity - 1)}
                aria-label="수량 감소"
              >
                −
              </button>
              <span className="cart-item-quantity-value">{item.quantity}</span>
              <button
                type="button"
                className="btn btn-ghost btn-icon"
                disabled={busyItemId === item.id}
                onClick={() => handleQuantityChange(item.id, item.quantity + 1)}
                aria-label="수량 증가"
              >
                +
              </button>
            </div>

            <p className="cart-item-subtotal">{formatPrice(item.subtotal)}</p>

            <button
              type="button"
              className="btn btn-ghost cart-item-remove"
              disabled={busyItemId === item.id}
              onClick={() => handleRemove(item.id)}
            >
              삭제
            </button>
          </li>
        ))}
      </ul>

      <div className="cart-summary">
        <span className="cart-summary-label">총 결제금액</span>
        <span className="cart-summary-amount">{formatPrice(cart.totalAmount)}</span>
      </div>

      {/* 체크아웃 진입점. 여기까지 왔다면 아이템이 1개 이상이다(빈 장바구니는 위에서 분기). */}
      <div className="cart-actions">
        <Link to="/checkout" className="btn btn-primary">
          주문하기
        </Link>
      </div>
    </div>
  );
}
