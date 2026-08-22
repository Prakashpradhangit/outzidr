package com.outzdir.in.outzdir.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.outzdir.in.outzdir.Entity.TYPE.CartStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountType;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private Users user;
    
    @Enumerated(EnumType.STRING)
    private CartStatus cartStatus = CartStatus.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cartitems> cartItems = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount appliedDiscount;

    private Double subtotal = 0.0;
    private Double discountAmount = 0.0;
    private Double total = 0.0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
