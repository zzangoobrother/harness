package com.example.ecommerce.order.service;

import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.common.exception.CartEmptyException;
import com.example.ecommerce.common.exception.ForbiddenException;
import com.example.ecommerce.common.exception.OrderNotFoundException;
import com.example.ecommerce.common.exception.OutOfStockException;
import com.example.ecommerce.common.exception.ProductNotFoundException;
import com.example.ecommerce.order.dto.OrderItemResponse;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.order.mapper.OrderMapper;
import com.example.ecommerce.order.repository.OrderItemRepository;
import com.example.ecommerce.order.repository.OrderRepository;
import com.example.ecommerce.payment.dto.PaymentResponse;
import com.example.ecommerce.payment.entity.Payment;
import com.example.ecommerce.payment.mapper.PaymentMapper;
import com.example.ecommerce.payment.repository.PaymentRepository;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 비즈니스 로직. 계약 api-spec.md 4절 규칙:
 * - POST /api/orders: 장바구니 전체를 주문으로 전환. priceAtOrder 에 가격 스냅샷,
 *   재고는 **주문 생성 시점**에 차감(계약 가정 2), status=PENDING, payment=null, 장바구니는 비운다.
 * - GET /api/orders: 배열(PageResponse 아님), 최신순.
 * - GET /api/orders/{id}: 타인 주문은 403 FORBIDDEN, 없는 주문은 404 NOT_FOUND.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        ProductRepository productRepository,
                        CartItemRepository cartItemRepository,
                        CartService cartService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
    }

    /**
     * POST /api/orders: 체크아웃. 아래 전 과정을 하나의 트랜잭션으로 묶어
     * "재고만 줄고 주문은 안 생기는" 정합성 붕괴를 막는다.
     * 재고 부족이면 예외를 던져 트랜잭션 전체를 롤백하므로 부분 차감이 남지 않는다(all-or-nothing).
     */
    @Transactional
    public OrderResponse checkout(Long userId) {
        Cart cart = cartService.getOrCreateCart(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new CartEmptyException("장바구니가 비어 있어 주문할 수 없습니다.");
        }

        // 상품을 일괄 조회한다(N+1 방지). 가격/이름은 이 시점 값을 스냅샷으로 사용한다.
        Map<Long, Product> productsById = findProductsById(
                cartItems.stream().map(CartItem::getProductId).distinct().toList());

        // 1) 재고 차감: 조건부 UPDATE 의 갱신 행 수로 재고 부족을 판정한다.
        //    검사와 차감이 DB 단일 문장 안에서 원자적으로 일어나므로 동시 주문에도 재고가 음수가 되지 않는다.
        //    (근거는 ProductRepository.decreaseStockIfAvailable 의 주석 참고)
        for (CartItem cartItem : cartItems) {
            Product product = requireProduct(productsById, cartItem.getProductId());
            int updatedRows = productRepository.decreaseStockIfAvailable(product.getId(), cartItem.getQuantity());
            if (updatedRows == 0) {
                throw new OutOfStockException("재고가 부족합니다. productId=" + product.getId());
            }
        }

        // 2) 주문 생성. totalAmount 는 스냅샷 단가 기준 소계 합(정수 원).
        int totalAmount = cartItems.stream()
                .mapToInt(item -> requireProduct(productsById, item.getProductId()).getPrice() * item.getQuantity())
                .sum();
        Order order = orderRepository.save(new Order(userId, totalAmount));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = requireProduct(productsById, cartItem.getProductId());
            orderItems.add(new OrderItem(order.getId(), product.getId(),
                    cartItem.getQuantity(), product.getPrice()));
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        // 3) 장바구니 비우기.
        cartItemRepository.deleteAll(cartItems);

        List<OrderItemResponse> itemResponses = savedItems.stream()
                .map(item -> OrderMapper.toItemResponse(item,
                        requireProduct(productsById, item.getProductId()).getName()))
                .toList();
        // 방금 생성한 주문은 아직 결제 이력이 없으므로 payment 는 null 이다.
        return OrderMapper.toOrderResponse(order, itemResponses, null);
    }

    /** GET /api/orders: 내 주문 목록(최신순 배열). */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        // 아이템/결제/상품을 각각 한 번의 질의로 일괄 조회해 주문 수만큼 질의가 늘어나지 않게 한다.
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdInOrderByIdAsc(orderIds)
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<Long, Payment> paymentsByOrderId = paymentRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(Payment::getOrderId, Function.identity()));
        Map<Long, Product> productsById = findProductsById(itemsByOrderId.values().stream()
                .flatMap(List::stream)
                .map(OrderItem::getProductId)
                .distinct()
                .toList());

        return orders.stream()
                .map(order -> buildOrderResponse(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of()),
                        paymentsByOrderId.get(order.getId()),
                        productsById))
                .toList();
    }

    /** GET /api/orders/{id}: 주문 상세. 본인 소유가 아니면 403. */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = findOwnedOrder(userId, orderId);
        return toOrderResponse(order);
    }

    /**
     * id 로 주문을 찾고, 요청자가 소유자가 아니면 403 을 던진다.
     * 결제(PaymentService)도 동일한 소유권 검증이 필요하므로 공개 메서드로 둔다.
     */
    @Transactional(readOnly = true)
    public Order findOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("본인 소유의 주문이 아닙니다.");
        }
        return order;
    }

    /** 단건 주문을 아이템·결제 정보와 결합해 OrderResponse 로 조립한다. */
    @Transactional(readOnly = true)
    public OrderResponse toOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        Map<Long, Product> productsById = findProductsById(
                items.stream().map(OrderItem::getProductId).distinct().toList());
        return buildOrderResponse(order, items, payment, productsById);
    }

    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items, Payment payment,
                                             Map<Long, Product> productsById) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderMapper.toItemResponse(item,
                        requireProduct(productsById, item.getProductId()).getName()))
                .toList();
        PaymentResponse paymentResponse = payment != null ? PaymentMapper.toResponse(payment) : null;
        return OrderMapper.toOrderResponse(order, itemResponses, paymentResponse);
    }

    private Map<Long, Product> findProductsById(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Product requireProduct(Map<Long, Product> productsById, Long productId) {
        Product product = productsById.get(productId);
        if (product == null) {
            throw new ProductNotFoundException("상품을 찾을 수 없습니다. id=" + productId);
        }
        return product;
    }
}
