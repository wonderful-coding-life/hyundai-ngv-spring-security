package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Collectors;

@Controller
@Slf4j
public class HomeController {
    @GetMapping
    public String getHome(Authentication authentication) {
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
        return "home";
    }

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/logout")
    public String getLogout() {
        return "logout";
    }
}
