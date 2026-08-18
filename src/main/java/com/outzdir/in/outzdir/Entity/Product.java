package com.outzdir.in.outzdir.Entity;

import com.outzdir.in.outzdir.Entity.TYPE.ProductStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Product {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product_name;

    private String product_description;

    private String sku;

    private Double price;

    private Long quantity;

    private String category;

    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;

}
