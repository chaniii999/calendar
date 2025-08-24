## 2025 캘린더 백엔드

간단한 일정 관리 백엔드 API입니다. Google OAuth2 로그인으로 사용자를 식별하고, JWT 토큰으로 보호되는 일정 CRUD 및 조회 기능을 제공합니다. 스케줄은 일자/시간, 반복, 색상, 완료율, 알림 등 다양한 메타정보를 포함합니다. ULID를 사용해 전역적으로 정렬 가능한 식별자를 부여합니다.

### API 명세서 (Swagger)
https://docs.google.com/spreadsheets/d/1yqBxcY0RIi6PEaZf8qRdayLTLLIKmhkgy8WLyVhIh-k/edit?gid=0#gid=0


### 핵심 기능
- 인증: Google OAuth2 로그인, Access/Refresh Token 발급 및 갱신(JWT)
- 일정: 생성/단건 조회/수정/삭제, 날짜/범위/상태별 조회, 완료율/알림 설정
- 문서화: Swagger UI 제공
- 운영: Actuator 헬스체크, Redis 연동

---

## 도메인
- 프론트엔드: `https://everyplan.site`
- 백엔드(API): `https://api.everyplan.site`
- Swagger UI: `https://api.everyplan.site/swagger-ui.html`

---

## 빌드 및 실행

### 1) 요구사항
- JDK 17 (Java 17)
- MySQL 8.x (`localhost:3306`)
- Redis 7.x (`localhost:6379`)
- Gradle Wrapper (`gradlew`, `gradlew.bat`)

기본 프로파일은 `local`이며 `src/main/resources/application-local.yml`을 사용합니다. 운영은 `-Dspring.profiles.active=prod`로 `application-prod.yml`을 사용합니다.

### 2) 데이터베이스 준비
1. DB 생성
   ```sql
   CREATE DATABASE IF NOT EXISTS calendar CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
2. (선택) 스키마/기초데이터 반영
   ```bash
   mysql -u root -p calendar < db/schema.sql
   mysql -u root -p calendar < db/data.sql
   ```

### 3) 로컬 실행
개발 모드(권장):
```bash
# macOS/Linux
./gradlew bootRun

# Windows PowerShell
./gradlew.bat bootRun
```

패키징 후 실행:
```bash
# macOS/Linux
./gradlew clean bootJar
JAR=$(ls build/libs/*-SNAPSHOT.jar || ls build/libs/*.jar | head -n 1)
java -jar -Dspring.profiles.active=local "$JAR"

# 운영 프로파일로 실행
java -jar -Dspring.profiles.active=prod "$JAR"
```

### 4) 헬스 체크
- `GET /api/auth/status` → `{ "ok": true }`

---

## 운영 설정 요약 (`application-prod.yml`)
- Frontend 성공 리다이렉트: `https://everyplan.site/login/success`
- DB: `jdbc:mysql://localhost:3306/calendar`, 사용자 `root`, 비밀번호 `mysql`
- Redis: `localhost:6379`
- JPA: `ddl-auto=update`, `show-sql=true`
- OAuth2 (Google): redirect-uri `https://api.everyplan.site/login/oauth2/code/{registrationId}`
- JWT: 비밀키 및 만료시간 값이 파일에 기입됨
- 로깅: `org.springframework.security`, `oauth2` 등 DEBUG

Google OAuth 콘솔에 승인된 리디렉션 URI로 `https://api.everyplan.site/login/oauth2/code/google`를 등록하세요.

---

## 배포 (GitHub Actions + EC2 Ubuntu)
워크플로우: `.github/workflows/deploy.yml`
- 트리거: `main` 브랜치 푸시
- EC2에 SSH 접속 → git clone/업데이트 → Gradle 빌드(테스트 제외) → JAR 배치
- `application-prod.yml`을 EC2 `/home/ubuntu/apps/config/application-prod.yml`로 복사
- systemd 서비스 설치/재시작 (`deploy/calendar.service`)

EC2 1회 초기 세팅(Ubuntu):
```bash
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk git mysql-server redis-server
sudo systemctl enable --now mysql
sudo systemctl enable --now redis-server

# (MySQL) DB/계정 예시
sudo mysql -e "CREATE DATABASE IF NOT EXISTS calendar CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mysql';" || true
```

수동 배포/실행(대안):
```bash
./gradlew clean bootJar -x test
sudo mkdir -p /home/ubuntu/apps
JAR=$(ls build/libs/*-SNAPSHOT.jar || ls build/libs/*.jar | head -n 1)
sudo cp "$JAR" /home/ubuntu/apps/calendar.jar
sudo cp deploy/calendar.service /etc/systemd/system/calendar.service
sudo systemctl daemon-reload
sudo systemctl enable --now calendar
```

문제 해결: JDK 미설치로 빌드 실패 시
```bash
sudo apt-get install -y openjdk-17-jdk
./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64 clean bootJar -x test
```

---

## 보안/CORS/문서화
- CORS 허용 오리진: `https://everyplan.site`, 로컬 개발 오리진(`http://localhost:5173` 등)
- Swagger UI: `https://api.everyplan.site/swagger-ui.html`
- OpenAPI 서버: 운영 `https://api.everyplan.site`, 로컬 `http://localhost:8080`

---

## 테스트
실행:
```bash
./gradlew test
```

추가된 테스트 개요:
- `AuthControllerIntegrationTest`: `GET /api/auth/status` 200/ok 검증
- `ScheduleControllerTest`(MockMvc WebMvcTest): 주요 CRUD/조회 응답 형식 검증
- `ScheduleControllerIntegrationTest`: 인증 경계(Authorization 헤더) 확인
- `JwtTokenProviderTest`: 토큰 생성/검증/인증 객체 생성 검증
- `RedisServiceTest`: 리프레시 토큰 저장/조회/삭제 검증

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
