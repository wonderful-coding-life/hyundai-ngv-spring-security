package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtEncoder encoder;

    @PostMapping("/api/tokens")
    public ResponseEntity<String> token(@RequestBody LoginRequest request) {
        try {
            String token = generateToken(request);
            return ResponseEntity.ok(token);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    private String generateToken(LoginRequest request) {
        /*
         * AuthenticationManager를 사용하면 UserDetailsService,
         * PasswordEncoder 등 Spring Security의 인증 기능을
         * 일관되게 활용할 수 있다.
         * 이를 위해 SecurityConfig에서 AuthenticationManager 빈을 등록한다.
         */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        /*
         * Spring Security는 scope 클레임의 공백으로 구분된 값을
         * GrantedAuthority로 변환한다.
         * 따라서 권한 목록을 공백으로 연결하여 scope 클레임을 생성한다.
         */
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        /*
         * Access Token은 탈취에 대비하여 수명을 짧게 설정하는 것이 일반적이다.
         * Refresh Token을 함께 사용하는 경우에는 Refresh Token의 수명을 더 길게 설정한다.
         *
         * scope 클레임에는 공백으로 구분된 권한 목록을 저장한다.
         * Resource Server는 scope 값을 GrantedAuthority로 변환하며,
         * 기본적으로 SCOPE_ 접두사를 추가한다.
         * 예) scope=ADMIN → SCOPE_ADMIN
         */
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();
        return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}