package com.example.ecommerce.order.repository;

import com.example.ecommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * 주문 아이템 데이터 접근.
 * 목록 조회 시 주문마다 개별 질의하지 않도록 findByOrderIdIn 으로 일괄 조회한다(N+1 방지).
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    List<OrderItem> findByOrderIdInOrderByIdAsc(Collection<Long> orderIds);
}
