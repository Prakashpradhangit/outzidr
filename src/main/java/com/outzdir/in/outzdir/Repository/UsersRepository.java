package com.outzdir.in.outzdir.Repository;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.outzdir.in.outzdir.Entity.Users;

public interface UsersRepository extends JpaRepository<Users, Long> {

}
