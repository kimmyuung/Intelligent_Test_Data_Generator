# Analyzer 스키마 분석 로직 구현 및 검증

## 1. 개요
`itdg-analyzer` 서비스에 실제 DB 스키마 정보를 추출하는 로직을 구현하고, 로컬 PostgreSQL 데이터베이스를 대상으로 검증을 수행했습니다.

## 2. 구현 내용

### 공통 모듈 (itdg-common)
- **스키마 DTO 구현**: `com.itdg.common.dto.metadata` 패키지 생성
  - `ColumnMetadata`: 컬럼명, 타입, PK 여부, Null 허용 여부 등
  - `TableMetadata`: 테이블명, 컬럼 목록, PK 목록 등
  - `SchemaMetadata`: 전체 스키마 정보 래퍼

### Analyzer 서비스 (itdg-analyzer)
- **MetadataExtractor**: JDBC `DatabaseMetaData` 인터페이스를 사용하여 DB 메타데이터 추출
  - 테이블 목록 조회
  - 컬럼 상세 정보 조회 (`isNullable`, `isAutoIncrement` 등)
  - Primary Key 정보 매핑
- **SchemaAnalyzerService**: 동적 DB 연결 관리 및 분석 프로세스 제어
  - 요청받은 JDBC URL, Username, Password로 연결 생성
  - 분석 완료 후 연결 종료 (Try-with-resources)
- **AnalyzerController**: REST API 엔드포인트 구현
  - `POST /api/analyze`: 분석 요청 처리

## 3. 검증 방법 및 결과

### 테스트 환경
- **Database**: 로컬 PostgreSQL (localhost:5432/itdg)
- **API Endpoint**: `http://localhost:8082/api/analyze`

### 테스트 수행
PowerShell `Invoke-RestMethod`를 사용하여 JSON 요청 전송:

```powershell
$headers = @{ "Content-Type" = "application/json" }
$body = Get-Content request.json -Raw
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/api/analyze" -Headers $headers -Body $body
```

### 테스트 결과
- **Status**: 성공 (`success: true`)
- **Response Data**:
  ```json
  {
    "success": true,
    "data": {
      "databaseName": "itdg",
      "tables": [],
      "analyzedAt": "2025-12-11T..."
    }
  }
  ```
- **특이사항**:
  - `curl` 명령어 사용 시 윈도우 환경에서 JSON 파싱 오류(`HttpMessageNotReadableException`) 발생
  - `request.json` 파일 생성 및 `Invoke-RestMethod` 사용으로 해결

## 4. 결론
Analyzer 서비스가 외부 DB에 정상적으로 연결하여 메타데이터를 추출하고 API로 반환하는 기능을 성공적으로 검증했습니다.
