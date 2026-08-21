package com.outzdir.in.outzdir.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.outzdir.in.outzdir.Entity.Cart;
import com.outzdir.in.outzdir.Entity.Users;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(Users user);
}
