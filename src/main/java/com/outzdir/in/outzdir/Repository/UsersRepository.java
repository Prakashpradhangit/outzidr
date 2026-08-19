package com.outzdir.in.outzdir.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.outzdir.in.outzdir.Entity.Users;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Users findByEmail(String email);
}
