package com.outzdir.in.outzdir.DTO;

import com.outzdir.in.outzdir.Entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private Long id;
    private Product product;
    private Long quantity;
    private Double unitPrice;
}
