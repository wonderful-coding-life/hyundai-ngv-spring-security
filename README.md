# Spring Security 기초

## 1. 개발 환경

- Java 21
- IntelliJ
- Postman

## 2. 실습 프로젝트

- starter: 실습 시작 코드
- completion: 완성 코드

## 3. 실습 순서

### (1) Starter 프로젝트 빌드 & 실행

- 웹 화면 점검
- API 호출 점검

> * Controller, RestController
> * JPA (Entity, Repository)
> * 초기 상품 데이터 생성 코드

### (2) 스프링 시큐리티 스타터

#### build.gradle에 starter 추가
```groovy
  implementation 'org.springframework.boot:spring-boot-starter-security'
  testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
```

#### Spring Security Starter Auto Configuration 
- 디폴트 사용자 생성: user, generated password in log
- 웹 화면 접근 제한: 모든 화면은 인증된 사용자만 접근 가능
- /login, /logout URL 및 화면 생성 및 사용자 인증
- 상품 생성, 수정 폼에 csrf 토큰 처리

> * Client 요청 시작
> * Session & SecurityContextHolderFilter
> * CsrfFilter
> * LogoutFilter: POST /logout
> * UsernamePasswordAuthenticationFilter: POST /login
> * DefaultLoginPageGeneratingFilter
> * DefaultLogoutPageGeneratingFilter
> * ExceptionTranslationFilter
> * AuthorizationFilter
> * Controller 도착

- 인증 절차
> * UsernamePasswordAuthenticationFilter
> * AuthenticationManager(ProviderManager)
> * AuthenticationProvider
> * UserDetailsService (Interface)

### (3) 사용자 인증 커스터마이즈

어플리케이션이 관리하는 사용자로 사용자 인증을 하도록 변경

- 회원 엔티티 추가: 회원 이름과 패스워드로 로그인 할 수 있도록 구성하고 권한도 관리
- 회원 리파지토리 추가: 회원 이름으로 조회할 수 있는 메서드 추가
- 초기 데이터 추가: PasswordEncoder 준비하고, 테스트를 위해 일반 사용자, 관리자 추가
- UserDetailsService 구현하여 Bean 객체로 등록

### (4) 시큐리티 필터 체인 커스터마이즈

#### Authorization 설정

requestMatchers()는 URL 패턴별 접근 권한을 설정한다.
```java
permitAll() : 누구나 접근 가능
authenticated() : 인증된 사용자만 접근 가능
hasAuthority("ROLE_ADMIN") : 특정 권한을 가진 사용자만 접근 가능
```
URL 패턴은 Ant 스타일을 사용한다.
```
/product/list : 정확히 일치
/product/* : 한 단계 하위 경로
/product/** : 모든 하위 경로
```
anyRequest()는 앞에서 매칭되지 않은 모든 요청에 적용된다.

#### 로그인, 로그아웃

Spring Security 6 이후 폼 로그인은 디폴트로 비 활성화 되어있고, 로그아웃은 활성화 되어 있다.

이러한 정책은 변경될 수 있으므로 확실하게 코드에서 기본 로그인/로그아웃을 쓸 것인지 커스터마이즈할 것인지 설정하는 것이 좋다.

- 기본 설정 활성화
```
.formLogin(Customizer.withDefaults())
.logout(Customizer.withDefaults())
```
- 커스터마이즈
컨트롤러에 /login, /logout 생성하고 templates/login.html, logout.html 생성
```java
.formLogin(login -> login
        .loginPage("/login")
        .defaultSuccessUrl("/")
        .permitAll()) // 로그인 페이지는 누구나 접근 가능
.logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/")
        .permitAll()) // 로그아웃 요청과 성공 후 이동 URL은 누구나 접근 가능
```

### (5) 인증 정보 접근

#### 컨트롤러
- Authentication 객체를 매개 변수로 주입 받아 사용
    - null이면 인증되지 않은 사용자
    - null이 아니면 name, authorities 접근 가능
- 필요에 따라 Principal 객체(UserDetailsService에서 반환한 객체)도 조회 가능
```java
CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
```

#### 템플릿 엔진(타임리프)에서 사용
- 스프링 시큐리티 문법 지원을 위한 라이브러리 지원
```groovy
  implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
```

- 인증 상태 및 사용자 정보 접근
```html
xmlns:sec="http://www.thymeleaf.org/thymeleaf-extras-springsecurity6"

<!-- 인증 상태 -->
<div sec:authorize="isAuthenticated()">
<div sec:authorize="isAnonymous()">
<div sec:authorize="hasAuthority('ROLE_ADMIN')">
<div sec:authorize="hasRole('ADMIN')">
<div sec:authorize="hasAnyAuthority('ROLE_USER','ROLE_ADMIN')">

<!-- 사용자 정보 -->
<span sec:authentication="name"></span>
<span sec:authentication="principal"></span>
<span sec:authentication="principal.username"></span>
<span sec:authentication="principal.authorities"></span>
```

- 예제 실습
    - home.html: 로그인 여부에 따라 로그인/로그아웃 링크, 관리자 권한이 있는 경우에 상품 추가 링크
    - product-list.html: 관리자 권한이 있는 경우에 상품 수정, 삭제 링크 추가
    - HomeController.java에서 인증된 사용자 정보(Authentication) 매개 변수로 주입

### (6) API 서버 시큐리티 필터 체인 커스터마이즈

RESTful API는 session less 기반 그리고 form 인증 대신 Basic 또는 Jwt Authentication을 사용하도록 시큐리티 필터 체인을 구성해야 한다.

또한 Spring MVC용 시큐리티 필터체인과 분리해서 작성하고 우선 순위를 @Order()로 정의한다.

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
                  .requestMatchers(HttpMethod.POST, "/api/products").hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                  .requestMatchers(HttpMethod.PUT, "/api/products/*").hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                  .requestMatchers(HttpMethod.DELETE, "/api/products/*").hasAnyAuthority("ROLE_ADMIN", "SCOPE_ROLE_ADMIN")
                  .anyRequest().denyAll()
          )
          .formLogin(AbstractHttpConfigurer::disable)
          .httpBasic(Customizer.withDefaults())
          .build();
}
```

> 하나의 어플리케이션에 Spring MVC와 RESTful API를 동시에 구현하는 것은 바람직하지 않다.

### (7) Basic Authentication

사용자 이름(username)과 비밀번호(password)를 `username:password` 형태로 결합한 뒤
Base64로 인코딩하여 HTTP `Authorization` 헤더로 전달하는 인증 방식이다.

```
Authorization: Basic dXNlcjpwYXNzd29yZA==
```

> 참고로 Base64는 **암호화(Encryption)가 아니라 인코딩(Encoding)** 이므로,
> 실무에서는 반드시 HTTPS와 함께 사용합니다.

### (8) CORS (Cross-Origin Resource Sharing)

브라우저가 다른 출처(도메인, 포트, 프로토콜)의 서버에 요청할 수 있도록 허용 여부를 제어하는 보안 정책으로,
Spring Security에서 사용하려면 CORS를 활성화하고 `CorsConfigurationSource` 빈을 등록하여 허용할 출처와 메서드 등을 설정해야 한다.

- CORS 활성화
```java
.cors(Customizer.withDefaults())
```

- CorsConfigurationSource에서 허용할 출처와 메서드 설정
```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
  CorsConfiguration config = new CorsConfiguration();
  config.setAllowedOrigins(List.of("https://www.hyundai-ngv.com:8080")); // "*" for null origin
  config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
  UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/api/**", config);
  return source;
}
```

- curl로 CORS 테스트
```shell
curl -i --request OPTIONS "http://localhost:8080/api/products" ^
  --header "Origin: https://www.hyundai-ngv.com:8080" ^
  --header "Access-Control-Request-Method: GET"
```

- product-list.html로 테스트
  - Application과 같은 Origin에서 다운로드 된 product-list.html에서는 CORS 설정 없이도 성공
  - 브라우저에서 직접 HTML 파일을 열면 오류 발생 --> CORS 활성화 후 "*"를 Origin으로 추가하면 성공

### (9) JWT (Json Web Token)

사용자 정보와 권한 정보를 포함한 토큰으로, 서버가 세션을 저장하지 않고도
사용자를 인증할 수 있는 Stateless 인증 방식이다. `https://www.jwt.io` encoding, decoding 실습

#### 1) JWT 구조
- Header : 토큰 타입(JWT), 서명 알고리즘 정보
- Payload : 사용자 정보(Claims), 권한, 만료 시간 등
- Signature : 토큰 위변조 방지를 위한 전자서명

#### 2) Authorization 헤더 구성
```http request
Authorization: Bearer <JWT>
```

#### 3) RSA 키페어 준비

RSA 알고리즘으로 공개키와 개인키를 PEM 포맷으로 준비한다.
개인키는 JWT를 Signing 할 때 사용하고 공개키는 JWT Signing을 검증할 때 사용한다.
- jwt.io: 키 생성 및 JWT 토큰 생성, 검증 테스트
- JwtTest.java --> generateJwt()

#### 4) OAuth2 Resource Server 의존성 추가

다음과 같이 `build.gradle`에 의존성 추가

```groove
implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
```

시큐리티 필터체인에서 JWT 기반 OAuth2 Resource Server로 동작하도록 설정
```java
.oauth2ResourceServer(oauth2 -> oauth2
        .jwt(Customizer.withDefaults())  // JWT로 인증
)
```

Resource Server 의존성을 추가하고 oauth2ResourceServer() 설정하면 BearerTokenAuthenticationFilter를 필터 체인에 추가하고 다음과 같이 인증 동작을 구성한다.

- BearerTokenAuthenticationFilter를 필터 체인에 추가
- Authorization에 Bearer 토큰이 발견되면 BearerTokenAuthenticationToken을 만들고 AuthenticationManager에게 위임
- AuthenticationManager는 JwtAuthenticationProvider를 선택 인증 요청
- JwtAuthenticationProvider는 JwtDecoder 인터페이스를 지원하는 Bean 객체를 찾아 JWT 검증하고 클레임 추출하여 JwtAuthenticationToken 객체 생성
- AuthenticationManager는 JwtAuthenticationProvider가 생성한 JwtAuthenticationToken(Authentication) 객체를 반환
- BearerTokenAuthenticationFilter는 SecurityContextHolder에 Authentication 저장

Jwt Decoder는 인터페이스 규격으로 Jwt 생성과 짝을 이루어 디코더 생성

- Jwt Decoder 준비
```java
    @Value("${jwt.public.key}")
    RSAPublicKey publicKey;

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
```

애플리케이션에서 JWT 토큰을 생성하기 위해 @PostMapping("/api/tokens") 컨트롤러 메서드 추가하고 다음과 같이 전달된 username, password로 인증한다.
컨트롤러는 .requestMatchers(HttpMethod.POST, "/api/tokens").permitAll()를 사용하여 모두 허용한다.

```java
Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
        )
);
```

인증이 성공하면 Authentication 객체를 JWT 토큰으로 생성하여 클라이언트에 전달한다.

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
return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
```

AuthenticationManager와 JwtEncoder를 생성한다.
AuthenticationManager 객체는 AuthenticationConfiguration 객체에서 꺼내 사용해도 되지만 Bean으로 만들어 두고 재사용하는 것이 좋다.
참고로 SecurityFilterChain에서는 AuthenticationConfiguration 객체에서 꺼내 사용한다.

```java
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Value("${jwt.private.key}")
    RSAPrivateKey privateKey;

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
```
