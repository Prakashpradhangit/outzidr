package com.outzdir.in.outzdir.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.outzdir.in.outzdir.Entity.Orderitem;

public interface OrderitemRepository extends JpaRepository<Orderitem, Long> {
}
