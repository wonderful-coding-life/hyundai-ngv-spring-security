package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/list")
    public String getProductList(Model model) {
        var products = productRepository.findAll();
        model.addAttribute("products", products);
        return "product-list";
    }

    @GetMapping("/add")
    public String getProductAdd() {
        return "product-add";
    }

    @PostMapping("/add")
    public String postProductAdd(Product product) {
        productRepository.save(product);
        return "redirect:/product/list";
    }

    @GetMapping("/edit")
    public String getProductEdit(@RequestParam("id") Long id, Model model) {
        var product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        return "product-edit";
    }

    @PostMapping("/edit")
    public String postProductEdit(Product product) {
        productRepository.save(product);
        return "redirect:/product/list";
    }

    @GetMapping("/delete")
    public String getProductDelete(@RequestParam("id") Long id) {
        var product = productRepository.findById(id).orElseThrow();
        productRepository.delete(product);
        return "redirect:/product/list";
    }
}
