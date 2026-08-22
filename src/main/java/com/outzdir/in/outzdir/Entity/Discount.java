package com.outzdir.in.outzdir.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.outzdir.in.outzdir.Entity.TYPE.DiscountStatus;
import com.outzdir.in.outzdir.Entity.TYPE.DiscountType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "discount")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType type;

    private Double value;

    private Double minCartValue;

    private Double maxDiscount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    private DiscountStatus active;

    @OneToMany(mappedBy = "discount", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<DiscountItem> discountItems = new ArrayList<>();
}
