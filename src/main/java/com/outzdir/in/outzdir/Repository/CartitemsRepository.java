package com.outzdir.in.outzdir.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.outzdir.in.outzdir.Entity.Cartitems;

public interface CartitemsRepository extends JpaRepository<Cartitems, Long> {
    
}
