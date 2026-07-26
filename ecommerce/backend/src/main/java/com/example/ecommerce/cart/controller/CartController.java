package com.example.ecommerce.cart.controller;

import com.example.ecommerce.cart.dto.AddCartItemRequest;
import com.example.ecommerce.cart.dto.CartResponse;
import com.example.ecommerce.cart.dto.UpdateCartItemRequest;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.config.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장바구니 API. 계약 api-spec.md 3절, 모두 인증 필요(SecurityConfig 의 anyRequest().authenticated()):
 * - GET    /api/cart            → 200, CartResponse (없으면 지연 생성 후 빈 카트)
 * - POST   /api/cart/items      → 201, CartResponse
 * - PATCH  /api/cart/items/{id} → 200, CartResponse
 * - DELETE /api/cart/items/{id} → 200, CartResponse
 * 모든 응답이 CartResponse 전체를 반환한다(계약 가정 C).
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.userId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal AuthPrincipal principal,
                                                @Valid @RequestBody AddCartItemRequest request) {
        CartResponse response = cartService.addItem(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal AuthPrincipal principal,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(principal.userId(), id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal AuthPrincipal principal,
                                                   @PathVariable Long id) {
        return ResponseEntity.ok(cartService.removeItem(principal.userId(), id));
    }
}
