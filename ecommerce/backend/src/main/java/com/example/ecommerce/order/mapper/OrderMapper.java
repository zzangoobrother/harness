package com.example.ecommerce.order.mapper;

import com.example.ecommerce.order.dto.OrderItemResponse;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.payment.dto.PaymentResponse;

import java.util.List;

/**
 * Order/OrderItem 엔티티 → 응답 DTO 변환.
 * 계약(data-model.md §6)상 OrderItem 은 상품명을 보관하지 않으므로,
 * 응답에 필요한 productName 은 호출 측(OrderService)이 조회한 값을 함께 넘긴다
 * (CartMapper 가 Product 를 넘겨받는 것과 동일한 관례).
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderItemResponse toItemResponse(OrderItem item, String productName) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                productName,
                item.getQuantity(),
                item.getPriceAtOrder(),
                item.subtotal()
        );
    }

    /** payment 는 결제 이력이 없으면 null 을 그대로 넘긴다(계약: 결제 전 주문의 payment 는 null). */
    public static OrderResponse toOrderResponse(Order order, List<OrderItemResponse> items,
                                                PaymentResponse payment) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                items,
                payment,
                order.getCreatedAt()
        );
    }
}
