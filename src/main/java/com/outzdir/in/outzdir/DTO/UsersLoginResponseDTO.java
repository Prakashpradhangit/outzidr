package com.outzdir.in.outzdir.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsersLoginResponseDTO {
    private String jwt;
    private Long id;
    private String name;
    private String email;
    
}
