package com.example.demo.config;

import com.example.demo.repository.MemberRepository;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

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
                        // 버전 정보는 누구나 호출 가능
                        .requestMatchers(HttpMethod.GET, "/api/version").permitAll()
                        // 상품 조회 인증된 사용자만 호출 가능
                        .requestMatchers(HttpMethod.GET, "/api/products").authenticated()
                        // 상품 등록/수정/삭제 API는 관리자만 호출 가능
                        // Basic Authentication은 ADMIN 권한을 사용하고,
                        // JWT Authentication은 scope 클레임이 SCOPE_ADMIN 권한으로 변환된다.
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*").hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                        // 사용자 인증 후 JWT Access Token 발급
                        .requestMatchers(HttpMethod.POST, "/api/tokens").permitAll()
                        // 위에서 정의하지 않은 API 요청은 모두 거부
                        .anyRequest().denyAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())  // JWT로 인증
                )
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // 홈 화면은 누구나 가능
                        .requestMatchers("/").permitAll()
                        // 상품 조회는 인증된 사용자만 가능
                        .requestMatchers("/product/list").authenticated()
                        // 상품 등록/수정/삭제 화면은 관리자만 가능
                        .requestMatchers("/product/add").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/product/edit").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/product/delete").hasAuthority("ROLE_ADMIN")
                        // 정적 리소스는 누구나 가능
                        .requestMatchers("/css/**", "/js/**", "/image/**").permitAll()
                        // 그 외 화면은 로그인한 사용자만 가능
                        .anyRequest().authenticated()
                )
                //.formLogin(Customizer.withDefaults())
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/")
                        .permitAll()) // 로그인 페이지는 누구나 접근 가능
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()) // 로그아웃 요청과 성공 후 이동 URL은 누구나 접근 가능
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

    // JWT

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Value("${jwt.public.key}")
    RSAPublicKey publicKey;

    @Value("${jwt.private.key}")
    RSAPrivateKey privateKey;

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}
