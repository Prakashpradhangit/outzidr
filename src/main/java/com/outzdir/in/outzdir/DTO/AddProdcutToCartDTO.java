package com.outzdir.in.outzdir.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddProdcutToCartDTO {
    private Long user_id;
    private Long product_id;
    private Long quantity;
}
