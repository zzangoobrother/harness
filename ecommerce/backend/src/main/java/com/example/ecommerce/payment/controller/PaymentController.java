package com.example.ecommerce.payment.controller;

import com.example.ecommerce.config.security.AuthPrincipal;
import com.example.ecommerce.payment.dto.PaymentRequest;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모의결제 API. 계약 api-spec.md 5절, 인증 필요:
 * - POST /api/orders/{id}/payment → 200, PaymentResponse
 *
 * 경로는 주문 하위지만 결제는 별도 도메인이므로 payment 패키지에 둔다(기능별 패키지 구조).
 * **결제 실패도 200 이다** — 실패 여부는 본문 status(SUCCESS/FAILED)로 구분한다.
 */
@RestController
@RequestMapping("/api/orders")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** simulateSuccess 는 선택 필드이며 본문 자체를 생략해도 성공(true)으로 간주한다. */
    @PostMapping("/{id}/payment")
    public ResponseEntity<PaymentResponse> pay(@AuthenticationPrincipal AuthPrincipal principal,
                                               @PathVariable Long id,
                                               @Valid @RequestBody(required = false) PaymentRequest request) {
        return ResponseEntity.ok(paymentService.pay(principal.userId(), id, request));
    }
}
