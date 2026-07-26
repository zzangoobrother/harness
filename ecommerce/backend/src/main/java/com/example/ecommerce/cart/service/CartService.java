package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.AddCartItemRequest;
import com.example.ecommerce.cart.dto.CartItemResponse;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.cart.mapper.CartMapper;
import com.example.ecommerce.cart.repository.CartItemRepository;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.common.exception.CartItemNotFoundException;
import com.example.ecommerce.common.exception.ForbiddenException;
import com.example.ecommerce.common.exception.OutOfStockException;
import com.example.ecommerce.common.exception.ProductNotFoundException;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 장바구니 비즈니스 로직. 계약 api-spec.md 3절 규칙:
 * - 사용자당 카트 1개, 최초 접근 시 지연 생성.
 * - 동일 productId 재추가 시 수량 합산, (cartId, productId) 복합 유니크로 경합 방어.
 * - 재고 검증만 수행(차감은 Stage 4 주문 생성 시점).
 * - 타인 소유 CartItem 조작은 403 FORBIDDEN(404 아님).
 * - 모든 응답은 CartResponse 전체.
 */
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    /** 사용자의 장바구니를 없으면 생성하여 반환한다(지연 생성). */
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    /** GET /api/cart: 카트가 없으면 생성 후 빈 카트를 반환한다. */
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    /**
     * POST /api/cart/items: 이미 담긴 productId 면 수량을 합산한다.
     * 동시 추가로 유니크 제약을 위반하면(DataIntegrityViolationException) 기존 행에 병합한다.
     */
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "상품을 찾을 수 없습니다. id=" + request.productId()));

        cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId())
                .ifPresentOrElse(
                        existing -> mergeQuantity(existing, product, request.quantity()),
                        () -> insertNewItem(cart, product, request.quantity())
                );

        return buildCartResponse(cart);
    }

    /** PATCH /api/cart/items/{id}: 수량을 절대값으로 변경한다. */
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = findOwnedItem(userId, itemId);
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "상품을 찾을 수 없습니다. id=" + item.getProductId()));

        validateStock(product, request.quantity());
        item.changeQuantity(request.quantity());

        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new CartItemNotFoundException("장바구니를 찾을 수 없습니다."));
        return buildCartResponse(cart);
    }

    /** DELETE /api/cart/items/{id}: 아이템 제거 후 전체 장바구니를 반환한다. */
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        CartItem item = findOwnedItem(userId, itemId);
        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new CartItemNotFoundException("장바구니를 찾을 수 없습니다."));

        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    /** id로 CartItem을 찾고, 요청자가 소유자가 아니면 403을 던진다. */
    private CartItem findOwnedItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "장바구니 아이템을 찾을 수 없습니다. id=" + itemId));
        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new CartItemNotFoundException("장바구니를 찾을 수 없습니다."));
        if (!cart.getUserId().equals(userId)) {
            throw new ForbiddenException("본인 소유의 장바구니 아이템이 아닙니다.");
        }
        return item;
    }

    private void mergeQuantity(CartItem existing, Product product, int addedQuantity) {
        int merged = existing.getQuantity() + addedQuantity;
        validateStock(product, merged);
        existing.changeQuantity(merged);
    }

    private void insertNewItem(Cart cart, Product product, int quantity) {
        validateStock(product, quantity);
        CartItem newItem = new CartItem(cart.getId(), product.getId(), quantity);
        try {
            cartItemRepository.saveAndFlush(newItem);
        } catch (DataIntegrityViolationException raceCondition) {
            // 동시 요청이 먼저 같은 (cartId, productId) 행을 만든 경우: 기존 행에 병합한다.
            CartItem existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                    .orElseThrow(() -> raceCondition);
            mergeQuantity(existing, product, quantity);
        }
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new OutOfStockException("재고가 부족합니다. productId=" + product.getId());
        }
    }

    /** 카트의 아이템 전체를 상품 정보와 결합해 CartResponse로 조립한다(상품 조회는 일괄 처리하여 N+1을 피한다). */
    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            return CartMapper.toCartResponse(cart, List.of());
        }

        List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().toList();
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    Product product = productsById.get(item.getProductId());
                    if (product == null) {
                        throw new ProductNotFoundException(
                                "상품을 찾을 수 없습니다. id=" + item.getProductId());
                    }
                    return CartMapper.toItemResponse(item, product);
                })
                .toList();

        return CartMapper.toCartResponse(cart, itemResponses);
    }
}
