package com.outzdir.in.outzdir.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.outzdir.in.outzdir.DTO.CartItemRequestDTO;
import com.outzdir.in.outzdir.DTO.RemoveProductFromCartDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Entity.TYPE.CartStatus;
import com.outzdir.in.outzdir.Entity.TYPE.ProductStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountType;
import com.outzdir.in.outzdir.Entity.Cart;
import com.outzdir.in.outzdir.Entity.Cartitems;
import com.outzdir.in.outzdir.Entity.Product;
import com.outzdir.in.outzdir.Entity.Discount;
import com.outzdir.in.outzdir.Entity.DiscountItem;
import com.outzdir.in.outzdir.Repository.CartRepository;
import com.outzdir.in.outzdir.Repository.CartitemsRepository;
import com.outzdir.in.outzdir.Repository.ProductRepository;
import com.outzdir.in.outzdir.Repository.UsersRepository;
import com.outzdir.in.outzdir.Repository.DiscountRepository;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartitemsRepository cartitemsRepository;
    private final ProductRepository productsRepository;
    private final UsersRepository usersRepository;
    private final DiscountRepository discountRepository;

    public Cart getMyCart(String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setCartStatus(CartStatus.ACTIVE);
                    newCart.setCreatedAt(LocalDateTime.now());
                    newCart.setUpdatedAt(LocalDateTime.now());
                    return cartRepository.save(newCart);
                });

        recalculateCart(cart);
        return cartRepository.save(cart);
    }

    public void addProductToCart(CartItemRequestDTO cartDTO, String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (cartDTO.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Product product = productsRepository.findById(cartDTO.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getProductStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Product is inactive");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setCartStatus(CartStatus.ACTIVE);
                    newCart.setCreatedAt(LocalDateTime.now());
                    newCart.setUpdatedAt(LocalDateTime.now());
                    return cartRepository.save(newCart);
                });

        final Cart finalCart = cart;
        Optional<Cartitems> existingItemOpt = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        long currentQtyInCart = existingItemOpt.map(Cartitems::getQuantity).orElse(0L);
        long newTotalQty = currentQtyInCart + cartDTO.getQuantity();

        if (newTotalQty > product.getQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity exceeds available stock (" + product.getQuantity() + ")");
        }

        if (existingItemOpt.isPresent()) {
            Cartitems cartitems = existingItemOpt.get();
            cartitems.setQuantity(newTotalQty);
            cartitems.setUpdatedAt(LocalDateTime.now());
        } else {
            Cartitems cartitems = new Cartitems();
            cartitems.setCart(finalCart);
            cartitems.setProduct(product);
            cartitems.setQuantity(cartDTO.getQuantity());
            cartitems.setUnitPrice(product.getPrice());
            cartitems.setCreatedAt(LocalDateTime.now());
            cartitems.setUpdatedAt(LocalDateTime.now());
            finalCart.getCartItems().add(cartitems);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        recalculateCart(cart);
        cartRepository.save(cart);
    }

    public void updateCartItemQuantity(Long productId, Long newQuantity, String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        Cartitems cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not in cart"));

        Product product = cartItem.getProduct();
        if (product.getProductStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("Product is inactive");
        }

        if (newQuantity > product.getQuantity()) {
            throw new IllegalArgumentException(
                    "Requested quantity exceeds available stock (" + product.getQuantity() + ")");
        }

        cartItem.setQuantity(newQuantity);
        cartItem.setUpdatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        recalculateCart(cart);
        cartRepository.save(cart);
    }

    public void removeCartItem(RemoveProductFromCartDTO removeDTO, String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        Cartitems cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(removeDTO.getProduct_id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not in cart"));

        cart.getCartItems().remove(cartItem);

        if (cart.getCartItems().isEmpty()) {
            cart.setAppliedDiscount(null);
        }

        cart.setUpdatedAt(LocalDateTime.now());

        recalculateCart(cart);
        cartRepository.save(cart);
    }

    public void applyCoupon(String code, String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Coupon code not found"));

        if (discount.getActive() != DiscountStatus.ACTIVE) {
            throw new IllegalArgumentException("Coupon is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new IllegalArgumentException("Coupon is not yet active");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new IllegalArgumentException("Coupon has expired");
        }

        double subtotal = cart.getCartItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        if (discount.getMinCartValue() != null && subtotal < discount.getMinCartValue()) {
            throw new IllegalArgumentException("Minimum cart value of " + discount.getMinCartValue() + " required to apply this coupon");
        }

        if (discount.getDiscountItems() != null && !discount.getDiscountItems().isEmpty()) {
            List<Long> eligibleProductIds = discount.getDiscountItems().stream()
                    .map(di -> di.getProduct().getId())
                    .toList();

            boolean hasEligibleProduct = cart.getCartItems().stream()
                    .anyMatch(item -> eligibleProductIds.contains(item.getProduct().getId()));

            if (!hasEligibleProduct) {
                throw new IllegalArgumentException("This coupon is not applicable to any products in your cart");
            }
        }

        cart.setAppliedDiscount(discount);
        cart.setUpdatedAt(now);
        recalculateCart(cart);
        cartRepository.save(cart);
    }

    public void removeCoupon(String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        cart.setAppliedDiscount(null);
        cart.setUpdatedAt(LocalDateTime.now());
        recalculateCart(cart);
        cartRepository.save(cart);
    }

    private void recalculateCart(Cart cart) {
        if (cart == null) {
            return;
        }

        double subtotal = 0.0;
        if (cart.getCartItems() != null) {
            subtotal = cart.getCartItems().stream()
                    .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                    .sum();
        }
        cart.setSubtotal(subtotal);

        double discountAmount = 0.0;
        Discount appliedDiscount = cart.getAppliedDiscount();
        if (appliedDiscount != null && appliedDiscount.getActive() == DiscountStatus.ACTIVE) {
            LocalDateTime now = LocalDateTime.now();
            boolean isDateValid = (appliedDiscount.getStartDate() == null || !now.isBefore(appliedDiscount.getStartDate())) &&
                                  (appliedDiscount.getEndDate() == null || !now.isAfter(appliedDiscount.getEndDate()));

            boolean isMinCartValid = appliedDiscount.getMinCartValue() == null || subtotal >= appliedDiscount.getMinCartValue();

            if (isDateValid && isMinCartValid) {
                List<DiscountItem> items = appliedDiscount.getDiscountItems();
                if (items == null || items.isEmpty()) {
                    if (appliedDiscount.getType() == DiscountType.PERCENTAGE) {
                        discountAmount = subtotal * (appliedDiscount.getValue() / 100.0);
                    } else if (appliedDiscount.getType() == DiscountType.FLAT) {
                        discountAmount = appliedDiscount.getValue();
                    }
                } else {
                    List<Long> eligibleProductIds = items.stream()
                            .map(di -> di.getProduct().getId())
                            .toList();

                    double eligibleSubtotal = cart.getCartItems().stream()
                            .filter(item -> eligibleProductIds.contains(item.getProduct().getId()))
                            .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                            .sum();

                    if (eligibleSubtotal > 0.0) {
                        if (appliedDiscount.getType() == DiscountType.PERCENTAGE) {
                            discountAmount = eligibleSubtotal * (appliedDiscount.getValue() / 100.0);
                        } else if (appliedDiscount.getType() == DiscountType.FLAT) {
                            discountAmount = appliedDiscount.getValue();
                        }
                    }
                }

                if (appliedDiscount.getMaxDiscount() != null && discountAmount > appliedDiscount.getMaxDiscount()) {
                    discountAmount = appliedDiscount.getMaxDiscount();
                }
            }
        }

        discountAmount = Math.min(discountAmount, subtotal);
        cart.setDiscountAmount(discountAmount);
        cart.setTotal(Math.max(0.0, subtotal - discountAmount));
    }
}
