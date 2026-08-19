package com.outzdir.in.outzdir.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.outzdir.in.outzdir.Entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{
    
}
