package com.example.ecommerce.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 주문 엔티티. 계약 data-model.md 의 Order 정의를 반영한다.
 * - totalAmount 는 모든 OrderItem subtotal(priceAtOrder * quantity)의 합, 정수 원(KRW).
 * - status 는 생성 시 PENDING, 결제 결과에 따라 PAID/FAILED 로 전이한다.
 * - userId 는 엔티티 연관관계가 아닌 단순 FK 컬럼으로 둔다(cart 도메인과 동일한 관례).
 *   양방향 연관관계를 만들지 않아 직렬화 순환참조·의도치 않은 오버페칭을 원천 차단한다.
 * - "orders" 는 일부 DB 에서 예약어라 테이블명을 명시적으로 인용 부호 없이 쓰되,
 *   H2(PostgreSQL 모드)/PostgreSQL 모두에서 사용 가능한 복수형 그대로 사용한다.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Order() {
        // JPA 기본 생성자
    }

    public Order(Long userId, Integer totalAmount) {
        if (totalAmount == null || totalAmount < 0) {
            throw new IllegalArgumentException("totalAmount는 0 이상이어야 합니다.");
        }
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    /** 결제 성공 시 PAID 로 전이한다. */
    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    /** 결제 실패 시 FAILED 로 전이한다. FAILED 주문은 재결제가 가능하다(계약상 SUCCESS 만 차단). */
    public void markFailed() {
        this.status = OrderStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
