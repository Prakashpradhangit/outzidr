package com.outzdir.in.outzdir.Controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outzdir.in.outzdir.DTO.UsersSignUpDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginRequestDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginResponseDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Service.UsersService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class UsersController {

    private final UsersService usersService;

   

    @PostMapping("/login")
    public ResponseEntity<UsersLoginResponseDTO> login(@RequestBody  UsersLoginRequestDTO usersLoginRequestDTO){
        return ResponseEntity.ok(usersService.login(usersLoginRequestDTO));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUpUser(@RequestBody UsersSignUpDTO usersSignUpDTO){
        return usersService.signup(usersSignUpDTO);
    }
    
    
    
}
