package com.example.demo;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@SpringBootTest
public class JwtTest {
    @Test
    public void generateJwt() throws NoSuchAlgorithmException, JOSEException, ParseException {
        // 1. RSA 키 쌍 생성
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        System.out.println(toPemPublicKey(publicKey));
        System.out.println(toPemPrivateKey(privateKey));

        // 2. JWT Header 생성
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();

        // 3. JWT Claims 생성
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("self")
                .subject("seojun")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("scope", "ROLE_USER ROLE_ADMIN")
                .build();

        // 4. SignedJWT 생성
        SignedJWT signedJWT = new SignedJWT(header, claims);

        // 5. 개인키로 서명
        JWSSigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);

        // 6. 문자열 토큰으로 변환
        String token = signedJWT.serialize();

        System.out.println("JWT:");
        System.out.println(token);

        // 7. 검증
        SignedJWT parsedJWT = SignedJWT.parse(token);

        JWSVerifier verifier = new RSASSAVerifier(publicKey);

        boolean verified = parsedJWT.verify(verifier);

        System.out.println();
        System.out.println("verified = " + verified);
        System.out.println("subject = " + parsedJWT.getJWTClaimsSet().getSubject());
        System.out.println("scope = " + parsedJWT.getJWTClaimsSet().getStringClaim("scope"));
        System.out.println("expiresAt = " + parsedJWT.getJWTClaimsSet().getExpirationTime());
    }

    @Test
    void generateJwtSpringSecurity() throws Exception {

        // 1. RSA 키 쌍 생성
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        System.out.println(toPemPublicKey(publicKey));
        System.out.println(toPemPrivateKey(privateKey));

        // 2. Encoder 생성
        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();

        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));

        JwtEncoder encoder = new NimbusJwtEncoder(jwks);

        // 3. Decoder 생성
        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        // 4. Claims 생성
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .subject("seojun")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("scope", "ROLE_USER ROLE_ADMIN")
                .build();

        // 5. Header 생성
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();

        // 6. JWT 생성
        Jwt jwt = encoder.encode(
                JwtEncoderParameters.from(headers, claims)
        );

        String token = jwt.getTokenValue();

        System.out.println("JWT:");
        System.out.println(token);

        // 7. JWT 검증 및 디코딩
        Jwt decodedJwt = decoder.decode(token);

        System.out.println();
        System.out.println("verified = true");
        System.out.println("subject = " + decodedJwt.getSubject());
        System.out.println("scope = " + decodedJwt.getClaimAsString("scope"));
        System.out.println("expiresAt = " + decodedJwt.getExpiresAt());
    }

    private String toPemPublicKey(RSAPublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(publicKey.getEncoded()) +
                "\n-----END PUBLIC KEY-----";
    }

    private String toPemPrivateKey(PrivateKey privateKey) {
        return "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(privateKey.getEncoded()) +
                "\n-----END PRIVATE KEY-----";
    }
}
