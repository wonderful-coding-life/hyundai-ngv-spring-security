package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductApiController {
    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/version")
    public String getVersion() {
        return "1.0.0";
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productRepository.findById(id).orElseThrow();
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
