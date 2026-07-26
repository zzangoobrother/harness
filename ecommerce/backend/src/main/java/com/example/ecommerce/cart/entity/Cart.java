package com.example.ecommerce.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * 장바구니 엔티티. 계약 data-model.md 의 Cart 정의를 반영한다.
 * - userId 는 유니크(User 1:1 Cart), 사용자당 1개만 존재한다.
 * - 최초 장바구니 접근 시 없으면 지연 생성한다(CartService 참고).
 */
@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(name = "uk_cart_user_id", columnNames = "user_id"))
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Cart() {
        // JPA 기본 생성자
    }

    public Cart(Long userId) {
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
