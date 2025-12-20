# API 문서화 (Swagger/OpenAPI) 워크플로우

## 개요
SpringDoc OpenAPI를 사용하여 ITDG 프로젝트의 API를 자동 문서화합니다.

---

## 적용 서비스
- `itdg-orchestrator` (메인 API)
- `itdg-analyzer` (분석 API)
- `itdg-generator` (생성 API)

---

## 설정 단계

### 1. 의존성 추가
```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8'
```

### 2. application.yml 설정
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

### 3. OpenAPI 설정 클래스
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ITDG API")
                .version("1.0.0")
                .description("Intelligent Test Data Generator API"));
    }
}
```

---

## 접속 URL
- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/api-docs`

---

## 완료 확인
- [ ] 각 서비스 Swagger UI 접속 가능
- [ ] API 엔드포인트 목록 표시
- [ ] 테스트 요청 실행 가능
