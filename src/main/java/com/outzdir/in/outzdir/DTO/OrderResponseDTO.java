package com.outzdir.in.outzdir.DTO;

import java.time.LocalDateTime;
import java.util.List;

import com.outzdir.in.outzdir.Entity.Orderitem;
import com.outzdir.in.outzdir.Entity.TYPE.OrderStatus;
import com.outzdir.in.outzdir.Entity.TYPE.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    
    private Long id;
    private OrderStatus orderStatus;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private Double subtotal;
    private Double discountAmount;
    private Double total;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDTO> orderItems;
   

}
