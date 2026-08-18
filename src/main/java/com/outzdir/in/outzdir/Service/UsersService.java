package com.outzdir.in.outzdir.Service;

import java.util.List;

import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Repository.UsersRepository;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class UsersService {

    private final UsersRepository usersRepository;

    
    public List<Users> findAllUser(){
        return usersRepository.findAll();
    }
}
