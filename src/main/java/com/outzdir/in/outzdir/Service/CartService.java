package com.outzdir.in.outzdir.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.outzdir.in.outzdir.DTO.CartItemRequestDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Entity.TYPE.CartStatus;
import com.outzdir.in.outzdir.Entity.TYPE.ProductStatus;
import com.outzdir.in.outzdir.Entity.Cart;
import com.outzdir.in.outzdir.Entity.Cartitems;
import com.outzdir.in.outzdir.Entity.Product;
import com.outzdir.in.outzdir.Repository.CartRepository;
import com.outzdir.in.outzdir.Repository.CartitemsRepository;
import com.outzdir.in.outzdir.Repository.ProductRepository;
import com.outzdir.in.outzdir.Repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartitemsRepository cartitemsRepository;
    private final ProductRepository productsRepository;
    private final UsersRepository usersRepository;

    public Cart getMyCart(String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setCartStatus(CartStatus.ACTIVE);
                    cart.setCreatedAt(LocalDateTime.now());
                    cart.setUpdatedAt(LocalDateTime.now());
                    return cartRepository.save(cart);
                });
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

        cartRepository.save(cart);
    }

    public void removeCartItem(Long productId, String authenticatedEmail) {
        Users user = usersRepository.findByEmail(authenticatedEmail);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        Cartitems cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not in cart"));

        cart.getCartItems().remove(cartItem);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }
}
