package com.example.ecommerce.order.controller;

import com.example.ecommerce.config.security.AuthPrincipal;
import com.example.ecommerce.order.dto.CheckoutRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주문 API. 계약 api-spec.md 4절, 모두 인증 필요(SecurityConfig 의 anyRequest().authenticated()):
 * - POST /api/orders      → 201, OrderResponse (본문은 빈 객체 {})
 * - GET  /api/orders      → 200, OrderResponse[] (**배열**, PageResponse 아님)
 * - GET  /api/orders/{id} → 200, OrderResponse
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 체크아웃. 요청 본문은 계약상 빈 객체 `{}` 이며 사용하는 필드가 없다.
     * 본문을 아예 보내지 않는 클라이언트도 400 이 나지 않도록 required=false 로 둔다.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal AuthPrincipal principal,
                                                  @RequestBody(required = false) CheckoutRequest request) {
        OrderResponse response = orderService.checkout(principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyOrders(principal.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@AuthenticationPrincipal AuthPrincipal principal,
                                                  @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(principal.userId(), id));
    }
}
