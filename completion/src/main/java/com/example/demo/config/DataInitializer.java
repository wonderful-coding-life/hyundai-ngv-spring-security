package com.example.demo.config;

import com.example.demo.entity.Member;
import com.example.demo.entity.Product;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (productRepository.count() == 0) {
            var products = List.of(
                    Product.builder().name("아이폰").price(1200000).build(),
                    Product.builder().name("맥북").price(950000).build()
            );
            productRepository.saveAll(products);
        }
        if (memberRepository.count() == 0) {
            var members = List.of(
                    Member.builder()
                            .name("seojun")
                            .password(passwordEncoder.encode("12345678"))
                            .authority("USER")
                            .build(),
                    Member.builder()
                            .name("kwangcheol")
                            .password(passwordEncoder.encode("12345678"))
                            .authority("ADMIN")
                            .build()
            );
            memberRepository.saveAll(members);
        }
    }
}
