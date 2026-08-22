package com.outzdir.in.outzdir.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.outzdir.in.outzdir.Entity.Discount;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    Optional<Discount> findByCode(String code);
}
