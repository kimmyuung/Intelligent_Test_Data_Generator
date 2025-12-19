# ITDG API 문서

> **Intelligent Test Data Generator** API 명세서  
> 버전: 1.0.0 | 마지막 업데이트: 2024-12-19

---

## 📋 목차

1. [개요](#개요)
2. [Orchestrator API](#orchestrator-api)
3. [Analyzer API](#analyzer-api)
4. [Generator API](#generator-api)
5. [ML Server API](#ml-server-api)
6. [에러 코드](#에러-코드)

---

## 개요

### 서비스 포트

| 서비스 | 기본 포트 | 설명 |
|--------|----------|------|
| Orchestrator | 8080 | 워크플로우 조정 |
| Analyzer | 8081 | 스키마 분석 |
| Generator | 8082 | 데이터 생성 |
| ML Server | 8000 | ML 기반 합성 데이터 |

### 공통 응답 형식

```json
{
  "success": true,
  "data": { ... },
  "message": "성공 또는 에러 메시지"
}
```

---

## Orchestrator API

> Base URL: `http://localhost:8080`

### 헬스체크

```http
GET /api/health
```

**응답 (200 OK)**
```json
{
  "status": "UP",
  "service": "orchestrator"
}
```

---

### 스트리밍 데이터 생성

```http
POST /api/orchestrator/stream/generate
Content-Type: application/json
Accept: text/event-stream
```

**요청 본문**
```json
{
  "tables": [
    {
      "tableName": "users",
      "targetRowCount": 100,
      "columns": [
        { "name": "id", "dataType": "INTEGER", "isPrimaryKey": true },
        { "name": "name", "dataType": "VARCHAR" }
      ]
    }
  ],
  "seed": 12345
}
```

**응답 (SSE)**
```
event: tableStart
data: {"tableName": "users"}

event: row
data: {"id": 1, "name": "홍길동"}

event: tableEnd
data: {"tableName": "users", "rowCount": 100}
```

---

### ML 기반 스트리밍 생성

```http
POST /api/orchestrator/stream/generate-ml
Content-Type: application/json
Accept: text/event-stream
```

**요청 본문**
```json
{
  "tables": [
    {
      "tableName": "users",
      "targetRowCount": 100,
      "mlModelId": "model-uuid"
    }
  ]
}
```

---

### 파일 다운로드

#### CSV 다운로드
```http
POST /api/orchestrator/stream/download/csv
Content-Type: application/json
```

#### JSON 다운로드
```http
POST /api/orchestrator/stream/download/json
Content-Type: application/json
```

#### Excel 다운로드
```http
POST /api/orchestrator/stream/download/xlsx
Content-Type: application/json
```

---

## Analyzer API

> Base URL: `http://localhost:8081`

### 헬스체크

```http
GET /api/health
```

---

### DB 스키마 분석

```http
POST /api/analyze
Content-Type: application/json
```

**요청 본문**
```json
{
  "url": "jdbc:postgresql://localhost:5432/mydb",
  "username": "user",
  "password": "password"
}
```

**응답 (200 OK)**
```json
{
  "success": true,
  "data": {
    "databaseName": "mydb",
    "tables": [
      {
        "tableName": "users",
        "columns": [
          { "name": "id", "dataType": "INTEGER", "isPrimaryKey": true }
        ]
      }
    ]
  }
}
```

---

### Git 저장소 분석

```http
POST /api/analyze/git
Content-Type: application/json
```

**요청 본문**
```json
{
  "repoUrl": "https://github.com/user/repo",
  "branch": "main"
}
```

---

### 파일 업로드 분석

```http
POST /api/analyze/upload
Content-Type: multipart/form-data
```

**파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| file | File | SQL/DDL 파일 |

---

## Generator API

> Base URL: `http://localhost:8082`

### 헬스체크

```http
GET /api/health
```

---

### 데이터 생성

```http
POST /api/generator/generate
Content-Type: application/json
```

**요청 본문**
```json
{
  "schema": {
    "tables": [...]
  },
  "rowCount": 100,
  "seed": 12345
}
```

---

### 스트리밍 CSV 생성

```http
POST /api/generator/stream/csv
Content-Type: application/json
Accept: text/event-stream
```

---

### 스트리밍 JSON 생성

```http
POST /api/generator/stream/json
Content-Type: application/json
Accept: text/event-stream
```

---

## ML Server API

> Base URL: `http://localhost:8080/api/ml` (Orchestrator 프록시 경유)  
> 직접 접근: `http://localhost:8000`

### 헬스체크

```http
GET /api/ml/health
```

**응답**
```json
{
  "status": "healthy",
  "sdv_available": true
}
```

---

### 파일 분석

```http
POST /api/ml/analyze
Content-Type: multipart/form-data
```

**파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| file | File | CSV/Excel 파일 |

**응답**
```json
{
  "success": true,
  "fileId": "uuid",
  "columns": ["id", "name", "age"],
  "rowCount": 1000,
  "stats": { ... }
}
```

---

### 모델 학습

```http
POST /api/ml/train?file_id={fileId}&model_type={type}
```

**파라미터**

| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| file_id | string | 필수 | 분석된 파일 ID |
| model_type | string | copula | `copula` 또는 `ctgan` |

**응답**
```json
{
  "success": true,
  "modelId": "model-uuid",
  "modelType": "copula",
  "status": "trained",
  "trainingTime": 2.5
}
```

---

### 합성 데이터 생성

```http
POST /api/ml/generate/{modelId}?num_rows={count}
```

**파라미터**

| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| modelId | string | 필수 | 학습된 모델 ID |
| num_rows | int | 100 | 생성할 행 수 |

**응답**
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "홍길동", "age": 25 }
  ],
  "rowCount": 100,
  "modelType": "copula"
}
```

---

### 모델 정보 조회

```http
GET /api/ml/model/{modelId}
```

**응답**
```json
{
  "modelId": "uuid",
  "exists": true,
  "size": 1024,
  "expiresIn": 3600
}
```

---

### 모델 삭제

```http
DELETE /api/ml/model/{modelId}
```

**응답**
```json
{
  "success": true,
  "message": "Model deleted"
}
```

---

### 다중 테이블 학습

```http
POST /api/ml/multi-table/train
Content-Type: application/json
```

**요청 본문**
```json
{
  "tables": [
    { "name": "users", "data": [...] },
    { "name": "orders", "data": [...] }
  ],
  "relationships": [
    {
      "parent_table": "users",
      "child_table": "orders",
      "parent_key": "id",
      "child_key": "user_id"
    }
  ]
}
```

---

### 다중 테이블 생성

```http
POST /api/ml/multi-table/generate/{modelId}?scale={scale}
```

**파라미터**

| 이름 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| modelId | string | 필수 | 다중 테이블 모델 ID |
| scale | float | 1.0 | 생성 비율 (1.0 = 원본과 동일) |

---

## 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| COMMON_001 | 500 | 서버 내부 오류 |
| COMMON_002 | 400 | 유효하지 않은 입력 |
| DB_001 | 500 | 데이터베이스 연결 실패 |
| SCHEMA_001 | 500 | 스키마 추출 실패 |
| SCHEMA_003 | 400 | 지원되지 않는 프로젝트 타입 |
| GEN_001 | 500 | 데이터 생성 실패 |
| ML_001 | 503 | ML 서버 불가 |
| ML_002 | 404 | ML 모델 없음 |
| ML_003 | 500 | ML 학습 실패 |
| SVC_001 | 503 | 서비스 불가 |
| SVC_002 | 504 | 서비스 타임아웃 |

### 에러 응답 예시

```json
{
  "status": 400,
  "code": "COMMON_002",
  "message": "유효하지 않은 입력입니다.",
  "path": "/api/orchestrator/stream/generate",
  "timestamp": "2024-12-19T20:00:00"
}
```

---

## Swagger UI

Java 백엔드 서비스에 Swagger UI를 활성화하려면:

### 1. 의존성 추가 (`build.gradle`)

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```

### 2. 접속 URL

| 서비스 | Swagger UI |
|--------|------------|
| Orchestrator | http://localhost:8080/swagger-ui.html |
| Analyzer | http://localhost:8081/swagger-ui.html |
| Generator | http://localhost:8082/swagger-ui.html |
| ML Server | http://localhost:8000/docs |

---

*이 문서는 자동 생성되었습니다.*
