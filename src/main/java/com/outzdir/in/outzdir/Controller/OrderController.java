package com.outzdir.in.outzdir.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.outzdir.in.outzdir.DTO.OrderRequestDTO;
import com.outzdir.in.outzdir.Entity.Order;
import com.outzdir.in.outzdir.Service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    
    // Place the order
    @PostMapping
    public ResponseEntity<Order> createOrder( @RequestBody @Valid OrderRequestDTO request, Principal principal) {
        
        // Delegates order creation and validation to the order service layer
        Order order = orderService.createOrder(request, principal.getName());
        return ResponseEntity.ok(order);
    }

    
    // Endpoint to fetch all orders belonging to the currently authenticated user.
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(Principal principal){
        return ResponseEntity.ok(orderService.getOrders(principal.getName()));
    }
}
