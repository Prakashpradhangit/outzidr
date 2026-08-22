package com.outzdir.in.outzdir.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outzdir.in.outzdir.DTO.CartItemRequestDTO;
import com.outzdir.in.outzdir.DTO.RemoveProductFromCartDTO;
import com.outzdir.in.outzdir.DTO.UpdateCartItemRequestDTO;
import com.outzdir.in.outzdir.DTO.ApplyCouponRequestDTO;
import com.outzdir.in.outzdir.Entity.Cart;
import com.outzdir.in.outzdir.Service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getMyCart(Principal principal) {
        return ResponseEntity.ok(cartService.getMyCart(principal.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<String> addProductToCart(@RequestBody @Valid CartItemRequestDTO cartDTO, Principal principal) {
        cartService.addProductToCart(cartDTO, principal.getName());
        return ResponseEntity.ok("Product added to cart successfully");
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<String> updateCartItemQuantity(
            @PathVariable Long productId,
            @RequestBody @Valid UpdateCartItemRequestDTO updateDTO,
            Principal principal) {
        cartService.updateCartItemQuantity(productId, updateDTO.getQuantity(), principal.getName());
        return ResponseEntity.ok("Cart item quantity updated successfully");
    }

    @DeleteMapping("/items")
    public ResponseEntity<String> removeCartItem(@RequestBody RemoveProductFromCartDTO removeDTO, Principal principal) {
        cartService.removeCartItem(removeDTO, principal.getName());
        return ResponseEntity.ok("Item removed from cart successfully");
    }


    @PostMapping("/coupon")
    public ResponseEntity<String> applyCoupon(@RequestBody @Valid ApplyCouponRequestDTO couponDTO, Principal principal) {
        cartService.applyCoupon(couponDTO.getCode(), principal.getName());
        return ResponseEntity.ok("Coupon applied successfully");
    }

    

    @DeleteMapping("/coupon")
    public ResponseEntity<String> removeCoupon(Principal principal) {
        cartService.removeCoupon(principal.getName());
        return ResponseEntity.ok("Coupon removed successfully");
    }
}
