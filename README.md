============================================================
## AI 기반 지능형 테스트 데이터 생성기 (Intelligent Test Data Generator)
============================================================

### 🌟 프로젝트 개요 (Project Overview)

이 프로젝트는 기존 데이터베이스의 스키마와 실제 데이터 분포를 **AI/ML 기반으로 분석**하여, **현실성과 일관성**을 갖춘 대량의 테스트 데이터를 **자동으로 생성, 주입 및 내보내기** 하는 지능형 Mocking 시스템입니다.

* **해결하는 문제:** 단순 랜덤 데이터의 낮은 신뢰도와 대용량 테스트 데이터 준비의 비효율성을 해결합니다.
* **핵심 가치:** 실제 서비스 환경과 유사한 데이터를 **대규모**로 제공하며, **MSA 배포 및 대용량 배치 처리** 역량을 입증합니다.

---

### 🏛️ 아키텍처 및 배포 (Architecture & Deployment)

프로젝트는 **Spring Boot 오케스트레이터**와 **Python ML 서버**로 구성된 마이크로서비스 아키텍처를 따르며, **Kubernetes (K8s)**를 통해 관리됩니다. 

#### 1. Spring Boot (Orchestrator)

* **역할:** 전체 프로세스 조정, 대상 DB 메타데이터 추출, AI 서버와 통신, 데이터 주입 및 파일 출력 관리.
* **핵심 기술:**
    * **Spring Batch:** AI가 생성한 대용량 데이터를 청크(Chunk) 단위로 나누어 DB에 **안정적으로 배치 삽입**합니다.
    * **스트리밍 API:** 대용량 파일 출력 시 메모리 부하를 방지하기 위해 **논블로킹 스트리밍(예: `StreamingResponseBody`)**을 적용합니다.

#### 2. Python ML Server (Analyzer & Generator)

* **역할:** 통계 분석, 데이터 분포 학습 및 새로운 데이터 생성.
* **핵심 기술:** **GAN/VAE** 모델을 활용하여 데이터 분포를 모방하고, **FastAPI/Flask**로 Spring Boot와의 REST API 통신을 담당합니다.

#### 3. Kubernetes (Deployment)

* **역할:** 두 마이크로서비스의 배포, 서비스 디스커버리, 로드 밸런싱 및 자동 복구 관리.
* **기술 스택:** **Minikube** (로컬 환경), **Deployment, Service, Ingress**를 사용합니다.

---

### ⚙️ 핵심 기술 스택 (Key Tech Stack)

| 구분 | 기술 스택 | 중요도 및 적용 내용 |
| :--- | :--- | :--- |
| **백엔드 (주 조정자)** | **Spring Boot 3.x** | 핵심 비즈니스 로직 및 API 컨트롤러. |
| **대용량 처리** | **Spring Batch** | 데이터 주입 시 트랜잭션 및 성능 최적화. |
| **파일 I/O** | **Apache POI/Commons CSV** | XLSX 및 CSV 파일 생성 및 스트리밍 처리. |
| **AI/ML** | **Python (FastAPI/Flask), PyTorch/TensorFlow** | AI 모델 서빙 및 데이터 분포 학습. |
| **배포/운영** | **Kubernetes (Minikube)** | MSA 환경의 배포, 확장성 및 관리 자동화. |

---

### 🎯 주요 특징 (Key Features)

1.  **AI 기반 지능형 분포 모방**
    * 기존 데이터의 **통계적 특성** (빈도, 평균, 표준편차)을 학습하여 현실적인 값을 생성합니다.
    * 복잡한 데이터 패턴(예: 시간대별 트래픽 집중)을 반영하여 테스트의 유효성을 높입니다.
2.  **관계형 무결성 보장**
    * DB의 **Foreign Key (외래 키) 관계**를 자동으로 인식하고, 참조 무결성을 유지하며 데이터를 생성하여 일관성을 보장합니다.
3.  **대용량 데이터의 안정적 주입 및 출력**
    * **Spring Batch**를 통한 **DB 배치 삽입**을 지원합니다.
    * 생성된 데이터를 **CSV, XLSX, JSON** 형식으로 **스트리밍 기반**으로 다운로드하여 서버 메모리 부하를 최소화합니다.
4.  **Kubernetes 기반 MSA 확장성**
    * Spring과 Python 서버를 독립적으로 배포 및 관리하며, K8s를 통해 서비스 디스커버리 및 확장성을 확보합니다.

---

### 📝 API 명세 (API Specification)

#### 1. DB 주입 요청 (Injection Request)

```http
POST /api/generate
Content-Type: application/json
요청 후 Spring Batch가 비동기적으로 DB 삽입을 수행합니다.

2. 파일 출력 요청 (Export Request)
생성된 데이터를 DB에 주입하지 않고 파일로 다운로드합니다.

JSON

POST /api/export
Content-Type: application/json

{
  "targetDbId": "target_db_id",
  "tableName": "user",
  "recordCount": 100000,
  "outputFormat": "XLSX",  // 지원 형식: CSV, XLSX, JSON
  "customRules": {
    "email": "unique_corporate_email"
  }
}
응답: Content-Disposition 헤더와 함께 요청된 형식의 파일이 스트리밍됩니다.

🚀 실행 방법 (Execution Steps)
Kubernetes 클러스터 구성: Minikube 또는 대상 K8s 환경을 설정합니다.

이미지 빌드: Spring Boot와 Python ML 서버의 컨테이너 이미지를 빌드합니다.

K8s 배포: k8s/ 디렉토리의 YAML 파일을 사용하여 두 서비스를 클러스터에 배포합니다.

Bash

kubectl apply -f k8s/
API 호출: 배포된 Spring Boot Service의 엔드포인트를 통해 작업을 요청합니다.
