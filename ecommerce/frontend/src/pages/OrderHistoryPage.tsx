// 주문 목록 페이지 — GET /api/orders.
// 계약 4.2에 따라 응답은 OrderResponse "배열"이다(PageResponse 래퍼가 아니므로 .items 접근 금지).
// 서버가 최신순 정렬을 "권장"으로만 규정하므로, 표시 순서는 프론트에서 한 번 더 보장한다.

import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import type { OrderResponse } from "../types/contract";
import { listOrders } from "../api/orders";
import { toErrorMessage } from "../api/client";
import { OrderStatusBadge } from "../components/OrderStatusBadge";
import { formatDateTime, formatPrice } from "../utils/format";

/** 최신순(createdAt 내림차순, 동일 시각이면 id 내림차순)으로 정렬한다. */
function sortByLatest(orders: OrderResponse[]): OrderResponse[] {
  return [...orders].sort((a, b) => {
    const diff = new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    return diff !== 0 ? diff : b.id - a.id;
  });
}

/** "무선 이어폰 외 2건" 형태의 항목 요약 문구를 만든다. */
function summarizeItems(order: OrderResponse): string {
  if (order.items.length === 0) return "주문 항목 없음";
  const [first, ...rest] = order.items;
  return rest.length === 0 ? first.productName : `${first.productName} 외 ${rest.length}건`;
}

export function OrderHistoryPage() {
  const [orders, setOrders] = useState<OrderResponse[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    listOrders()
      .then((res) => {
        if (!cancelled) setOrders(sortByLatest(res));
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

  if (loading) {
    return <p className="muted">불러오는 중...</p>;
  }

  if (error || !orders) {
    return (
      <div className="order-list-page">
        <div className="alert alert-error" role="alert">
          {error ?? "주문 목록을 불러오지 못했습니다."}
        </div>
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="order-list-page cart-empty">
        <h1>주문 내역</h1>
        <p className="muted">아직 주문 내역이 없습니다.</p>
        <Link to="/products" className="btn btn-primary">
          상품 보러 가기
        </Link>
      </div>
    );
  }

  return (
    <div className="order-list-page">
      <h1>주문 내역</h1>
      <ul className="order-list">
        {orders.map((order) => (
          <li key={order.id}>
            <Link to={`/orders/${order.id}/complete`} className="order-card">
              <div className="order-card-head">
                <span className="order-card-id">주문번호 {order.id}</span>
                <OrderStatusBadge status={order.status} />
              </div>
              <p className="order-card-summary">{summarizeItems(order)}</p>
              <div className="order-card-foot">
                <span className="muted">{formatDateTime(order.createdAt)}</span>
                <span className="order-card-amount">{formatPrice(order.totalAmount)}</span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
