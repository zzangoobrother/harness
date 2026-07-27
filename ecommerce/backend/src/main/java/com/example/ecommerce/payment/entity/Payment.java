package com.example.ecommerce.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * 결제 엔티티. 계약 data-model.md 의 Payment 정의를 반영한다.
 * - orderId 유니크(Order 1:1 Payment). 재결제(FAILED 주문 재시도) 시 새 행을 만들지 않고
 *   기존 행의 status/paidAt 을 갱신한다 — 유니크 제약을 위반하지 않기 위함이다.
 * - amount 는 Order.totalAmount 와 동일한 정수 원(KRW).
 * - paidAt 은 성공 시각. 실패/대기 상태에서는 null 이며 계약상 응답에도 null 로 노출된다.
 */
@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id"))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Payment() {
        // JPA 기본 생성자
    }

    public Payment(Long orderId, Integer amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("amount는 0 이상이어야 합니다.");
        }
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.method = PaymentMethod.MOCK;
    }

    /** 모의결제 성공 처리: status=SUCCESS, paidAt=결제 시각. */
    public void markSuccess(Instant paidAt) {
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = paidAt;
    }

    /** 모의결제 실패 처리: status=FAILED, paidAt=null(성공한 적 없으므로 비운다). */
    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.paidAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Integer getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
