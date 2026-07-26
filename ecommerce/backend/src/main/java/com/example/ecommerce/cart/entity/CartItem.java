package com.example.ecommerce.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 장바구니 아이템 엔티티. 계약 data-model.md 의 CartItem 정의를 반영한다.
 * - (cartId, productId) 복합 유니크 — 같은 장바구니에 동일 상품은 한 행으로만 존재.
 *   동시 요청으로 중복 삽입이 시도되면 DB 제약이 DataIntegrityViolationException 을 던지며,
 *   CartService 가 이를 잡아 기존 행에 수량을 병합한다(동시성 경합 방어).
 * - quantity는 항상 1 이상. cartId/productId는 엔티티 연관관계가 아닌 단순 FK 컬럼으로 둔다
 *   (Product 조회는 서비스 계층에서 ProductRepository 로 직접 수행).
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_item_cart_product", columnNames = {"cart_id", "product_id"}))
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    protected CartItem() {
        // JPA 기본 생성자
    }

    public CartItem(Long cartId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
    }

    /** 수량을 변경한다. 1 이상만 허용(호출 전 재고 검증은 서비스 계층 책임). */
    public void changeQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getCartId() {
        return cartId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
