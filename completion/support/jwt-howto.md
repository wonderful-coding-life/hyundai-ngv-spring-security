# Spring Security 기반 JWT 예제

## 배경: 왜 JWT가 필요한가?

RESTful API의 인증은 Basic Auth로도 할 수 있지만, 매번 API 호출 시 사용자 이름과 패스워드를 전송해야 하는 보안 위험이 있습니다.

실무에서는 일반적으로 **JWT(JSON Web Token)** 에 사용자 인증 정보를 담아 서명(sign)한 후 클라이언트에 전달합니다.  
이후 클라이언트는 패스워드와 같은 민감한 정보 없이, 이미 서명된 JWT를 요청 헤더에 포함하여 서버에 전달합니다.  
서버는 서명(signature)을 검증한 뒤 JWT에 담긴 사용자 인증 정보를 신뢰하고 사용합니다.

Spring Security는 이를 위해 두 가지 스타터 패키지를 제공합니다.

| 패키지 | 역할 |
|---|---|
| OAuth2 Authorization Server | JWT 토큰 발급 전담 인증 서버 |
| OAuth2 Resource Server | JWT 토큰을 검증하여 리소스 보호 |

여러 시스템이 공통 인증 서버를 사용하는 경우에는 Authorization Server에서 토큰을 발급받아 각 시스템이 Resource Server로 동작합니다.  
**이 예제에서는 별도의 인증 서버 없이 Resource Server 안에서 직접 JWT 토큰을 발급하는 방식을 구현합니다.**

---

## 구현 기능

### `POST /tokens` — 토큰 발급

요청 본문에 사용자 이름과 패스워드를 전달하면 JWT 액세스 토큰을 응답합니다.

**요청 본문**
```json
{
    "username": "user",
    "password": "1234"
}
```

**응답 (JWT 문자열)**
```
eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzZWxmIiwic3ViIjoidXNlciIsImV4cCI6MTc4MDQwNDc3NywiaWF0IjoxNzgwMzY4Nzc3LCJzY29wZSI6IlJPTEVfVVNFUiBGQUNUT1JfUEFTU1dPUkQifQ.IbL4yhxZ...
```

---

### `GET /members` — 내 회원 정보 조회

요청 헤더에 발급받은 JWT 토큰을 Bearer 형식으로 포함하면 현재 로그인한 회원 정보를 JSON으로 응답합니다.

**요청 헤더**
```
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

**응답 예시**
```json
{
    "id": 1,
    "username": "user",
    "authorities": [
        { "authority": "ROLE_USER" }
    ]
}
```

---

## 프로젝트 구성

### 기술 스택

| 항목 | 내용 |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Web | REST API 제공 |
| Spring Security | 인증 / 인가 |
| OAuth2 Resource Server | JWT 기반 리소스 보호 |
| Spring Data JPA | 데이터 접근 계층 |
| H2 Database | 인메모리 데이터베이스 |
| Spring AI (OpenAI) | AI 모델 연동 |
| Lombok | 보일러플레이트 코드 제거 |

### 프로젝트 구조

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── config/
│   ├── SecurityConfig.java       # 시큐리티 설정, JwtEncoder/JwtDecoder 빈 등록
│   └── DataInitializer.java      # 초기 테스트 계정 생성 (user, admin)
├── controller/
│   ├── AuthController.java       # POST /tokens - 토큰 발급
│   └── MemberController.java     # GET /members - 회원 정보 조회
├── dto/
│   └── LoginRequest.java         # 로그인 요청 DTO
├── entity/
│   ├── Member.java               # 회원 엔티티
│   └── MemberAuthority.java      # 권한 엔티티
└── repository/
    ├── MemberRepository.java
    └── MemberAuthorityRepository.java
```

---

## 스프링 시큐리티 설정

### (1) SecurityFilterChain 설정

`SecurityConfig.java`에서 필터 체인을 다음과 같이 구성합니다.

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 미사용
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/tokens").permitAll()  // 토큰 발급은 인증 불필요
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(Customizer.withDefaults())  // JWT로 인증
            )
            .build();
}
```

- **STATELESS** 세션 정책: JWT 기반이므로 서버 측 세션을 사용하지 않습니다.
- **`POST /tokens`** 엔드포인트만 인증 없이 허용하고 나머지는 모두 JWT 인증을 요구합니다.
- `oauth2ResourceServer`에 `jwt()`를 설정하여 요청 헤더의 JWT를 자동으로 검증합니다.

### (2) RSA 키 페어 생성

OAuth2 Resource Server에서 JWT를 서명하고 검증하려면 **RSA 키 페어**가 필요합니다.  
아래 `jwt-keygen.jar`를 사용하여 키를 생성합니다.

```bash
# jwt-keygen.jar 다운로드
# https://github.com/wonderful-coding-life/jwt-keygen/releases/download/v1.0.0/jwt-keygen.jar

# 키 생성 (기본 경로: 사용자 홈 디렉토리/.jwt/app.key, app.pub)
java -jar jwt-keygen.jar
```

생성 결과:
- `~/.jwt/app.key` — RSA 개인키 (서명에 사용)
- `~/.jwt/app.pub` — RSA 공개키 (검증에 사용)

### (3) application.properties 설정

```properties
spring.application.name=demo

spring.h2.console.enabled=true

jwt.private.key=file:${user.home}/.jwt/app.key
jwt.public.key=file:${user.home}/.jwt/app.pub
```

### (4) JwtEncoder / JwtDecoder 빈 등록

```java
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
```

- **`JwtDecoder`**: 요청으로 들어온 JWT 토큰의 서명을 검증하고 클레임을 파싱합니다.
- **`JwtEncoder`**: 사용자 인증 후 JWT 토큰을 생성하고 서명합니다.

---

## 토큰 발급 흐름 (`AuthController`)

```java
@PostMapping("/tokens")
public String token(@RequestBody LoginRequest request) {
    // 1. AuthenticationManager로 사용자 인증 (UserDetailsService + PasswordEncoder 활용)
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
            )
    );

    // 2. 인증된 사용자의 권한 목록을 공백으로 연결하여 scope 클레임 생성
    //    (Spring Security는 scope 클레임의 공백 구분 값을 GrantedAuthority로 변환)
    String scope = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

    // 3. JWT 클레임 구성 (발급자, 발급시각, 만료시각 1시간, 사용자명, 권한 범위)
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))  // 1시간 유효
            .subject(authentication.getName())
            .claim("scope", scope)
            .build();

    // 4. JWT 인코딩 후 토큰 문자열 반환
    return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
}
```

**흐름 요약:**

```
클라이언트                        서버
   |                               |
   |  POST /tokens                 |
   |  { username, password } ----> |
   |                               | AuthenticationManager.authenticate()
   |                               |   → UserDetailsService: DB에서 사용자 조회
   |                               |   → PasswordEncoder: 패스워드 검증
   |                               | JWT 클레임 생성 (sub, scope, exp 등)
   |                               | JwtEncoder로 RSA 서명
   |  <---- JWT 토큰 문자열 반환   |
   |                               |
   |  GET /members                 |
   |  Authorization: Bearer {JWT}  |
   |  --------------------------> |
   |                               | JwtDecoder로 서명 검증
   |                               | SecurityContext에 인증 정보 저장
   |  <---- 회원 정보 JSON 반환   |
```

> **Access Token 만료 시간:** 탈취 위험에 대비하여 짧게 설정하는 것이 권장됩니다.  
> Refresh Token과 함께 사용하는 경우, Refresh Token의 만료 시간을 더 길게 설정합니다.

---

## 초기 테스트 계정

애플리케이션 시작 시 `DataInitializer`가 자동으로 아래 계정을 H2 데이터베이스에 생성합니다.

| 사용자명 | 패스워드 | 권한 |
|---|---|---|
| `user` | `1234` | `ROLE_USER` |
| `admin` | `1234` | `ROLE_USER`, `ROLE_ADMIN` |

---

## 실행 방법

### 1. RSA 키 생성

```bash
java -jar jwt-keygen.jar
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 토큰 발급

```bash
curl -X POST http://localhost:8080/api/tokens \
  -H "Content-Type: application/json" \
  -d '{"username":"seojun","password":"12345678"}'
```

### 4. 회원 정보 조회

```bash
curl -X GET http://localhost:8080/members \
  -H "Authorization: Bearer {발급받은_JWT_토큰}"
```

### 5. H2 콘솔 접속

브라우저에서 `http://localhost:8080/h2-console` 접속 후 데이터베이스 상태를 확인할 수 있습니다.

