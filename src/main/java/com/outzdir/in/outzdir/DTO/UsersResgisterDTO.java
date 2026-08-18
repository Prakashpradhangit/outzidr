package com.outzdir.in.outzdir.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersResgisterDTO {
    private String name;

    @NotNull(message="Email is required")
    @Email(message = "invalid email format")
    private String email;

    @NotNull(message = "Phone Number is required")
    private String phoneNumber;

    @NotNull(message = "Password is required")
    private String password;
}
