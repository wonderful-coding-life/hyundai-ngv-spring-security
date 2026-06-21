# Spring Security 기초

Spring Security의 기본 인증·인가부터 Form Login, Basic Authentication, CORS, JWT 기반 인증까지 단계별로 학습하는 실습 프로젝트입니다.

## 프로젝트 구성

| 디렉터리 | 설명 |
| --- | --- |
| [`starter`](../../../starter) | 실습 시작 코드 |
| [`completion`](../..) | 전체 실습이 반영된 완성 코드 |

## 개발 환경

- Java 21
- IntelliJ IDEA
- Postman

## 실습 과정

### 1. Starter 프로젝트 빌드 및 실행

프로젝트를 실행한 뒤 웹 화면과 API가 정상적으로 동작하는지 확인합니다.

#### 점검 항목

- 웹 화면 호출
- API 호출
- `Controller`, `RestController` 구성
- JPA `Entity`, `Repository` 구성
- 초기 상품 데이터 생성 코드

### 2. Spring Security Starter 적용

#### 의존성 추가

`build.gradle`에 다음 의존성을 추가합니다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
```

#### 자동 설정 확인

Spring Security Starter를 추가하면 기본적으로 다음 기능이 적용됩니다.

- 기본 사용자 `user` 생성
- 실행 로그에 임시 비밀번호 출력
- 인증된 사용자만 모든 웹 화면에 접근 가능
- `/login`, `/logout` URL과 화면 자동 생성
- 상품 생성·수정 폼에 CSRF 토큰 적용

#### 기본 필터 체인 흐름

1. 클라이언트 요청
2. `SessionManagementFilter` 및 `SecurityContextHolderFilter`
3. `CsrfFilter`
4. `LogoutFilter`: `POST /logout`
5. `UsernamePasswordAuthenticationFilter`: `POST /login`
6. `DefaultLoginPageGeneratingFilter`
7. `DefaultLogoutPageGeneratingFilter`
8. `ExceptionTranslationFilter`
9. `AuthorizationFilter`
10. 컨트롤러 도착

#### 인증 처리 흐름

1. `AuthenticationFilter` (`UsernamePasswordAuthenticationFilter`, `BasicAuthenticationFilter`, `BearerTokenAuthenticationFilter`)
2. `AuthenticationManager`
3. `AuthenticationProvider` (`DaoAuthenticationProvider`, `JwtAuthenticationProvider`)
4. `UserDetailsService`

### 3. 사용자 인증 커스터마이징

애플리케이션에서 관리하는 사용자 정보로 인증하도록 변경합니다.

- 회원 엔티티 추가
  - 사용자 이름과 비밀번호로 로그인
  - 사용자별 권한 관리
- 회원 리포지토리 추가
  - 사용자 이름으로 회원을 조회하는 메서드 구현
- 초기 데이터 추가
  - `PasswordEncoder` 구성
  - 테스트를 위해 일반 사용자와 관리자 생성
- `UserDetailsService` 구현체를 Bean으로 등록

### 4. Security Filter Chain 커스터마이징

#### URL별 접근 권한 설정

`requestMatchers()`를 사용하여 URL 패턴별 접근 권한을 설정합니다.

URL 패턴에는 Ant 스타일 패턴을 사용합니다.

```text
/product/list    정확히 일치
/product/*       한 단계 하위 경로
/product/**      모든 하위 경로
```

```java
permitAll()                       // 누구나 접근 가능
authenticated()                   // 인증된 사용자만 접근 가능
hasAuthority("ROLE_ADMIN")        // 특정 권한을 가진 사용자만 접근 가능
```

`anyRequest()`는 앞의 조건과 일치하지 않은 나머지 모든 요청에 적용됩니다.

#### 로그인 및 로그아웃 설정

Spring Security 6부터 Form Login은 기본적으로 비활성화되며, Logout은 활성화됩니다. 프레임워크의 기본 정책은 변경될 수 있으므로 로그인과 로그아웃의 사용 방식을 코드에 명시하는 것이 좋습니다.

기본 설정을 활성화하려면 다음과 같이 구성합니다.

```java
.formLogin(Customizer.withDefaults())
.logout(Customizer.withDefaults())
```

로그인과 로그아웃을 커스터마이징하려면 컨트롤러에 `/login`, `/logout` 매핑을 추가하고 `templates/login.html`, `templates/logout.html`을 생성합니다.

```java
.formLogin(login -> login
        .loginPage("/login")
        .defaultSuccessUrl("/")
        .permitAll()
)
.logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/")
        .permitAll()
)
```

로그인 페이지, 로그아웃 요청, 로그아웃 성공 후 이동 URL은 누구나 접근할 수 있도록 허용합니다.

### 5. 인증 정보 접근

#### 컨트롤러에서 접근

컨트롤러 메서드의 매개변수로 `Authentication` 객체를 주입받아 사용합니다.

- `null`: 인증되지 않은 사용자
- `null`이 아님: 사용자 이름(`name`)과 권한(`authorities`) 조회 가능
- 필요한 경우 `principal`에서 `UserDetailsService`가 반환한 객체 조회 가능

```java
CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
```

#### Thymeleaf에서 접근

Spring Security 문법을 사용하기 위한 의존성을 추가합니다.

```groovy
implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
```

HTML 문서에 Spring Security 네임스페이스를 선언합니다.

```html
<html xmlns:sec="http://www.thymeleaf.org/thymeleaf-extras-springsecurity6">
```

인증 상태와 권한은 다음과 같이 확인합니다.

```html
<div sec:authorize="isAuthenticated()"></div>
<div sec:authorize="isAnonymous()"></div>
<div sec:authorize="hasAuthority('ROLE_ADMIN')"></div>
<div sec:authorize="hasRole('ADMIN')"></div>
<div sec:authorize="hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')"></div>
```

사용자 정보는 다음과 같이 조회합니다.

```html
<span sec:authentication="name"></span>
<span sec:authentication="principal"></span>
<span sec:authentication="principal.username"></span>
<span sec:authentication="principal.authorities"></span>
```

#### 예제 실습

- `home.html`
  - 로그인 여부에 따른 로그인·로그아웃 링크 표시
  - 관리자에게만 상품 추가 링크 표시
- `product-list.html`
  - 관리자에게만 상품 수정·삭제 링크 표시
- `HomeController.java`
  - `Authentication` 매개변수로 인증된 사용자 정보 조회

### 6. API용 Security Filter Chain 구성

RESTful API는 일반적으로 세션을 사용하지 않는 Stateless 방식으로 구성하며, Form Login 대신 Basic 또는 JWT 인증을 사용합니다.
Spring MVC용 필터 체인과 API용 필터 체인을 분리하고 `@Order`로 우선순위를 지정합니다.
Basic, Token 기반 인증으로 일반적으로 CSRF는 비활성화 합니다.

```java
@Bean
@Order(1)
SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/api/**")
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/version").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/products")
                        .hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/products/*")
                        .hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/*")
                        .hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                    .anyRequest().denyAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .build();
}
```

> 하나의 애플리케이션에 Spring MVC와 RESTful API를 함께 구현하는 구성은 실습을 위한 예제입니다. 실제 서비스에서는 애플리케이션의 책임과 배포 구조를 고려하여 분리 여부를 결정해야 합니다.

### 7. Basic Authentication

사용자 이름과 비밀번호를 `username:password` 형식으로 결합한 뒤 Base64로 인코딩하여 HTTP `Authorization` 헤더로 전달합니다.

```http
Authorization: Basic dXNlcjpwYXNzd29yZA==
```

시큐리티 필터 체인에서 Basic Authentication 활성화
```java
.httpBasic(Customizer.withDefaults())
```

> Base64는 암호화가 아닌 인코딩 방식입니다. 실제 서비스에서는 반드시 HTTPS와 함께 사용해야 합니다.


### 8. JWT

JWT(JSON Web Token)는 사용자 정보와 권한을 포함하는 토큰입니다. 서버가 세션을 저장하지 않고도 사용자를 인증할 수 있어 Stateless 인증에 사용됩니다.

[jwt.io](https://www.jwt.io/)에서 JWT 인코딩과 디코딩을 실습할 수 있습니다.

#### JWT 구조

- Header: 토큰 타입과 서명 알고리즘
- Payload: 사용자 정보(Claims), 권한, 만료 시간
- Signature: 토큰 위·변조를 방지하는 전자서명

#### Authorization 헤더

```http
Authorization: Bearer <JWT>
```

#### RSA 키 쌍 준비

RSA 알고리즘의 공개키와 개인키를 PEM 형식으로 준비합니다.

- 개인키: JWT 서명에 사용
- 공개키: JWT 서명 검증에 사용
- `jwt.io`: 키 생성 및 JWT 생성·검증 테스트
- `JwtTest.java`: `generateJwt()` 테스트 실행

생성한 키는 PEM 포맷으로 다음과 같이 저장하고 application.yaml에 설정
- `~/.jwt/app.key` — RSA 개인키 (서명에 사용)
- `~/.jwt/app.pub` — RSA 공개키 (검증에 사용)
```yaml
jwt:
  private:
    key: file:${user.home}/.jwt/app.key
  public:
    key: file:${user.home}/.jwt/app.pub
```

또 다른 RSA 키페어 생성하는 방법으로 아래 `jwt-keygen.jar`를 사용하여 키를 생성합니다.

```bash
# jwt-keygen.jar 다운로드
# https://github.com/wonderful-coding-life/jwt-keygen/releases/download/v1.0.0/jwt-keygen.jar

# 키 생성 (기본 경로: 사용자 홈 디렉토리/.jwt/app.key, app.pub)
java -jar jwt-keygen.jar
```

생성 결과:
- `~/.jwt/app.key` — RSA 개인키 (서명에 사용)
- `~/.jwt/app.pub` — RSA 공개키 (검증에 사용)

#### OAuth2 Resource Server 의존성 추가

`build.gradle`에 다음 의존성을 추가합니다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
```

Security Filter Chain에 JWT 기반 OAuth2 Resource Server 설정을 추가합니다.

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

#### JWT 인증 처리 흐름

Resource Server 의존성과 `oauth2ResourceServer()` 설정을 추가하면 `BearerTokenAuthenticationFilter`가 필터 체인에 등록됩니다.

1. `Authorization` 헤더에서 Bearer 토큰 탐색
2. `BearerTokenAuthenticationToken` 생성
3. `AuthenticationManager`에 인증 위임
4. `JwtAuthenticationProvider`가 `JwtDecoder`로 JWT 검증 및 Claim 추출
5. `JwtAuthenticationToken` 생성 및 반환
6. `BearerTokenAuthenticationFilter`가 `Authentication`을 `SecurityContextHolder`에 저장

#### JwtDecoder 구성

JWT 생성 시 사용한 키와 짝을 이루는 공개키로 Decoder를 구성합니다.

```java
@Value("${jwt.public.key}")
RSAPublicKey publicKey;

@Bean
JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(publicKey).build();
}
```

#### 토큰 발급 API 구현

`POST /api/tokens` 요청을 처리하는 컨트롤러 메서드를 추가합니다. 토큰 발급 API는 인증 전에도 호출할 수 있어야 합니다.

```java
.requestMatchers(HttpMethod.POST, "/api/tokens").permitAll()
```

전달받은 사용자 이름과 비밀번호로 인증을 요청합니다.

```java
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        )
);
```

인증에 성공하면 `Authentication`의 사용자 이름과 권한으로 JWT를 생성하여 클라이언트에 반환합니다.

```java
String scope = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.joining(" "));

JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .subject(authentication.getName())
        .claim("scope", scope)
        .build();

return this.encoder
        .encode(JwtEncoderParameters.from(claims))
        .getTokenValue();
```

#### AuthenticationManager와 JwtEncoder 구성

`AuthenticationManager`는 `AuthenticationConfiguration`에서 직접 가져올 수도 있지만, 여러 곳에서 재사용할 수 있도록 Bean으로 등록합니다.

```java
@Bean
AuthenticationManager authenticationManager(
        AuthenticationConfiguration configuration
) throws Exception {
    return configuration.getAuthenticationManager();
}

@Value("${jwt.private.key}")
RSAPrivateKey privateKey;

@Bean
JwtEncoder jwtEncoder() {
    JWK jwk = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .build();
    JWKSource<SecurityContext> jwks =
            new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwks);
}
```

### 9. CORS

CORS(Cross-Origin Resource Sharing)는 브라우저가 서로 다른 출처(도메인, 포트 또는 프로토콜)의 서버에 요청할 수 있는지를 제어하는 보안 정책입니다.

Spring Security에서 CORS를 사용하려면 CORS를 활성화하고, `CorsConfigurationSource` Bean에 허용할 출처와 HTTP 메서드 등을 설정합니다.

#### CORS 활성화

```java
.cors(Customizer.withDefaults())
```

#### 허용할 출처와 메서드 설정

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
            List.of("https://www.hyundai-ngv.com:8080")
    );
    config.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
    );

    UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

#### CORS 테스트

Windows 명령 프롬프트에서 다음 명령으로 Preflight 요청을 테스트합니다.

```bat
curl -i --request OPTIONS "http://localhost:8080/api/products" ^
  --header "Origin: https://www.hyundai-ngv.com:8080" ^
  --header "Access-Control-Request-Method: GET"
```

`product-list.html`을 이용한 테스트 결과는 다음과 같습니다.

- 애플리케이션과 같은 Origin에서 내려받은 HTML은 별도의 CORS 설정 없이 요청 성공
- 브라우저에서 HTML 파일을 직접 열면 Origin 차이로 오류 발생
- 허용 Origin을 적절히 추가한 뒤 요청 성공

## 참고 자료

- [`completion/support/jwt-howto.md`](./completion/support/jwt-howto.md)
- [`completion/support/docs`]()
