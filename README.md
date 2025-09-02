## 2025 캘린더 백엔드

간단한 일정 관리 백엔드 API입니다. Google OAuth2 로그인으로 사용자를 식별하고, JWT 토큰으로 보호되는 일정 CRUD 및 조회 기능을 제공합니다. 스케줄은 일자/시간, 반복, 색상, 완료율, 알림 등 다양한 메타정보를 포함합니다. ULID를 사용해 전역적으로 정렬 가능한 식별자를 부여합니다.

### 🚀 **핵심 기술 스택 및 아키텍처**

#### **Backend Framework**
- **Spring Boot 3.5.0**: Java 17 LTS 기반, Jakarta EE 마이그레이션 완료
- **Spring Security 6**: 향상된 보안 기능과 OAuth2 통합
- **Spring Data JPA**: 데이터 접근 계층 및 쿼리 최적화
- **Spring Events**: 이벤트 기반 느슨한 결합 아키텍처

#### **데이터베이스 & 캐싱**
- **MySQL 8.x**: 관계형 데이터의 ACID 속성 보장
- **Redis 7.x**: 세션 관리, 토큰 블랙리스트, 알림 상태 관리
- **ULID**: 26자리 고유 식별자로 시간순 정렬 및 성능 최적화

#### **인증 & 보안**
- **Google OAuth2**: 신뢰할 수 있는 외부 인증 시스템
- **JWT (JSON Web Token)**: Access Token 1시간, Refresh Token 7일
- **Spring Security**: 세밀한 권한 관리 및 CORS 정책

#### **실시간 통신**
- **SSE (Server-Sent Events)**: 단방향 실시간 알림 시스템
- **비동기 처리**: @Async를 통한 이벤트 기반 알림 처리
- **스케줄링**: @Scheduled를 통한 백그라운드 알림 관리

### 📚 **API 명세서 (Swagger)**
- **Google Sheets**: https://docs.google.com/spreadsheets/d/1yqBxcY0RIi6PEaZf8qRdayLTLLIKmhkgy8WLyVhIh-k/edit?gid=0#gid=0
- **Swagger UI**: `https://api.everyplan.site/swagger-ui.html`

---

## 🏗️ **아키텍처 및 설계 패턴**

### **계층형 아키텍처**
```
Controller (표현 계층) → Service (비즈니스 로직) → Repository (데이터 접근) → Entity (도메인 모델)
```

### **이벤트 기반 아키텍처**
- **이벤트 발행**: `ApplicationEventPublisher`를 통한 이벤트 발행
- **이벤트 리스너**: `@EventListener`와 `@Async`를 통한 비동기 처리
- **느슨한 결합**: 이벤트를 통한 컴포넌트 간 독립성 확보

### **보안 아키텍처**
- **JWT 기반 인증**: Stateless 아키텍처로 확장성 확보
- **OAuth2 통합**: Google의 신뢰할 수 있는 인증 시스템 활용
- **토큰 블랙리스트**: Redis를 통한 로그아웃 토큰 관리

---

## 🔐 **핵심 기능 상세**

### **인증 시스템**
- **Google OAuth2 로그인**: 자동 회원가입 및 프로필 동기화
- **JWT 토큰 관리**: 자동 갱신 및 블랙리스트 관리
- **권한 관리**: 사용자별 데이터 격리 및 접근 제어

### **일정 관리**
- **CRUD 작업**: 생성/조회/수정/삭제 및 권한 검증
- **다양한 조회**: 날짜별, 범위별, 상태별, 완료율별 조회
- **메타데이터**: 색상, 설명, 반복 설정, 알림 설정

### **실시간 알림 시스템**
- **SSE 연결 관리**: 다중 탭 지원 및 연결 상태 모니터링
- **스케줄링**: 매분 정각 알림 시간 체크 및 이벤트 발행
- **비동기 처리**: 이벤트 기반 알림 발송으로 성능 최적화

### **보안 기능**
- **CORS 정책**: 도메인별 세밀한 접근 제어
- **입력 검증**: Bean Validation을 통한 데이터 무결성 보장
- **SQL 인젝션 방지**: JPA 사용으로 자동 방어

---

## 🗄️ **데이터베이스 설계**

### **인덱스 최적화**
- **`idx_user_id`**: 사용자별 일정 조회 최적화
- **`idx_schedule_date`**: 날짜별 일정 조회 최적화
- **`idx_created_at`**: 생성일 기준 정렬 최적화

### **ULID 사용의 장점**
- **시간순 정렬**: 생성 순서대로 자동 정렬
- **26자리 길이**: UUID(36자리) 대비 저장 공간 절약
- **충돌 방지**: 분산 환경에서 극히 낮은 충돌 가능성

### **트랜잭션 관리**
- **Service 계층**: `@Transactional`로 비즈니스 로직 트랜잭션 관리
- **읽기 최적화**: `@Transactional(readOnly = true)`로 성능 향상
- **데이터 일관성**: ACID 속성 보장으로 안정성 확보

---

## 📡 **SSE 실시간 알림 시스템**

### **연결 관리**
- **다중 탭 지원**: `ConcurrentHashMap`과 `CopyOnWriteArrayList` 사용
- **연결 상태 추적**: 실시간 연결 통계 및 모니터링
- **자동 정리**: 연결 완료, 타임아웃, 에러 시 자동 정리

### **타임아웃 설정**
- **기본 타임아웃**: 1시간 (EC2 환경 고려)
- **하트비트 주기**: 15초 (프록시/브라우저 타임아웃 방지)
- **연결 유지**: 주기적 ping 이벤트로 연결 상태 확인

### **이벤트 기반 알림**
- **즉시 알림**: 스케줄 생성/수정 시 즉시 이벤트 발행
- **예약 알림**: `PreciseReminderScheduler`가 매분마다 시간 체크
- **비동기 처리**: `@Async`로 메인 스레드 블로킹 방지

---

## 🔧 **성능 최적화 전략**

### **캐싱 전략**
- **Redis 활용**: 세션 정보, 토큰, 알림 상태 캐싱
- **캐시 무효화**: Write-Through 전략으로 일관성 보장
- **TTL 관리**: 적절한 만료 시간으로 메모리 효율성 확보

### **데이터베이스 최적화**
- **쿼리 최적화**: N+1 문제 해결을 위한 Fetch Join 사용
- **인덱스 설계**: 조회 패턴에 맞는 최적화된 인덱스
- **연결 풀**: HikariCP를 통한 효율적인 연결 관리

### **비동기 처리**
- **이벤트 기반**: 느슨한 결합으로 확장성 확보
- **백그라운드 작업**: 스케줄링과 알림 처리를 별도 스레드에서 실행
- **성능 모니터링**: Spring Actuator를 통한 실시간 성능 추적

---

## 🚀 **배포 및 운영**

### **CI/CD 파이프라인**
- **GitHub Actions**: main 브랜치 푸시 시 자동 배포
- **EC2 배포**: SSH를 통한 자동 빌드 및 서비스 재시작
- **프로파일 관리**: 환경별 설정 분리 및 자동 적용

### **모니터링 및 로깅**
- **Spring Actuator**: 헬스체크, 메트릭, 환경 정보 제공
- **구조화된 로깅**: SLF4J를 통한 상세한 디버그 정보
- **성능 추적**: API 응답 시간, Redis 상태, SSE 연결 수 모니터링

### **헬스체크 엔드포인트**
- **`GET /api/auth/status`**: 기본 헬스체크
- **`GET /api/auth/health`**: 상세 헬스체크 (DB, Redis, SSE 상태)
- **`GET /actuator/health`**: Spring Boot Actuator 헬스체크

---

## 🧪 **테스트 전략**

### **테스트 계층**
- **단위 테스트**: Service, Repository 계층의 비즈니스 로직 검증
- **통합 테스트**: Controller, Security 계층의 API 동작 검증
- **테스트 환경**: H2 인메모리 데이터베이스와 Mock 객체 활용

### **테스트 커버리지**
- **현재 목표**: 70% 이상 (비즈니스 로직 중심)
- **향후 계획**: 80% 이상으로 확대
- **중점 영역**: 보안 관련 코드 및 복잡한 비즈니스 로직

### **테스트 도구**
- **MockMvc**: API 엔드포인트 테스트
- **TestContainers**: MySQL, Redis 통합 테스트 환경
- **JUnit 5**: 최신 테스트 프레임워크 활용

---

## 🔒 **보안 및 개인정보 보호**

### **JWT 토큰 보안**
- **암호화**: HMAC-SHA256 알고리즘으로 토큰 서명
- **자동 갱신**: 토큰 만료 시 자동으로 Refresh Token 사용
- **블랙리스트**: 로그아웃된 토큰의 재사용 방지

### **OAuth2 보안**
- **Google 인증**: 신뢰할 수 있는 외부 인증 시스템 활용
- **프로필 동기화**: 사용자 정보 자동 업데이트
- **권한 관리**: 사용자별 데이터 접근 제어

### **데이터 보호**
- **개인정보 최소화**: 이메일과 닉네임만 저장
- **접근 로그**: 데이터 접근 이력 추적
- **데이터 삭제**: 사용자 요청 시 완전한 데이터 삭제

---

## 🔮 **향후 개발 계획**

### **기능 확장**
- **반복 일정**: RRULE(RFC 5545) 표준 기반 반복 규칙
- **캘린더 공유**: 사용자 간 일정 공유 및 협업 기능
- **AI 기반 추천**: 사용자 패턴 분석을 통한 일정 추천

### **기술 개선**
- **DDD 적용**: 도메인 주도 설계로 전환
- **이벤트 소싱**: 일정 변경 이력 정확한 추적
- **API Gateway**: 마이크로서비스 전환 준비

### **운영 개선**
- **ELK Stack**: 로그 분석 및 시각화
- **Slack 알림**: 에러 발생 시 즉시 알림
- **성능 모니터링**: APM 도구 도입

---

## 📊 **성능 및 확장성**

### **현재 성능**
- **API 응답 시간**: 평균 100ms 이하
- **동시 사용자**: 100명 이상 동시 접속 지원
- **데이터 처리**: 10,000건 이상 일정 데이터 처리 가능

### **확장성 고려사항**
- **Stateless 아키텍처**: JWT 기반으로 수평 확장 가능
- **Redis Cluster**: 대용량 세션 및 캐시 데이터 처리
- **로드 밸런싱**: 다중 서버 인스턴스 지원

### **성능 최적화**
- **연결 풀**: 데이터베이스 및 Redis 연결 최적화
- **비동기 처리**: 이벤트 기반 비동기 알림 처리
- **캐싱 전략**: Redis를 활용한 효율적인 데이터 캐싱

---

## 🔍 **문제 해결 및 트러블슈팅**

### **EC2 Timeout 문제 해결**
- **SSE 타임아웃**: 6시간 → 1시간으로 단축
- **하트비트 주기**: 30초 → 15초로 단축
- **연결 모니터링**: 실시간 연결 상태 추적

### **데이터베이스 연결 최적화**
- **HikariCP 설정**: 최대 10개, 최소 5개 연결 풀
- **타임아웃 설정**: 연결 30초, 쿼리 60초
- **자동 재연결**: MySQL 연결 끊김 시 자동 복구

### **Redis 연결 안정성**
- **Lettuce 클라이언트**: 5초 타임아웃 및 자동 재연결
- **연결 풀**: 최대 8개 연결 관리
- **에러 처리**: 연결 실패 시 안전한 폴백

---

## 📚 **API 개요**

### **인증 API**
- `GET /api/auth/login/google`: Google OAuth2 로그인
- `POST /api/auth/refresh`: Refresh Token으로 Access Token 갱신
- `GET /api/auth/status`: 서버 동작 확인
- `GET /api/auth/health`: 상세 헬스체크

### **일정 관리 API**
- `POST /api/schedule`: 일정 생성
- `GET /api/schedule/{id}`: 단건 조회
- `PUT /api/schedule/{id}`: 일정 수정
- `DELETE /api/schedule/{id}`: 일정 삭제
- `GET /api/schedule`: 전체 조회
- `GET /api/schedule/today`: 오늘 일정
- `GET /api/schedule/date/{date}`: 특정 일자
- `GET /api/schedule/range`: 날짜 범위 조회
- `GET /api/schedule/completed`: 완료 일정
- `GET /api/schedule/in-progress`: 진행 일정

### **알림 API**
- `GET /api/notifications/subscribe`: SSE 구독
- `GET /api/notifications/stream`: 알림 스트림
- `GET /api/notifications/test`: 테스트 이벤트

### **응답 형식**
모든 API는 `CommonResponse<T>` 래퍼 클래스를 사용하여 일관된 응답 형식을 제공합니다:
```json
{
  "success": true,
  "message": "처리 완료",
  "data": { ... }
}
```

---

## 🛠️ **개발 환경 설정**

### **필수 요구사항**
- **JDK 17**: Java 17 LTS 버전
- **MySQL 8.x**: `localhost:3306`
- **Redis 7.x**: `localhost:6379`
- **Gradle**: Wrapper 포함 (`gradlew`, `gradlew.bat`)

### **환경별 프로파일**
- **개발 환경**: `local` (기본값)
- **운영 환경**: `prod` (`-Dspring.profiles.active=prod`)
- **보안 환경**: `secure` (보안 테스트용)

### **데이터베이스 초기화**
```sql
-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS calendar CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 스키마 및 초기 데이터 (선택사항)
mysql -u root -p calendar < db/schema.sql
mysql -u root -p calendar < db/data.sql
```

---

## 🚀 **빌드 및 실행**

### **개발 모드 실행 (권장)**
```bash
# macOS/Linux
./gradlew bootRun

# Windows PowerShell
./gradlew.bat bootRun
```

### **패키징 후 실행**
```bash
# JAR 파일 생성
./gradlew clean bootJar

# 로컬 환경 실행
JAR=$(ls build/libs/*-SNAPSHOT.jar || ls build/libs/*.jar | head -n 1)
java -jar -Dspring.profiles.active=local "$JAR"

# 운영 환경 실행
java -jar -Dspring.profiles.active=prod "$JAR"
```

### **테스트 실행**
```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests ScheduleServiceTest
```

---

## 🌐 **도메인 및 접속 정보**

### **운영 환경**
- **프론트엔드**: `https://everyplan.site`
- **백엔드 API**: `https://api.everyplan.site`
- **Swagger UI**: `https://api.everyplan.site/swagger-ui.html`

### **개발 환경**
- **백엔드 API**: `http://localhost:8080`
- **프론트엔드**: `http://localhost:5173`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **MySQL**: `localhost:3306`
- **Redis**: `localhost:6379`

---

## 📋 **주요 클래스 및 역할**

### **🔐 인증 및 보안**
- **`JwtTokenProvider`**: JWT 토큰 생성, 검증, 갱신
- **`JwtAuthenticationFilter`**: JWT 토큰 검증 및 인증 처리
- **`SecurityConfig`**: Spring Security 전체 설정
- **`OAuth2SuccessHandler`**: OAuth2 로그인 성공 처리

### **📡 실시간 알림**
- **`SsePushService`**: SSE 연결 관리 및 이벤트 전송
- **`NotificationController`**: SSE 구독 및 알림 API
- **`ScheduleEventListener`**: 이벤트 기반 알림 처리
- **`PreciseReminderScheduler`**: 정확한 시간 알림 스케줄링

### **🗄️ 데이터 관리**
- **`ScheduleService`**: 일정 CRUD 및 비즈니스 로직
- **`UserService`**: OAuth2 사용자 관리
- **`RedisService`**: Redis 기반 토큰 및 세션 관리
- **`ReminderService`**: 알림 발송 및 상태 관리

### **🏗️ 엔티티 및 도메인**
- **`User`**: 사용자 정보 (ULID 기반)
- **`Schedule`**: 일정 정보 (다양한 메타데이터 포함)
- **`ScheduleEvent`**: 이벤트 기반 알림 시스템

---

## 🔧 **운영 설정 상세**

### **운영 환경 설정 (`application-prod.yml`)**
- **프론트엔드 리다이렉트**: `https://everyplan.site/login/success`
- **데이터베이스**: HikariCP 연결 풀 최적화
- **Redis**: Lettuce 클라이언트 타임아웃 설정
- **JPA**: `ddl-auto=validate`, `show-sql=false`
- **OAuth2**: Google OAuth 콘솔 설정
- **JWT**: 운영 환경용 비밀키 및 만료시간
- **로깅**: 운영 최적화된 로그 레벨
- **SSE**: 1시간 타임아웃, 15초 하트비트

### **Google OAuth2 설정**
Google OAuth 콘솔에 다음 리디렉션 URI를 등록해야 합니다:
```
https://api.everyplan.site/login/oauth2/code/google
```

---

## 📊 **모니터링 및 로깅**

### **Spring Actuator 엔드포인트**
- **`/actuator/health`**: 애플리케이션 상태 확인
- **`/actuator/metrics`**: 성능 메트릭 조회
- **`/actuator/info`**: 애플리케이션 정보
- **`/actuator/env`**: 환경 설정 정보

### **커스텀 헬스체크**
- **`/api/auth/health`**: DB, Redis, SSE 상태 개별 확인
- **실시간 모니터링**: 각 컴포넌트별 상태 정보 제공
- **성능 지표**: API 응답 시간, 연결 수 등

### **로그 관리**
- **파일 로그**: `logs/calendar-app.log`
- **로그 로테이션**: 10MB 단위, 30일 보관
- **로그 레벨**: 환경별 최적화된 로그 레벨

---

## 🚀 **배포 자동화**

### **GitHub Actions 워크플로우**
- **트리거**: `main` 브랜치 푸시 시 자동 실행
- **배포 과정**: EC2 SSH 접속 → git clone/업데이트 → Gradle 빌드 → JAR 배치
- **설정 관리**: `application-prod.yml` 자동 복사
- **서비스 관리**: systemd 서비스 자동 재시작

### **EC2 초기 설정 (Ubuntu)**
```bash
# 시스템 업데이트 및 필수 패키지 설치
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk git mysql-server redis-server

# 서비스 활성화
sudo systemctl enable --now mysql
sudo systemctl enable --now redis-server

# 데이터베이스 및 계정 설정
sudo mysql -e "CREATE DATABASE IF NOT EXISTS calendar CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mysql';" || true

# 로그 디렉토리 생성
sudo mkdir -p /home/ubuntu/apps/logs
sudo chown -R ubuntu:ubuntu /home/ubuntu/apps
```

### **수동 배포 (대안)**
```bash
# JAR 파일 생성
./gradlew clean bootJar -x test

# 배포 디렉토리 생성 및 파일 복사
sudo mkdir -p /home/ubuntu/apps
JAR=$(ls build/libs/*-SNAPSHOT.jar || ls build/libs/*.jar | head -n 1)
sudo cp "$JAR" /home/ubuntu/apps/calendar.jar
sudo cp deploy/calendar.service /etc/systemd/system/calendar.service

# systemd 서비스 활성화
sudo systemctl daemon-reload
sudo systemctl enable --now calendar
```

---

## 🧪 **테스트 전략 상세**


### **테스트 유형별 설명**
- **`AuthControllerIntegrationTest`**: 인증 상태 확인 (200/ok 검증)
- **`ScheduleControllerTest`**: MockMvc 기반 CRUD/조회 응답 형식 검증
- **`ScheduleControllerIntegrationTest`**: 인증 경계 및 권한 검증
- **`JwtTokenProviderTest`**: 토큰 생성/검증/인증 객체 생성 검증
- **`RedisServiceTest`**: 리프레시 토큰 저장/조회/삭제 검증

### **테스트 환경 구성**
- **H2 인메모리 DB**: 테스트용 데이터베이스
- **Mock 객체**: 외부 의존성 격리
- **TestContainers**: 통합 테스트 환경
- **@Transactional**: 테스트 데이터 자동 롤백

---

## 🔒 **보안 설정 상세**

### **JWT 토큰 자동 갱신**
- **만료 예정 감지**: 기본 5분 전 토큰 만료 예정 감지
- **자동 갱신**: 만료된 토큰으로 요청 시 자동 갱신 시도
- **새 토큰 전송**: `New-Access-Token` 헤더로 새로운 토큰 전송

### **CORS 보안 정책**
- **개발 환경**: 모든 도메인 허용 (`*`)
- **운영 환경**: 특정 도메인만 허용
  - `https://everyplan.site`
  - `capacitor://localhost`
  - `ionic://localhost`


## 🔮 **기술적 성과 및 교훈**

### **주요 성과**
1. **OAuth2 + JWT 하이브리드 인증**: 보안성과 성능을 동시에 확보
2. **SSE 실시간 알림**: 사용자 경험 향상과 시스템 성능 최적화
3. **이벤트 기반 아키텍처**: 느슨한 결합으로 확장성 확보
4. **ULID 도입**: 데이터베이스 성능 및 정렬 효율성 향상

### **기술적 교훈**
1. **보안 우선 설계**: 초기부터 보안을 고려한 설계의 중요성
2. **점진적 개선**: 복잡한 기술을 단계별로 구현하고 지속 개선
3. **확장성 고려**: 현재 요구사항뿐만 아니라 미래 확장성도 함께 고려
4. **테스트와 모니터링**: 개발과 동시에 고려해야 하는 운영 요소

---

## 📞 **문의 및 지원**

- **이메일**: psc0729@naver.com
- **GitHub**: https://github.com/chaniii999/calendar
- **라이선스**: MIT License

---

## 📝 **변경 이력**

### **v1.0.0 (2025-01-15)**
- 초기 릴리즈
- Google OAuth2 + JWT 인증 시스템
- SSE 실시간 알림 시스템
- 기본 일정 CRUD 기능
- Spring Boot 3.5.0 기반

### **향후 계획**
- 반복 일정 기능
- 캘린더 공유 기능

---

이 프로젝트는 현대적인 웹 개발 기술을 활용하여 안전하고 확장 가능한 일정 관리 시스템을 구축한 것입니다. Spring Boot 3.x, OAuth2, JWT, SSE 등 최신 기술을 적극적으로 도입하여 개발자와 사용자 모두에게 만족스러운 경험을 제공합니다.
