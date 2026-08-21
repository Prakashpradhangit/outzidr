package com.outzdir.in.outzdir.Entity;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Getter
@Setter
@Table(name = "cartitems", uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"}))
public class Cartitems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    
    @JsonIgnore
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    private Long quantity;

    private Double unitPrice;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
