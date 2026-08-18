package com.outzdir.in.outzdir.Controller;

import java.util.List;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Service.UsersService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping
    public List<Users> findAll(){
        return usersService.findAllUser();
    }
    
}
