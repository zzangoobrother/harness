package com.example.ecommerce.payment.repository;

import com.example.ecommerce.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 결제 데이터 접근. orderId 는 유니크하므로 단건 조회는 Optional 을 반환한다.
 * 주문 목록 응답 조립 시에는 findByOrderIdIn 으로 일괄 조회한다(N+1 방지).
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByOrderIdIn(Collection<Long> orderIds);
}
