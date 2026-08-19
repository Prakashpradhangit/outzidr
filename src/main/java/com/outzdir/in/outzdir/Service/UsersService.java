package com.outzdir.in.outzdir.Service;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.outzdir.in.outzdir.DTO.UsersSignUpDTO;
import com.outzdir.in.outzdir.DTO.UsersSignUpResponseDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginRequestDTO;
import com.outzdir.in.outzdir.DTO.UsersLoginResponseDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Repository.UsersRepository;
import com.outzdir.in.outzdir.Security.AuthUtil;
import com.outzdir.in.outzdir.Security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UsersService {

    private final ModelMapper modelMapper;
    private final UsersRepository usersRepository;
    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final PasswordEncoder passwordEncoder;

    public UsersLoginResponseDTO login(UsersLoginRequestDTO usersLoginRequestDTO) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usersLoginRequestDTO.getEmail(),
                        usersLoginRequestDTO.getPassword()));

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        Users users = customUserDetails.getUser();

        String token = authUtil.generateAccessToken(users);

        return new UsersLoginResponseDTO(token, users.getId(), users.getEmail(), users.getName());

    }

    public ResponseEntity<?> signup(UsersSignUpDTO usersSignUpDTO) {

        Users user = usersRepository.findByEmail(usersSignUpDTO.getEmail());
        if (user != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("User already exist with email: " + usersSignUpDTO.getEmail());
        }

        Users users = modelMapper.map(usersSignUpDTO, Users.class);
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        usersRepository.save(users);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsersSignUpResponseDTO(users.getId(), users.getName(), users.getEmail()));
    }

}
