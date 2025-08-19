## 2025 캘린더 백엔드

간단한 일정 관리 백엔드 API입니다. Google OAuth2 로그인으로 사용자를 식별하고, JWT 토큰으로 보호되는 일정 CRUD 및 조회 기능을 제공합니다. 스케줄은 일자/시간, 반복, 색상, 완료율, 알림 등 다양한 메타정보를 포함합니다. ULID를 사용해 전역적으로 정렬 가능한 식별자를 부여합니다.

### 핵심 기능
- 인증: Google OAuth2 로그인, Access/Refresh Token 발급 및 갱신(JWT)
- 일정: 생성/단건 조회/수정/삭제, 날짜/범위/상태별 조회, 완료율 업데이트
- 문서화: Swagger UI 제공
- 운영: Actuator 헬스체크, Redis 연동

---

## 빌드 및 실행 방법

### 1) 요구사항(로컬)
- Java 17 (JDK 17)
- MySQL 8.x (로컬: `localhost:3306`)
- Redis 7.x (로컬: `localhost:6379`)
- Gradle Wrapper 동봉(`gradlew`, `gradlew.bat`)

애플리케이션 기본 프로파일은 `local`입니다. 설정은 `src/main/resources/application-local.yml`을 사용합니다.

### 2) 환경 설정
`src/main/resources/application-local.yml` 기본값 요약:
- DB: `jdbc:mysql://localhost:3306/calendar`
  - 사용자: `root`
  - 비밀번호: `mysql`
- Redis: `localhost:6379`
- OAuth2 (Google): client-id/client-secret 필요
- JWT: 비밀키 및 토큰 만료시간 설정

필요 시 위 파일을 로컬 환경에 맞게 수정하세요. 민감정보는 환경변수 또는 외부 설정으로 관리하는 것을 권장합니다.

### 3) 데이터베이스 준비
1. 데이터베이스 생성
   ```sql
   CREATE DATABASE IF NOT EXISTS calendar CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
2. 스키마/기초데이터 반영
   ```bash
   mysql -u root -p calendar < db/schema.sql
   mysql -u root -p calendar < db/data.sql
   ```

### 4) 애플리케이션 실행
개발 모드(권장):
```bash
# macOS/Linux
./gradlew bootRun

# Windows PowerShell
./gradlew.bat bootRun
```

패키징 실행:
```bash
# macOS/Linux
./gradlew clean build
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar

# Windows PowerShell
./gradlew.bat clean build
java -jar build\libs\demo-0.0.1-SNAPSHOT.jar
```

### 5) 확인
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 헬스체크 예시: `GET /api/auth/status` → `{ "ok": true }`

### 6) 테스트
```bash
# macOS/Linux
./gradlew test

# Windows PowerShell
./gradlew.bat test
```

---

## DB 스키마 및 기초데이터 백업파일
- 스키마: `db/schema.sql`
- 기초데이터: `db/data.sql`

엔티티 개요:
- `users`: OAuth2 사용자(비밀번호 없음), 기본 권한 `ROLE_USER`
- `schedules`: 사용자 소유 일정, 상태/반복/완료율/알림/색상 등 포함, 인덱스(사용자, 일자, 생성일)

주의: 애플리케이션은 기본적으로 JPA `ddl-auto=update`이지만, 초기 세팅을 위해 수동으로 스키마를 제공했습니다.

---

## 주력 라이브러리 및 사용 이유
- Spring Boot Web/Data JPA/Security/Validation: 표준적인 REST API, 엔티티 매핑, 인증/인가, 입력 검증을 신속하게 구성하기 위해 사용
- Spring Boot Data Redis: 토큰/세션 유틸 및 캐시/블랙리스트 등 확장 고려
- Spring Security OAuth2 Client: Google OAuth2 로그인 연동 간소화
- JJWT (`io.jsonwebtoken:jjwt-*`): JWT 생성/검증을 안정적으로 처리
- springdoc-openapi: OpenAPI 3 문서 자동 생성 및 Swagger UI 제공
- ULID Creator: 시간순 정렬 가능하고 충돌 확률이 낮은 26자 ULID 사용
- Lombok: 보일러플레이트(게터/세터/빌더) 감소로 코드 가독성 향상
- MySQL Connector/J: MySQL 연동
- Spring Cloud Config Server: 구성 외부화·확장이 용이하도록 채택(필요 시 비활성화 가능)

---

## API 개요
- 인증
  - `GET /api/auth/login/google`: Google 로그인 시작(리다이렉트)
  - `POST /api/auth/refresh`: Refresh Token으로 Access Token 갱신
  - `GET /api/auth/status`: 서버 동작 확인
- 일정
  - `POST /api/schedule`: 생성
  - `GET /api/schedule/{id}`: 단건 조회
  - `PUT /api/schedule/{id}`: 수정
  - `DELETE /api/schedule/{id}`: 삭제
  - `GET /api/schedule`: 전체 조회
  - `GET /api/schedule/today`: 오늘 일정
  - `GET /api/schedule/date/{date}`: 특정 일자
  - `GET /api/schedule/range?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`: 날짜 범위
  - `GET /api/schedule/completed`: 완료 일정
  - `GET /api/schedule/in-progress`: 진행 일정
  - `PUT /api/schedule/{id}/status?status=...`: 상태 변경
  - `PUT /api/schedule/{id}/completion-rate?completionRate=0..100`: 완료율 변경

인증이 필요한 API는 `Authorization: Bearer <ACCESS_TOKEN>` 헤더가 필요합니다.

---

## 기타
- 프론트엔드 리다이렉트 경로는 `application-local.yml`의 `frontend.success-redirect`에서 관리됩니다.
- 실제 운영 환경에서는 OAuth2/DB/JWT/Redis 설정을 반드시 외부화하고, HTTPS 및 CORS 설정을 강화하세요.


