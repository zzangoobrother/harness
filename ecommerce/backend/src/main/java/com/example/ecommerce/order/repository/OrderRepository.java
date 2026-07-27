package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주문 데이터 접근.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 사용자의 주문을 최신순으로 조회한다(계약 4.2: 배열, 최신순).
     * createdAt 이 동일할 수 있으므로 id 내림차순을 2차 정렬 키로 두어 순서를 결정적으로 만든다.
     */
    List<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
