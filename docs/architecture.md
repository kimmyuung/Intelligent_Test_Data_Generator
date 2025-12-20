# ITDG 시스템 아키텍처

## 1. 개요

**Intelligent Test Data Generator (ITDG)**는 AI/ML 기반의 테스트 데이터 생성 플랫폼입니다.

### 핵심 기능
- Git 리포지토리 분석을 통한 스키마 자동 추출
- 패턴 기반 테스트 데이터 생성
- ML 모델(CTGAN)을 활용한 고품질 데이터 생성

---

## 2. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Frontend (React)                          │
│                         http://localhost:3000                       │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     API Gateway (Spring Cloud)                      │
│                         http://localhost:8888                       │
│    ┌─────────────┬─────────────┬─────────────┬─────────────────┐   │
│    │Rate Limiter │Circuit      │ Routing     │ Load Balancing  │   │
│    │ (Redis)     │Breaker      │             │                 │   │
│    └─────────────┴─────────────┴─────────────┴─────────────────┘   │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  Orchestrator │───▶│   Analyzer    │    │   Generator   │
│   :8080       │    │    :8081      │    │    :8082      │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        │                     ▼                     │
        │            ┌───────────────┐              │
        └───────────▶│  ML Server    │◀─────────────┘
                     │   :5000       │
                     │  (FastAPI)    │
                     └───────────────┘
                              │
                              ▼
                     ┌───────────────┐
                     │  PostgreSQL   │
                     │    :5432      │
                     └───────────────┘
```

---

## 3. 서비스별 역할

| 서비스 | 기술 스택 | 포트 | 역할 |
|--------|----------|:----:|------|
| **Orchestrator** | Spring Boot | 8080 | 워크플로우 조정, 서비스 간 통신 관리 |
| **Analyzer** | Spring Boot | 8081 | DB 스키마 분석, Git 리포지토리 파싱 |
| **Generator** | Spring Boot | 8082 | 패턴 기반 테스트 데이터 생성 |
| **ML Server** | FastAPI (Python) | 5000 | CTGAN 학습/추론, 합성 데이터 생성 |
| **API Gateway** | Spring Cloud Gateway | 8888 | 라우팅, Rate Limiting, Circuit Breaker |
| **Frontend** | React + Vite | 3000 | 사용자 인터페이스 |

---

## 4. 데이터 흐름

### 4.1 Git 리포지토리 분석 흐름
```
Frontend → Orchestrator → Analyzer → (Git Clone) → 파싱 → 스키마 반환
```

### 4.2 데이터 생성 흐름
```
Frontend → Orchestrator → Generator → (패턴/전략 적용) → 데이터 반환
```

### 4.3 ML 학습/추론 흐름
```
Frontend → Orchestrator → ML Server → (CTGAN 학습) → 모델 저장
                        → ML Server → (추론) → 합성 데이터 반환
```

---

## 5. 기술 스택

### Backend
- **Java 21** + **Spring Boot 4.0**
- **Spring WebFlux** (비동기 통신)
- **Spring Cloud Gateway** (API Gateway)
- **PostgreSQL** (운영 DB)
- **H2** (테스트 DB)
- **Redis** (Rate Limiter, 캐싱)

### ML Server
- **Python 3.11** + **FastAPI**
- **CTGAN** (합성 데이터 생성)
- **Pandas** (데이터 처리)

### Frontend
- **React 19** + **Vite**
- **Material-UI** (UI 컴포넌트)
- **Axios** (HTTP 클라이언트)

### DevOps
- **Docker** + **Docker Compose**
- **Kubernetes** (K8s)
- **GitHub Actions** (CI/CD)
- **Prometheus** + **Grafana** (모니터링)

---

## 6. 배포 구조

### 6.1 Docker Compose (개발/테스트)
```bash
docker-compose -f docker-compose.full.yml up -d
```

### 6.2 Kubernetes (운영)
```bash
kubectl apply -f itdg-infra/k8s/
```

---

## 7. API 엔드포인트 (주요)

| 메서드 | 경로 | 서비스 | 설명 |
|--------|------|--------|------|
| POST | `/api/orchestrator/generate` | Orchestrator | 데이터 생성 요청 |
| POST | `/api/analyzer/analyze` | Analyzer | DB 스키마 분석 |
| POST | `/api/analyzer/project/analyze` | Analyzer | Git 프로젝트 분석 |
| POST | `/api/generator/generate` | Generator | 패턴 기반 데이터 생성 |
| POST | `/api/ml/train` | ML Server | CTGAN 모델 학습 |
| POST | `/api/ml/generate` | ML Server | 합성 데이터 생성 |

---

## 8. 모니터링

- **Actuator**: `/actuator/health`, `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **Swagger UI**: `/swagger-ui.html` (각 서비스)

---

## 9. 보안 고려사항

> ⚠️ 현재 개발 단계로 보안 설정 미완료

- [ ] DB 비밀번호 환경변수화
- [ ] Spring Security 적용
- [ ] JWT/OAuth2.0 인증 구현
- [ ] HTTPS 적용
