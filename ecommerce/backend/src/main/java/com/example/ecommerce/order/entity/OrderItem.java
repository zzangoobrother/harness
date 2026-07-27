package com.example.ecommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 주문 아이템 엔티티. 계약 data-model.md 의 OrderItem 정의를 반영한다.
 * - priceAtOrder 는 **주문 생성 시점의 Product.price 스냅샷**이다.
 *   이후 상품 가격이 바뀌어도 과거 주문 금액이 왜곡되지 않게 하기 위함이다.
 * - subtotal(= priceAtOrder * quantity)은 DB 에 저장하지 않고 응답 조립 시 계산한다(계약 규정).
 * - orderId/productId 는 단순 FK 컬럼(cart 도메인과 동일한 관례).
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_order", nullable = false)
    private Integer priceAtOrder;

    protected OrderItem() {
        // JPA 기본 생성자
    }

    public OrderItem(Long orderId, Long productId, Integer quantity, Integer priceAtOrder) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다.");
        }
        if (priceAtOrder == null || priceAtOrder < 0) {
            throw new IllegalArgumentException("priceAtOrder는 0 이상이어야 합니다.");
        }
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    /** 응답용 소계. 저장하지 않고 매번 계산한다. */
    public int subtotal() {
        return priceAtOrder * quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getPriceAtOrder() {
        return priceAtOrder;
    }
}
