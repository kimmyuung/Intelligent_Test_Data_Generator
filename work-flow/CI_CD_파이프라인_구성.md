# CI/CD 파이프라인 구성 워크플로우

## 개요
GitHub Actions를 사용하여 ITDG 프로젝트의 CI/CD 파이프라인을 구성합니다.

---

## CI (Continuous Integration)

### 트리거
- `push`: main, develop 브랜치
- `pull_request`: main 브랜치

### 단계
1. **Checkout**: 코드 체크아웃
2. **Setup JDK**: Java 21 설정
3. **Cache**: Gradle 캐시
4. **Build**: 전체 프로젝트 빌드
5. **Test**: 단위 테스트 실행
6. **Upload Report**: 테스트 리포트 업로드

---

## CD (Continuous Deployment)

### 트리거
- `release`: 릴리스 발행 시

### 단계
1. **Build Docker Images**: 각 서비스 이미지 빌드
2. **Push to Registry**: Docker Hub 또는 GitHub Container Registry
3. **Deploy**: (선택) K8s 클러스터 배포

---

## 파일 구조
```
.github/
└── workflows/
    ├── ci.yml      # 빌드 및 테스트
    └── cd.yml      # 배포 (Docker 이미지 빌드)
```

---

## 설정 완료 후 확인 사항
- [ ] GitHub Actions 탭에서 워크플로우 확인
- [ ] PR 생성 시 자동 빌드 확인
- [ ] 테스트 리포트 확인
