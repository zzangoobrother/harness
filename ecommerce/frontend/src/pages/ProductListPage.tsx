// 상품 목록 페이지 — 카드 그리드 + 카테고리 필터 + 페이지네이션.
// 인증 불필요(계약상 GET /api/products는 permitAll). GET /api/products (PageResponse<Product>)를 호출한다.
// 페이지/카테고리 상태는 URL 쿼리스트링(searchParams)에 반영하여 새로고침·뒤로가기에도 유지되게 한다.

import { useEffect, useState, type ChangeEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import type { PageResponse, Product } from "../types/contract";
import { listProducts } from "../api/products";
import { toErrorMessage } from "../api/client";

const PAGE_SIZE = 12;

// 카테고리 옵션을 모으기 위해 한 번 넉넉히 조회할 크기.
// 계약(api-spec.md)에는 카테고리 목록 전용 API가 없다. 상품 수가 적은 MVP 특성상,
// 넉넉한 size로 한 번 조회한 응답에서 등장하는 category 값들을 모아 필터 옵션으로 쓴다.
// 상품 수가 이 값을 넘으면 일부 카테고리가 필터에서 누락될 수 있으므로(MVP 범위의 한계),
// 데이터가 늘어나면 api-architect에게 전용 카테고리 API 추가를 요청해야 한다.
const CATEGORY_SCAN_SIZE = 100;

function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") ?? "0");
  const category = searchParams.get("category") ?? "";

  const [data, setData] = useState<PageResponse<Product> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [categories, setCategories] = useState<string[]>([]);

  // 카테고리 옵션은 최초 1회만 수집한다 (필터/페이지 변경과 무관).
  useEffect(() => {
    let cancelled = false;
    listProducts({ page: 0, size: CATEGORY_SCAN_SIZE })
      .then((res) => {
        if (cancelled) return;
        const set = new Set<string>();
        for (const p of res.items) {
          if (p.category) set.add(p.category);
        }
        setCategories(Array.from(set).sort());
      })
      .catch(() => {
        // 카테고리 수집 실패는 필터 옵션이 비어 보이는 정도로 그치고
        // 목록 조회 자체(아래 effect)는 별도로 계속 진행한다.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    listProducts({ page, size: PAGE_SIZE, category: category || undefined })
      .then((res) => {
        if (!cancelled) setData(res);
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
  }, [page, category]);

  const handleCategoryChange = (e: ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    const next = new URLSearchParams(searchParams);
    next.set("page", "0");
    if (value) {
      next.set("category", value);
    } else {
      next.delete("category");
    }
    setSearchParams(next);
  };

  const goToPage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams);
    next.set("page", String(nextPage));
    setSearchParams(next);
  };

  return (
    <div className="product-list">
      <div className="product-list-header">
        <h1>상품 목록</h1>
        <select
          className="category-select"
          value={category}
          onChange={handleCategoryChange}
          aria-label="카테고리 필터"
        >
          <option value="">전체 카테고리</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {loading && <p className="muted">불러오는 중...</p>}

      {!loading && error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && data && data.items.length === 0 && (
        <p className="muted">해당 조건의 상품이 없습니다.</p>
      )}

      {!loading && !error && data && data.items.length > 0 && (
        <>
          <div className="product-grid">
            {data.items.map((product) => (
              <Link key={product.id} to={`/products/${product.id}`} className="product-card">
                <div className="product-card-image-wrap">
                  {product.imageUrl ? (
                    <img src={product.imageUrl} alt={product.name} className="product-card-image" />
                  ) : (
                    <div className="product-card-image-placeholder">이미지 없음</div>
                  )}
                  {product.stock === 0 && <span className="badge badge-soldout">품절</span>}
                </div>
                <div className="product-card-body">
                  <p className="product-card-name">{product.name}</p>
                  <p className="product-card-price">{formatPrice(product.price)}</p>
                </div>
              </Link>
            ))}
          </div>

          <div className="pagination">
            <button
              type="button"
              className="btn btn-ghost"
              disabled={page <= 0}
              onClick={() => goToPage(page - 1)}
            >
              이전
            </button>
            <span className="pagination-status">
              {data.page + 1} / {Math.max(data.totalPages, 1)}
            </span>
            <button
              type="button"
              className="btn btn-ghost"
              disabled={page >= data.totalPages - 1}
              onClick={() => goToPage(page + 1)}
            >
              다음
            </button>
          </div>
        </>
      )}
    </div>
  );
}
