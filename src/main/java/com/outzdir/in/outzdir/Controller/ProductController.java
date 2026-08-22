package com.outzdir.in.outzdir.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.outzdir.in.outzdir.Entity.Product;
import com.outzdir.in.outzdir.Repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("products")
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public Page<Product> getProductByPage(@RequestParam int page,@RequestParam  int size){
        return productRepository.findAll(PageRequest.of(page, size));
    }
    
    @GetMapping("/all")
    public List<Product> getAllProduct(){
        return productRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> findProductById(@PathVariable Long id){
        Optional<Product> product = productRepository.findById(id);
        if(product.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(product);

    }
    
}
