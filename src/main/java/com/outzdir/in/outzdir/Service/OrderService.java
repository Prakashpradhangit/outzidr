package com.outzdir.in.outzdir.Service;

import org.modelmapper.ModelMapper;
import org.springframework.boot.security.autoconfigure.SecurityProperties.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.outzdir.in.outzdir.DTO.OrderRequestDTO;
import com.outzdir.in.outzdir.DTO.OrderResponseDTO;
import com.outzdir.in.outzdir.Entity.*;
import com.outzdir.in.outzdir.Entity.TYPE.*;
import com.outzdir.in.outzdir.Repository.*;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final ModelMapper modelMapper;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UsersRepository usersRepository;
    

    

    // 
    // Creates a new Order from the authenticated user's cart. 
    // Using Thread lock
    public Order createOrder(OrderRequestDTO request, String email) {
        // 1. Retrieve the authenticated user
        Users user = usersRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // 2. Fetch the user's shopping cart
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        // 3. Verify the cart contains at least one item
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place order. The shopping cart is empty.");
        }

        // 4. Revalidate product availability and lock inventory
        for (Cartitems item : cart.getCartItems()) {
            // Fetch product using PESSIMISTIC_WRITE lock to prevent race conditions
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProduct().getId()));

            // Verify active status
            if (product.getProductStatus() != ProductStatus.ACTIVE) {
                throw new IllegalArgumentException("Product is no longer active: " + product.getProduct_name());
            }

            // Verify stock availability
            if (item.getQuantity() > product.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getProduct_name() + 
                        ". Available stock: " + product.getQuantity());
            }

            // 5. Revalidate product prices from the database
            item.setUnitPrice(product.getPrice());
        }

        // 6. Recalculate cart subtotals and discounts
        double subtotal = cart.getCartItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        double discountAmount = 0.0;
        Discount appliedDiscount = cart.getAppliedDiscount();

        if (appliedDiscount != null) {
            // Verify coupon active status
            if (appliedDiscount.getActive() != DiscountStatus.ACTIVE) {
                throw new IllegalArgumentException("Applied coupon is no longer active.");
            }

            // Verify coupon expiry dates
            LocalDateTime now = LocalDateTime.now();
            if (appliedDiscount.getStartDate() != null && now.isBefore(appliedDiscount.getStartDate())) {
                throw new IllegalArgumentException("Applied coupon is not yet active.");
            }
            if (appliedDiscount.getEndDate() != null && now.isAfter(appliedDiscount.getEndDate())) {
                throw new IllegalArgumentException("Applied coupon has expired.");
            }

            // Verify minimum cart value rules
            if (appliedDiscount.getMinCartValue() != null && subtotal < appliedDiscount.getMinCartValue()) {
                throw new IllegalArgumentException("Minimum cart value of " + appliedDiscount.getMinCartValue() + 
                        " is required for this coupon. Current subtotal: " + subtotal);
            }

            // Verify product mapping eligibility rules
            List<DiscountItem> items = appliedDiscount.getDiscountItems();
            if (items == null || items.isEmpty()) {
                // Sitewide discount
                if (appliedDiscount.getType() == DiscountType.PERCENTAGE) {
                    discountAmount = subtotal * (appliedDiscount.getValue() / 100.0);
                } else if (appliedDiscount.getType() == DiscountType.FLAT) {
                    discountAmount = appliedDiscount.getValue();
                }
            } else {
                // Product-specific discount
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
                } else {
                    throw new IllegalArgumentException("None of the products in your cart are eligible for the applied coupon.");
                }
            }

            // Enforce max discount cap
            if (appliedDiscount.getMaxDiscount() != null && discountAmount > appliedDiscount.getMaxDiscount()) {
                discountAmount = appliedDiscount.getMaxDiscount();
            }
        }

        discountAmount = Math.min(discountAmount, subtotal);
        double finalTotal = Math.max(0.0, subtotal - discountAmount);

        // 7. Create the order
        Order order = new Order();
        order.setUser(user);
        order.setAppliedDiscount(appliedDiscount);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setTotal(finalTotal);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // 8. Create the order items and reduce stock inventory
        for (Cartitems item : cart.getCartItems()) {
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            // Create Orderitem
            Orderitem orderitem = new Orderitem();
            orderitem.setOrder(order);
            orderitem.setProduct(product);
            orderitem.setQuantity(item.getQuantity());
            orderitem.setUnitPrice(item.getUnitPrice());
            order.getOrderItems().add(orderitem);

            // Reduce product stock inventory
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        // 9. Clear the cart
        cart.getCartItems().clear();
        cart.setAppliedDiscount(null);
        cart.setSubtotal(0.0);
        cart.setDiscountAmount(0.0);
        cart.setTotal(0.0);
        cartRepository.save(cart);

        // 10. Save and return the order
        return orderRepository.save(order);
    }

    public List<OrderResponseDTO> getOrders(String email) {
        Users users = usersRepository.findByEmail(email);
        if (users == null) {
            throw new IllegalArgumentException("User not found");
        }
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(users);
        return orders.stream().map(order -> modelMapper.map(order, OrderResponseDTO.class)).toList();
    }
    
    
}
