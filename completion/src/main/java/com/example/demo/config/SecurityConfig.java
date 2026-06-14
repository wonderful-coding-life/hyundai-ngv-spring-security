package com.example.demo.config;

import com.example.demo.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 상품 조회 API는 누구나 호출 가능
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/*").permitAll()
                        // 상품 등록/수정/삭제 API는 관리자만 호출 가능
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*").hasAuthority("ADMIN")
                        // 위에서 정의하지 않은 API 요청은 모두 거부
                        .anyRequest().denyAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // 상품 조회 화면은 누구나 접근 가능
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/product", "/product/list").permitAll()
                        // 상품 등록/수정/삭제 화면은 관리자만 접근 가능
                        .requestMatchers("/product/add").hasAuthority("ADMIN")
                        .requestMatchers("/product/edit").hasAuthority("ADMIN")
                        .requestMatchers("/product/delete").hasAuthority("ADMIN")
                        // 그 외 화면은 로그인한 사용자만 접근 가능
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(MemberRepository memberRepository) {
        return username -> {
            var member = memberRepository.findByName(username).orElseThrow(() -> new UsernameNotFoundException(username));
            return User.builder()
                    .username(member.getName())
                    .password(member.getPassword())
                    .authorities(member.getAuthority())
                    .build();
        };
    }
}
