// 상품 상세 페이지 — GET /api/products/{id}. 인증 불필요.
// "장바구니 담기" 버튼은 Stage 3(장바구니)에서 POST /api/cart/items로 연동될 자리표시자다.

import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import type { Product } from "../types/contract";
import { getProduct } from "../api/products";
import { ApiError, toErrorMessage } from "../api/client";

function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);
  const invalidId = id === undefined || id === "" || Number.isNaN(productId);

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(!invalidId);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (invalidId) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotFound(false);
    getProduct(productId)
      .then((res) => {
        if (!cancelled) setProduct(res);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.code === "NOT_FOUND") {
          setNotFound(true);
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
  }, [productId, invalidId]);

  if (invalidId) {
    return (
      <div className="product-detail">
        <div className="alert alert-error" role="alert">
          잘못된 상품 ID입니다.
        </div>
        <Link to="/products">상품 목록으로 돌아가기</Link>
      </div>
    );
  }

  if (loading) {
    return <p className="muted">불러오는 중...</p>;
  }

  if (notFound) {
    return (
      <div className="product-detail">
        <div className="alert alert-error" role="alert">
          상품을 찾을 수 없습니다.
        </div>
        <Link to="/products">상품 목록으로 돌아가기</Link>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="product-detail">
        <div className="alert alert-error" role="alert">
          {error ?? "상품 정보를 불러오지 못했습니다."}
        </div>
        <Link to="/products">상품 목록으로 돌아가기</Link>
      </div>
    );
  }

  return (
    <div className="product-detail">
      <Link to="/products" className="product-detail-back">
        ← 목록으로
      </Link>
      <div className="product-detail-body">
        <div className="product-detail-image-wrap">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} className="product-detail-image" />
          ) : (
            <div className="product-card-image-placeholder product-detail-image-placeholder">
              이미지 없음
            </div>
          )}
        </div>
        <div className="product-detail-info">
          <p className="product-detail-category muted">{product.category ?? "미분류"}</p>
          <h1 className="product-detail-name">{product.name}</h1>
          <p className="product-detail-price">{formatPrice(product.price)}</p>
          <p className="product-detail-stock">
            {product.stock === 0 ? (
              <span className="badge badge-soldout">품절</span>
            ) : (
              `재고 ${product.stock}개`
            )}
          </p>
          <p className="product-detail-description">
            {product.description ?? "상품 설명이 없습니다."}
          </p>
          {/* TODO(Stage 3: 장바구니): POST /api/cart/items 연동 후 활성화 */}
          <button
            type="button"
            className="btn btn-primary"
            disabled
            title="장바구니 기능은 다음 단계에서 제공됩니다"
          >
            장바구니 담기
          </button>
        </div>
      </div>
    </div>
  );
}
