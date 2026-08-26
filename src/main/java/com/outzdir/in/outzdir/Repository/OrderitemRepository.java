package com.outzdir.in.outzdir.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.outzdir.in.outzdir.Entity.Orderitem;

public interface OrderitemRepository extends JpaRepository<Orderitem, Long> {

    List<Orderitem> findByOrderId(Long order_id);
}
