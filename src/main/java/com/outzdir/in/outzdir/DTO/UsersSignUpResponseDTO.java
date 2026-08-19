package com.outzdir.in.outzdir.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersSignUpResponseDTO {
    private Long id;
    private String name;
    private String email;
}
