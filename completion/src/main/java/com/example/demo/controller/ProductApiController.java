package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class ProductApiController {
    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/version")
    public String getVersion(Authentication authentication) {
        if (authentication != null) {
            log.info("name {}", authentication.getName());
            log.info("authorities {}", authentication
                    .getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(" ")));
        } else {
            log.info("authentication null");
        }
        return "1.0.0";
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @PostMapping("/products")
    public Product postProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @PutMapping("/products/{id}")
    public Product putProduct(@PathVariable Long id, @RequestBody Product product) {
        productRepository.findById(id).orElseThrow();
        product.setId(id);
        return productRepository.save(product);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        var product = productRepository.findById(id).orElseThrow();
        productRepository.delete(product);
    }
}
