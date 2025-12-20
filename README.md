# 🤖 AI 기반 지능형 테스트 데이터 생성기
## Intelligent Test Data Generator (ITDG)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.10+-blue.svg)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 프로젝트 개요

이 프로젝트는 기존 데이터베이스의 스키마와 실제 데이터 분포를 **AI/ML 기반으로 분석**하여, **현실성과 일관성**을 갖춘 대량의 테스트 데이터를 **자동으로 생성, 주입 및 내보내기** 하는 지능형 Mocking 시스템입니다.

### 💡 핵심 가치

- **해결하는 문제:** 단순 랜덤 데이터의 낮은 신뢰도와 대용량 테스트 데이터 준비의 비효율성 해결
- **핵심 가치:** 실제 서비스 환경과 유사한 데이터를 **대규모**로 제공하며, **MSA 배포 및 대용량 배치 처리** 역량 입증

---

## 🏗️ 시스템 아키텍처

### 마이크로서비스 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                        itdg-frontend                            │
│                      React (Port: 3000)                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      itdg-api-gateway                           │
│                     Spring Cloud Gateway                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ itdg-orchestrator│ │  itdg-analyzer  │ │  itdg-generator │
│   Port: 8080     │ │   Port: 8081    │ │   Port: 8082    │
│   (조율 서비스)   │ │   (분석 서비스)  │ │   (생성 서비스)  │
└────────┬─────────┘ └────────┬────────┘ └────────┬────────┘
         │                    │                   │
         └────────────────────┼───────────────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │   itdg-ml-server    │
                   │ FastAPI (Port: 8000)│
                   │ (ML 합성 데이터 생성) │
                   └─────────────────────┘
```

### 모듈별 역할

| 모듈 | 포트 | 역할 |
|------|:----:|------|
| **itdg-orchestrator** | 8080 | 전체 프로세스 조율, 서비스 간 통신 관리 |
| **itdg-analyzer** | 8081 | DB 스키마 추출, Git 리포지토리 분석 (JPA Entity/SQL 파싱) |
| **itdg-generator** | 8082 | 패턴 기반 테스트 데이터 생성 |
| **itdg-ml-server** | 8000 | Python FastAPI, SDV/CTGAN 기반 합성 데이터 생성 |
| **itdg-api-gateway** | - | API Gateway (Spring Cloud) |
| **itdg-frontend** | 3000 | React 기반 웹 UI |
| **itdg-common** | - | 공통 DTO, 예외, 유틸리티 라이브러리 |
| **itdg-domain** | - | 도메인 모델 |
| **itdg-infra** | - | 인프라 설정 |

---

## ⚙️ 기술 스택

### Backend
| 구분 | 기술 |
|------|------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 4.0, Spring Cloud 2025.1.0 |
| **Build Tool** | Gradle (Multi-Module) |
| **Communication** | WebClient (Reactive), REST API |

### ML Server
| 구분 | 기술 |
|------|------|
| **Language** | Python 3.10+ |
| **Framework** | FastAPI |
| **ML Library** | SDV (Synthetic Data Vault), CTGAN |

### Frontend
| 구분 | 기술 |
|------|------|
| **Framework** | React.js |
| **Styling** | CSS (Custom) |

### DevOps
| 구분 | 기술 |
|------|------|
| **Containerization** | Docker, Docker Compose |
| **Orchestration** | Kubernetes (Minikube) |

---

## 🎯 주요 기능

### 1. 📊 스키마 분석
- **DB 연결 분석**: JDBC를 통한 데이터베이스 스키마 자동 추출
- **Git 저장소 분석**: JPA Entity 및 SQL DDL 파싱을 통한 스키마 분석
- **프로젝트 타입 감지**: Java, Python, JavaScript 등 자동 인식

### 2. 🎲 패턴 기반 데이터 생성
9가지 스마트 데이터 생성기 내장:

| Generator | 설명 |
|-----------|------|
| `NameGenerator` | 현실적인 한글/영문 이름 생성 |
| `EmailGenerator` | 유효한 이메일 형식 생성 |
| `PhoneGenerator` | 한국 전화번호 형식 생성 |
| `AddressGenerator` | 실제 주소 형식 생성 |
| `DateGenerator` | 날짜/시간 데이터 생성 |
| `NumberGenerator` | 숫자 범위 데이터 생성 |
| `BooleanGenerator` | 불리언 값 생성 |
| `UrlGenerator` | 유효한 URL 형식 생성 |
| `UuidGenerator` | UUID 생성 |

### 3. 🤖 AI/ML 기반 합성 데이터
- **CTGAN**: 기존 데이터의 통계적 분포를 학습하여 새로운 데이터 생성
- **SDV (Synthetic Data Vault)**: 관계형 데이터 무결성 유지
- **분포 모방**: 실제 데이터와 유사한 패턴 재현

### 4. 📦 대용량 처리
- **Spring Batch**: 청크 단위 배치 삽입으로 안정적 데이터 주입
- **스트리밍 출력**: CSV, XLSX, JSON 형식 스트리밍 다운로드
- **메모리 최적화**: 논블로킹 I/O로 대용량 처리

---

## 📂 프로젝트 구조

```
Intelligent_Test_Data_Generator/
├── 📦 itdg-orchestrator/        # 조율 서비스
│   ├── controller/              # REST API 엔드포인트 (5개)
│   ├── service/                 # 비즈니스 로직 (6개)
│   ├── client/                  # 외부 서비스 클라이언트
│   └── exception/               # 예외 처리 (9개)
│
├── 📦 itdg-analyzer/            # 분석 서비스
│   ├── controller/              # 분석 API
│   ├── service/
│   │   └── parser/              # 프로젝트 파서 (15개)
│   └── exception/
│
├── 📦 itdg-generator/           # 생성 서비스
│   ├── controller/              # 생성 API
│   ├── pattern/generators/      # 데이터 생성기 (9개)
│   ├── strategy/                # 생성 전략 (6개)
│   └── constraint/              # 제약조건 처리
│
├── 📦 itdg-ml-server/           # ML 서버 (Python)
│   ├── app/
│   │   ├── api/v1/              # FastAPI 엔드포인트
│   │   └── services/            # ML 서비스 (6개)
│   └── requirements.txt
│
├── 📦 itdg-frontend/            # React 프론트엔드
│   └── src/
│       ├── components/          # UI 컴포넌트 (16개)
│       ├── api/                 # API 클라이언트
│       └── utils/               # 유틸리티
│
├── 📦 itdg-common/              # 공통 라이브러리
│   └── dto/                     # 공통 DTO (12개)
│
├── 📦 itdg-api-gateway/         # API Gateway
├── 📦 itdg-domain/              # 도메인 모델
├── 📦 itdg-infra/               # 인프라 설정
│
├── 📄 docker-compose.yml        # Docker 구성
├── 📄 docker-compose.full.yml   # 전체 서비스 Docker 구성
└── 📄 build.gradle              # Gradle 빌드 설정
```

---

## 🚀 실행 방법

### 사전 요구사항

- Java 21+
- Python 3.10+
- Node.js 18+
- Docker & Docker Compose (선택)

### 1. 저장소 클론

```bash
git clone https://github.com/your-repo/Intelligent_Test_Data_Generator.git
cd Intelligent_Test_Data_Generator
```

### 2. Backend 서비스 실행

```bash
# Gradle 빌드
./gradlew build

# 각 서비스 실행 (별도 터미널에서)
./gradlew :itdg-orchestrator:bootRun
./gradlew :itdg-analyzer:bootRun
./gradlew :itdg-generator:bootRun
```

### 3. ML Server 실행

```bash
cd itdg-ml-server
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. Frontend 실행

```bash
cd itdg-frontend
npm install
npm start
```

### 5. Docker Compose로 전체 실행

```bash
docker-compose -f docker-compose.full.yml up -d
```

---

## 📝 API 명세

### 1. 스키마 분석 요청

```http
POST /api/analyze
Content-Type: application/json

{
  "jdbcUrl": "jdbc:mysql://localhost:3306/mydb",
  "username": "user",
  "password": "password"
}
```

### 2. 데이터 생성 요청

```http
POST /api/generate
Content-Type: application/json

{
  "tableName": "users",
  "recordCount": 1000,
  "columns": [
    { "name": "email", "type": "EMAIL" },
    { "name": "name", "type": "NAME" }
  ]
}
```

### 3. 파일 출력 요청

```http
POST /api/export
Content-Type: application/json

{
  "targetDbId": "target_db_id",
  "tableName": "user",
  "recordCount": 100000,
  "outputFormat": "XLSX"
}
```

**지원 형식**: CSV, XLSX, JSON

---

## 📚 문서

- [API 명세서](docs/api-spec.md)
- [개발 가이드](docs/development-guide.md)
- [아키텍처 문서](docs/architecture.md)

---

## 🔄 워크플로우 문서

| 문서 | 설명 |
|------|------|
| [API 문서 Swagger 명세서](work-flow/API_문서_Swagger_명세서.md) | API 문서화 |
| [Analyzer 스키마 분석 로직](work-flow/Analyzer_스키마_분석_로직_구현_및_검증.md) | 분석 로직 상세 |
| [Generator 및 Orchestrator 구현](work-flow/Generator_및_Orchestrator_구현_및_통합.md) | 생성 및 조율 로직 |
| [SDV CTGAN 합성 데이터 생성](work-flow/SDV_CTGAN_합성_데이터_생성_워크플로우.md) | ML 기반 생성 |
| [멀티 모듈 API 통신 검증](work-flow/멀티_모듈_API_통신_검증.md) | 서비스 간 통신 |

---

## 📄 라이선스

이 프로젝트는 [MIT License](LICENSE) 하에 배포됩니다.

---

## 👥 기여

이슈 및 PR은 언제든 환영합니다!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
