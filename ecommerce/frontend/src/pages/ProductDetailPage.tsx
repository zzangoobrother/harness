// 상품 상세 페이지 — GET /api/products/{id}. 인증 불필요.
// "장바구니 담기" 버튼은 POST /api/cart/items로 연동된다. 비로그인 사용자가 누르면 로그인으로 유도한다.

import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import type { Product } from "../types/contract";
import { getProduct } from "../api/products";
import { addCartItem } from "../api/cart";
import { ApiError, toErrorMessage } from "../api/client";
import { useAuth } from "../store/AuthContext";

function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

/** 장바구니 담기 실패 시 계약 에러 코드를 사용자 메시지로 변환 */
function toAddToCartErrorMessage(err: unknown): string {
  if (err instanceof ApiError && err.code === "OUT_OF_STOCK") {
    return "재고가 부족하여 담을 수 없습니다.";
  }
  return toErrorMessage(err);
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);
  const invalidId = id === undefined || id === "" || Number.isNaN(productId);
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(!invalidId);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [addResult, setAddResult] = useState<{ type: "success" | "error"; message: string } | null>(
    null,
  );

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

  // 상품이 바뀔 때마다 수량 선택과 담기 결과 피드백을 초기화한다.
  useEffect(() => {
    setQuantity(1);
    setAddResult(null);
  }, [productId]);

  const handleAddToCart = async () => {
    if (!product) return;
    if (!isAuthenticated) {
      // 비로그인 사용자는 로그인으로 유도한다. 로그인 후 이 상품 페이지로 돌아올 수 있게 from을 담는다.
      navigate("/login", { state: { from: { pathname: `/products/${productId}` } } });
      return;
    }
    setAdding(true);
    setAddResult(null);
    try {
      await addCartItem({ productId: product.id, quantity });
      setAddResult({ type: "success", message: "장바구니에 담았습니다." });
    } catch (err) {
      setAddResult({ type: "error", message: toAddToCartErrorMessage(err) });
    } finally {
      setAdding(false);
    }
  };

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

          {product.stock > 0 && (
            <div className="product-detail-quantity">
              <button
                type="button"
                className="btn btn-ghost btn-icon"
                disabled={quantity <= 1}
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                aria-label="수량 감소"
              >
                −
              </button>
              <span className="product-detail-quantity-value">{quantity}</span>
              <button
                type="button"
                className="btn btn-ghost btn-icon"
                disabled={quantity >= product.stock}
                onClick={() => setQuantity((q) => Math.min(product.stock, q + 1))}
                aria-label="수량 증가"
              >
                +
              </button>
            </div>
          )}

          <button
            type="button"
            className="btn btn-primary"
            disabled={product.stock === 0 || adding}
            onClick={handleAddToCart}
          >
            {product.stock === 0 ? "품절" : adding ? "담는 중..." : "장바구니 담기"}
          </button>

          {addResult && (
            <p
              className={addResult.type === "success" ? "field-success" : "field-error"}
              role="status"
            >
              {addResult.message}
            </p>
          )}
          {!isAuthenticated && product.stock > 0 && (
            <p className="muted">로그인 후 장바구니에 담을 수 있습니다.</p>
          )}
        </div>
      </div>
    </div>
  );
}
