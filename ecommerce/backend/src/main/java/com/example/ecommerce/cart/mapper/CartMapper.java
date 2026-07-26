package com.example.ecommerce.cart.mapper;

import com.example.ecommerce.cart.dto.CartItemResponse;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.entity.Cart;
import com.example.ecommerce.cart.entity.CartItem;
import com.example.ecommerce.product.entity.Product;

import java.util.List;

/**
 * Cart/CartItem 엔티티 → 응답 DTO 변환. 상품 정보(이름/이미지/현재가)는
 * CartItem 이 직접 들고 있지 않으므로 호출 측(CartService)이 조회한 Product 를 함께 넘긴다.
 */
public final class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toItemResponse(CartItem item, Product product) {
        int subtotal = product.getPrice() * item.getQuantity();
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                item.getQuantity(),
                subtotal
        );
    }

    public static CartResponse toCartResponse(Cart cart, List<CartItemResponse> items) {
        int totalAmount = items.stream().mapToInt(CartItemResponse::subtotal).sum();
        return new CartResponse(cart.getId(), items, totalAmount);
    }
}
