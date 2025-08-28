## 2025 캘린더 백엔드

간단한 일정 관리 백엔드 API입니다. Google OAuth2 로그인으로 사용자를 식별하고, JWT 토큰으로 보호되는 일정 CRUD 및 조회 기능을 제공합니다. 스케줄은 일자/시간, 반복, 색상, 완료율, 알림 등 다양한 메타정보를 포함합니다. ULID를 사용해 전역적으로 정렬 가능한 식별자를 부여합니다.

### API 명세서 (Swagger)
https://docs.google.com/spreadsheets/d/1yqBxcY0RIi6PEaZf8qRdayLTLLIKmhkgy8WLyVhIh-k/edit?gid=0#gid=0


### 핵심 기능
- **인증**: Google OAuth2 로그인, Access/Refresh Token 발급 및 자동 갱신(JWT)
- **일정**: 생성/단건 조회/수정/삭제, 날짜/범위/상태별 조회, 완료율/알림 설정
- **보안**: JWT 토큰 자동 갱신, Redis 기반 세션 관리
- **문서화**: Swagger UI 제공
- **운영**: Actuator 헬스체크, Redis 연동, SSE 실시간 알림

---

## 도메인
- **프론트엔드**: `https://everyplan.site`
- **백엔드(API)**: `https://api.everyplan.site`
- **Swagger UI**: `https://api.everyplan.site/swagger-ui.html`

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
- `GET /api/auth/status` → `{ "ok": true }` (기본 헬스체크)
- `GET /api/auth/health` → 상세 헬스체크 (DB, Redis, SSE 상태 포함)
- `GET /actuator/health` → Spring Boot Actuator 헬스체크

---

## 운영 설정 요약 (`application-prod.yml`)
- **Frontend 성공 리다이렉트**: `https://everyplan.site/login/success`
- **DB**: `jdbc:mysql://localhost:3306/calendar` (HikariCP 연결 풀 설정 포함)
- **Redis**: `localhost:6379` (Lettuce 클라이언트 타임아웃 설정 포함)
- **JPA**: `ddl-auto=validate`, `show-sql=false` (보안 강화)
- **OAuth2 (Google)**: redirect-uri `https://api.everyplan.site/login/oauth2/code/{registrationId}`
- **JWT**: 비밀키 및 만료시간 값이 파일에 기입됨
- **로깅**: `org.springframework.security`, `oauth2` 등 INFO 레벨
- **SSE**: 1시간 타임아웃, 15초 하트비트 주기
- **서버**: Tomcat 스레드 풀 및 연결 타임아웃 설정

Google OAuth 콘솔에 승인된 리디렉션 URI로 `https://api.everyplan.site/login/oauth2/code/google`를 등록하세요.

---

## EC2 Timeout 문제 해결

### 🔧 **주요 개선사항**

#### 1. **SSE (Server-Sent Events) 개선**
- **타임아웃 단축**: 6시간 → 1시간 (프록시/로드밸런서 타임아웃 고려)
- **하트비트 주기 단축**: 30초 → 15초 (연결 유지 강화)
- **에러 처리 개선**: 연결 끊김 시 안전한 정리 로직 추가
- **연결 상태 모니터링**: 실시간 연결 통계 제공

#### 2. **데이터베이스 연결 개선**
- **HikariCP 연결 풀 설정**: 최대 10개 연결, 최소 5개 유휴 연결
- **연결 타임아웃**: 30초 연결 타임아웃, 60초 쿼리 타임아웃
- **자동 재연결**: MySQL 연결 끊김 시 자동 재연결
- **연결 유효성 검사**: `SELECT 1` 쿼리로 연결 상태 확인

#### 3. **Redis 연결 개선**
- **Lettuce 클라이언트 설정**: 5초 타임아웃, 자동 재연결
- **연결 풀 설정**: 최대 8개 연결 관리
- **명령어 타임아웃**: Redis 명령어별 5초 타임아웃
- **종료 타임아웃**: 안전한 종료를 위한 5초 타임아웃

#### 4. **스케줄 작업 개선**
- **작업 타임아웃**: 알림 체크 30초, 상태 초기화 60초 타임아웃
- **에러 처리**: 개별 작업 실패 시 전체 작업 중단 방지
- **성능 모니터링**: 작업 실행 시간 추적 및 경고

#### 5. **시스템 레벨 개선**
- **JVM 메모리 설정**: 최소 512MB, 최대 1GB 힙 메모리
- **GC 최적화**: G1GC 사용, 최대 200ms 일시정지 목표
- **파일 디스크립터 제한**: 65,536개로 증가
- **프로세스 제한**: 4,096개로 증가

#### 6. **헬스체크 강화**
- **상세 헬스체크**: DB, Redis, SSE 상태 개별 확인
- **Spring Boot Actuator**: 표준 헬스체크 엔드포인트 제공
- **실시간 모니터링**: 각 컴포넌트별 상태 정보 제공

### 📊 **모니터링 엔드포인트**
- `GET /api/auth/health` - 상세 헬스체크
- `GET /actuator/health` - Spring Boot Actuator 헬스체크
- `GET /actuator/metrics` - 애플리케이션 메트릭
- `GET /actuator/info` - 애플리케이션 정보

### 🔍 **로그 관리**
- **파일 로그**: `logs/calendar-app.log`
- **로그 로테이션**: 10MB 단위, 30일 보관
- **로그 레벨**: 운영 환경 최적화된 로그 레벨 설정

---

## 보안 설정

### JWT 토큰 자동 갱신
- 액세스 토큰이 곧 만료될 때 자동으로 갱신
- 만료된 토큰으로 요청 시 자동 갱신 시도
- 새로운 토큰은 `New-Access-Token` 헤더로 전송

### CORS 보안
- **개발 환경**: 모든 도메인 허용 (`*`)
- **프로덕션 환경**: 특정 도메인만 허용
  - `https://everyplan.site`
  - `https://www.everyplan.site`
  - `capacitor://localhost`
  - `ionic://localhost`

### 환경 변수 지원
프로덕션 환경에서 환경 변수로 설정 가능:
- `FRONTEND_SUCCESS_REDIRECT`
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`

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

# 로그 디렉토리 생성
sudo mkdir -p /home/ubuntu/apps/logs
sudo chown -R ubuntu:ubuntu /home/ubuntu/apps
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
### 인증
- `GET /api/auth/login/google`: Google 로그인 시작(리다이렉트)
- `POST /api/auth/refresh`: Refresh Token으로 Access Token 갱신
- `GET /api/auth/status`: 서버 동작 확인
- `GET /api/auth/health`: 상세 헬스체크

### 일정
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

### 알림
- `GET /api/notifications/subscribe-public`: 공개 알림 구독
- `GET /api/notifications/stream`: 알림 스트림

인증이 필요한 API는 `Authorization: Bearer <ACCESS_TOKEN>` 헤더가 필요합니다.

---

## 개발 환경 접속 정보
- **백엔드 API**: `http://localhost:8080`
- **프론트엔드**: `http://localhost:5173`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **MySQL**: `localhost:3306`
- **Redis**: `localhost:6379`
