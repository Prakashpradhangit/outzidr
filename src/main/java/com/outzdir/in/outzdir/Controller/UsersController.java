package com.outzdir.in.outzdir.Controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outzdir.in.outzdir.DTO.UsersResgisterDTO;
import com.outzdir.in.outzdir.DTO.UsersResponseDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Service.UsersService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UsersController {

    private final UsersService usersService;

    @GetMapping
    public List<UsersResponseDTO> findAll(){
        return usersService.findAllUser();
    }

    @PostMapping
    public ResponseEntity<?> createNewUser(@RequestBody @Valid UsersResgisterDTO usersResgisterDTO){
        return usersService.createUser(usersResgisterDTO);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id){
        return usersService.findUserByid(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteuserById(@PathVariable Long id){
        return usersService.deleteUser(id);
    }
}
