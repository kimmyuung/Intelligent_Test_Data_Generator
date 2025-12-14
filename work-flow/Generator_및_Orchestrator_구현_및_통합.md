# Generator 및 Orchestrator 구현 및 통합 검증

## 1. 개요
지능형 테스트 데이터 생성을 위한 핵심 엔진인 **Generator 서비스**를 구현하고, 이를 Analyzer 서비스와 연결하여 통합 제어하는 **Orchestrator 로직**을 완성했습니다. 이를 통해 사용자는 DB 연결 정보만으로 스키마 분석부터 데이터 생성까지의 전 과정을 자동화할 수 있게 되었습니다.

## 2. 주요 구현 내용

### 2.1 Generator 서비스 (`itdg-generator`)
**목표**: 스키마 정보를 기반으로 지능적이고 현실적인 테스트 데이터 생성

*   **Strategy Pattern 적용**: 데이터 생성 로직을 유연하게 확장 가능하도록 설계
    *   `DataGeneratorStrategy` 인터페이스 구현
*   **지능형 패턴 매칭 (Intelligent Generators)**: 컬럼명을 분석하여 문맥에 맞는 데이터 생성
    *   `EmailGenerator`: `user123@example.com`
    *   `PhoneGenerator`: `010-XXXX-XXXX`
    *   `NameGenerator`: 한국어 이름 (`김철수`, `이영희`)
    *   `AddressGenerator`: 한국 주소 (`서울시 강남구...`)
    *   `UrlGenerator`: 웹사이트 URL
*   **기본 타입 생성 (Basic Generators)**: 문자열, 숫자, 날짜/시간, Boolean 등 타입 기반 생성
*   **제약조건 처리**:
    *   **Primary Key**: Auto Increment 및 UUID 지원
    *   **Unique**: 중복 값 방지 로직 (`UniqueValueTracker`)
    *   **Not Null**: 필수 값 보장

### 2.2 공통 모듈 리팩토링 (`itdg-common`)
**목표**: 서비스 간 데이터 교환 효율화 및 중복 제거

*   **DTO 통합 및 이동**:
    *   `DbConnectionRequest`: Analyzer와 Orchestrator가 공유하도록 이동
    *   `OrchestrationRequest`: DB 연결 정보 + 생성 옵션(Row 수, Seed) 통합 요청 객체 생성

### 2.3 Orchestrator 서비스 (`itdg-orchestrator`)
**목표**: 마이크로서비스 간의 워크플로우 조정 및 통합 제어

*   **Service Communication**:
    *   WebClient를 활용한 비동기 서비스 호출
    *   Analyzer (`extractSchema`) -> Generator (`generateData`) 파이프라인 구축
*   **Orchestration Logic**:
    *   단일 API Endpoint (`POST /api/orchestrator/process`) 제공
    *   Reactive Flow (`Mono`, `Flux`)를 이용한 순차적 비동기 처리

## 3. 검증 결과

### 3.1 통합 테스트 시나리오
*   **요청**: 로컬 PostgreSQL DB 연결 정보와 함께 5개의 데이터 생성 요청
*   **흐름 Verification**:
    1.  **Orchestrator**가 요청 수신
    2.  **Analyzer**가 DB에 접속하여 테이블 및 컬럼 메타데이터 추출
    3.  **Generator**가 추출된 메타데이터(컬럼명: `email`, `phone` 등)를 분석하여 패턴 매칭 적용
    4.  **최종 결과**: JSON 형태로 생성된 데이터 반환

### 3.2 결과 확인
*   **성공 여부**: 성공 (`200 OK`)
*   **데이터 품질**:
    *   `email` 컬럼 -> 이메일 형식 데이터 생성 확인
    *   `username` 컬럼 -> 한국어 이름 생성 확인
    *   `id` 컬럼 -> 순차적 PK 생성 확인

## 4. 향후 계획
*   **Frontend 개발**: 사용자가 쉽게 DB 정보를 입력하고 결과를 확인할 수 있는 웹 UI 구현
*   **Data Loader 구현**: 생성된 데이터를 JSON 반환에 그치지 않고 실제 DB에 Insert 하는 기능 추가
*   **대용량 처리**: Batch Processing을 통한 대량 데이터 생성 최적화
