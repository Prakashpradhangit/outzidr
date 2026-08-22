package com.outzdir.in.outzdir.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyCouponRequestDTO {
    @NotBlank(message = "Coupon code is required")
    private String code;
}
