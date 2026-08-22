package com.outzdir.in.outzdir.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.outzdir.in.outzdir.Entity.DiscountItem;

public interface DiscountItemRepository extends JpaRepository<DiscountItem, Long> {
    
}
