package com.outzdir.in.outzdir.Service;

import java.util.List;
import java.util.Optional;

import org.apache.catalina.User;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.outzdir.in.outzdir.DTO.UsersResgisterDTO;
import com.outzdir.in.outzdir.DTO.UsersResponseDTO;
import com.outzdir.in.outzdir.Entity.Users;
import com.outzdir.in.outzdir.Repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UsersService {

    private final ModelMapper modelMapper;
    private final UsersRepository usersRepository;

    public List<UsersResponseDTO> findAllUser() {
        List<Users> users = usersRepository.findAll();
        return users
                .stream()
                .map(user -> modelMapper.map(user, UsersResponseDTO.class))
                .toList();
    }

    public ResponseEntity<?> createUser(UsersResgisterDTO usersResgisterDTO) {
        Users newUser = modelMapper.map(usersResgisterDTO, Users.class);
        Users user = usersRepository.save(newUser);
        return ResponseEntity.ok(modelMapper.map(user, UsersResponseDTO.class));
    }

    public ResponseEntity<?> findUserByid(Long id) {
        Optional<Users> userOpt = usersRepository.findById(id);
        if (userOpt != null) {
            UsersResponseDTO usersResponseDTO = modelMapper.map(userOpt.get(), UsersResponseDTO.class);
            return ResponseEntity.ok(usersResponseDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not exist with id: " + id);
        }
    }

    public ResponseEntity<String> deleteUser(Long id) {
        if (usersRepository.existsById(id)) {
            usersRepository.deleteById(id);
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not exist with id: " + id);
        }
    }

}
