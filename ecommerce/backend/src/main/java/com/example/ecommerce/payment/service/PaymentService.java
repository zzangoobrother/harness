package com.example.ecommerce.payment.service;

import com.example.ecommerce.common.exception.AlreadyPaidException;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.payment.dto.PaymentRequest;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.entity.Payment;
import com.example.ecommerce.payment.entity.PaymentStatus;
import com.example.ecommerce.payment.mapper.PaymentMapper;
import com.example.ecommerce.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 모의결제 비즈니스 로직. 계약 api-spec.md 5절 규칙:
 * - simulateSuccess=true(생략 시 기본) → Payment.status=SUCCESS, paidAt=now, Order.status=PAID.
 * - simulateSuccess=false → Payment.status=FAILED, paidAt=null, Order.status=FAILED.
 * - **결제 실패는 에러가 아니다.** 요청 처리 자체는 성공했으므로 HTTP 200 + 본문 status 로 결과를 구분한다(가정 7).
 * - 이미 SUCCESS 인 주문의 재결제만 409 ALREADY_PAID. FAILED 주문의 재시도는 허용한다.
 * - 타인 주문은 403, 없는 주문은 404 (OrderService.findOwnedOrder 가 판정).
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    public PaymentService(PaymentRepository paymentRepository, OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
    }

    /**
     * POST /api/orders/{id}/payment: 모의결제를 실행한다.
     * Payment 생성/갱신과 Order 상태 전이를 한 트랜잭션에서 처리해 두 값이 어긋나지 않게 한다.
     *
     * <p>재고 복원에 대한 결정: 결제 실패 시 재고를 **복원하지 않는다**.
     * 계약상 FAILED 주문은 재결제가 가능한데(SUCCESS 만 409 ALREADY_PAID),
     * 실패 시점에 재고를 되돌려 놓으면 이후 재결제가 성공했을 때 재고가 차감되지 않은 채 판매가 확정되어
     * 오히려 재고가 부풀려진다. 재고는 주문 생성 시점에 확보한 것으로 보고 유지한다(계약 가정 2가
     * 복원 여부를 backend 구현 세부사항으로 위임함). 주문 취소 API 가 생기면 그 시점에 복원한다.
     */
    @Transactional
    public PaymentResponse pay(Long userId, Long orderId, PaymentRequest request) {
        Order order = orderService.findOwnedOrder(userId, orderId);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new AlreadyPaidException("이미 결제가 완료된 주문입니다. orderId=" + orderId);
        }
        if (payment == null) {
            // Payment 는 orderId 유니크(주문당 1건)이므로 재시도 시 새 행을 만들지 않고 기존 행을 갱신한다.
            payment = paymentRepository.save(new Payment(orderId, order.getTotalAmount()));
        }

        // 본문이 아예 없으면(request == null) 계약상 기본값 true(성공)로 간주한다.
        boolean simulateSuccess = request == null || request.resolveSimulateSuccess();
        if (simulateSuccess) {
            payment.markSuccess(Instant.now());
            order.markPaid();
        } else {
            payment.markFailed();
            order.markFailed();
        }

        return PaymentMapper.toResponse(payment);
    }
}
