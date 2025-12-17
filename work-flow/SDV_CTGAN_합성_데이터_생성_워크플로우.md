---
description: SDV/CTGAN 기반 합성 데이터 생성 워크플로우 (Python ML Server)
---

# SDV/CTGAN 합성 데이터 생성 워크플로우

이 워크플로우는 SDV(Synthetic Data Vault) 라이브러리를 활용하여 업로드된 샘플 데이터의 패턴을 학습하고, 현실적인 합성 테스트 데이터를 생성하는 과정을 설명합니다.

## 1. 아키텍처 및 기술 스택

- **Module**: `itdg-ml-server`
- **Language**: Python 3.9+
- **Framework**: FastAPI, Uvicorn
- **ML Libraries**: SDV (CTGAN, GaussianCopula), Pandas, NumPy

## 2. 지원 모델

| 모델 | 특징 | 학습 시간 | 권장 사용 |
|:---|:---|:---|:---|
| **GaussianCopula** | 빠른 학습, 가벼움 | 수 초 | 단순 분포, 프로토타이핑 |
| **CTGAN** | 복잡한 분포 모방 | 5-10분 | 정교한 데이터 필요 시 |

## 3. 전체 프로세스

```
┌───────────────┐    ┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  1. 파일 업로드  │ -> │  2. 통계 분석   │ -> │  3. 모델 학습   │ -> │  4. 데이터 생성  │
│   (analyze)    │    │   (stats)     │    │   (train)     │    │  (generate)   │
└───────────────┘    └───────────────┘    └───────────────┘    └───────────────┘
```

## 4. API 명세

### `POST /api/v1/train`
업로드된 샘플 데이터로 SDV 모델을 학습합니다.

- **Query Parameters**:
  - `file_id` (required): 분석된 파일의 UUID
  - `model_type` (optional): `"copula"` (기본값) 또는 `"ctgan"`

- **Response**:
```json
{
  "success": true,
  "modelId": "uuid-string",
  "status": "trained",
  "modelType": "copula",
  "columns": ["name", "age", "salary"],
  "trainingTime": 2.5
}
```

### `POST /api/v1/generate/{model_id}`
학습된 모델로 합성 데이터를 생성합니다.

- **Path Parameter**: `model_id` - 학습된 모델 UUID
- **Query Parameter**: `num_rows` (optional, default: 100)

- **Response**:
```json
{
  "success": true,
  "data": [
    {"name": "Alice", "age": 28, "salary": 55000},
    {"name": "Bob", "age": 34, "salary": 72000}
  ],
  "columns": ["name", "age", "salary"],
  "rowCount": 100
}
```

### `GET /api/v1/model/{model_id}`
모델 상태를 조회합니다.

### `DELETE /api/v1/model/{model_id}`
학습된 모델을 삭제합니다.

## 5. 모델 저장 및 정리

- **저장 경로**: `models/{model_id}.pkl`
- **자동 삭제**: 학습 후 **1시간** 경과 시 자동 삭제
- **명시적 삭제**: `DELETE /api/v1/model/{model_id}` 호출

## 6. 사용 예시 (Frontend → ML Server)

```javascript
// Step 1: 파일 업로드 및 분석
const analyzeRes = await axios.post('/api/v1/analyze', formData);
const fileId = analyzeRes.data.fileId;

// Step 2: 모델 학습
const trainRes = await axios.post(`/api/v1/train?file_id=${fileId}&model_type=copula`);
const modelId = trainRes.data.modelId;

// Step 3: 합성 데이터 생성
const generateRes = await axios.post(`/api/v1/generate/${modelId}?num_rows=1000`);
const syntheticData = generateRes.data.data; // 생성된 데이터 배열
```

## 7. 디렉토리 구조

```
itdg-ml-server/
├── app/
│   ├── api/v1/endpoints/
│   │   ├── analysis.py      # 파일 분석 API
│   │   └── synthesis.py     # 학습/생성 API (신규)
│   ├── services/
│   │   ├── file_manager.py  # 파일/모델 관리
│   │   ├── stat_extractor.py # 통계 추출
│   │   └── synthesizer.py   # SDV 래퍼 (신규)
│   └── main.py
├── models/                   # 학습된 모델 저장 (런타임 생성)
├── temp/                     # 임시 파일 저장
└── requirements.txt          # SDV 의존성 추가
```

## 8. Spring Boot Orchestrator 연동

### 8.1 설정 파일
- `application.yml`: `mlserver.service.url: http://localhost:8000`

### 8.2 추가된 클래스
| 파일 | 설명 |
|:---|:---|
| `WebClientConfig.java` | `mlServerWebClient` Bean 추가 |
| `MlServerDto.java` | Train/Generate 응답 DTO |
| `MlServerService.java` | ML Server API 호출 서비스 |
| `ServiceCommunicationService.java` | ML Server 헬스체크 통합 |

### 8.3 Orchestrator에서 ML 데이터 사용 예시

```java
@Autowired
private MlServerService mlServerService;

// 1. 모델 학습
MlServerDto.TrainResponse trainResult = mlServerService
    .trainModel(fileId, "copula")
    .block();

// 2. 합성 데이터 생성
MlServerDto.GenerateResponse syntheticData = mlServerService
    .generateSyntheticData(trainResult.getModelId(), 1000)
    .block();

// 3. Generator와 결합하여 DB 주입 또는 파일 다운로드
Map<String, List<Object>> columnValues = mlServerService
    .convertToColumnValues(syntheticData);
```
