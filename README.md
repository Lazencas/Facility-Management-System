# Facility Access Management

## 1. 프로젝트 소개
Facility Access Management 는 시설물과 접근요청을 등록하고, 등록한 시설물에 대한 접근요청을 처리하는 시스템입니다.

## 2. 기술 스택
JAVA Spring/Framework Mybatis MySQL Junit

## 3. 핵심 기능
사용자와 시설 그리고 접근요청에 대한 CRUD 및 로그 기록
<details>
  <summary><strong>API 명세서 펼쳐보기</strong></summary>

  <br>

  | 기능 | 메소드 | URL | 요청 | 응답 | 권한 |
| --- | --- | --- | --- | --- | --- |
| 로그인 | `POST` | `/login` | Body(JSON): `loginId`, `password` | `200 OK` 로그인 성공 / `401 Unauthorized` 로그인 실패 | PUBLIC |
| 로그아웃 | `POST` | `/logout` | 없음 | `200 OK` 로그아웃 성공 | USER / ADMIN |
| 시설 목록 조회 | `GET` | `/facilities` | 없음 | `200 OK` `List<Facility>` | USER / ADMIN |
| 시설 상세 조회 | `GET` | `/facilities/{facilityId}` | Path: `facilityId` | `200 OK` `Facility` / `404 Not Found` | USER / ADMIN |
| 시설 등록 | `POST` | `/admin/facilities` | Body(JSON): `name`, `location`, `description` | `200 OK` 등록된 `Facility` | ADMIN |
| 시설 수정 | `PUT` | `/admin/facilities/{facilityId}` | Path: `facilityId` + Body(JSON): `name`, `location`, `description` | `200 OK` 수정된 `Facility` / `404 Not Found` | ADMIN |
| 시설 비활성화 | `POST` | `/admin/facilities/{facilityId}/deactivate` | Path: `facilityId` | `200 OK` `"시설 비활성화 성공"` / `404 Not Found` | ADMIN |
| 접근 신청 | `POST` | `/access-requests` | Body(JSON): `facilityId`, `requestReason`, `accessStartAt`, `accessEndAt` | `200 OK` 생성된 `AccessRequest` | USER / ADMIN |
| 접근 신청 취소 | `POST` | `/access-requests/{accessRequestId}/cancel` | Path: `accessRequestId` | `200 OK` 취소 성공 | 신청자 본인 |
| 관리자 접근 신청 조회·검색 | `GET` | `/admin/access-requests` | Query: `userId`, `facilityId`, `status`, `from`, `to` (선택) | `200 OK` `List<AccessRequest>` | ADMIN |
| 접근 신청 승인 | `POST` | `/admin/access-requests/{accessRequestId}/approve` | Path: `accessRequestId` | `200 OK` 승인 성공 | ADMIN |
| 접근 신청 반려 | `POST` | `/admin/access-requests/{accessRequestId}/reject` | Path: `accessRequestId` + `rejectReason` | `200 OK` 반려 성공 | ADMIN |
| 시설 접근 시도 | `POST` | `/facilities/{facilityId}/access` | Path: `facilityId` | `200 OK` `ALLOWED` 또는 `DENIED` | USER / ADMIN |
| 접근 로그 조회·검색 | `GET` | `/admin/access-logs` | Query: `userName`, `facilityName`, `result`, `from`, `to` (선택) | `200 OK` `List<AccessLog>` | ADMIN |
</details>

## 4. 아키텍쳐
### 4.1 ERD
<details>
  <summary><strong>ERD 펼쳐보기</strong></summary>

  <br>

  <img width="634" height="573" alt="image" src="https://github.com/user-attachments/assets/88bba476-cd98-44aa-ba21-715c8da95eaa" />

</details>
### 4.2 디렉터리구조

## 5. 시작 가이드
## 시작 가이드

### Requirements

프로젝트 실행을 위해 다음 환경이 필요합니다.

- Java 17+
- MySQL 8.x

### Installation

```bash
git clone [GitHub Repository URL]
cd facility-access-management
```

MySQL에서 `docs/sql/schema.sql`을 실행하여 데이터베이스와 테이블을 생성합니다.

이후 `application.properties`의 DB 접속 정보를 자신의 환경에 맞게 설정합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/[DB_NAME]
spring.datasource.username=[USERNAME]
spring.datasource.password=[PASSWORD]
```

### Run

Windows

```bash
gradlew.bat bootRun
```

Mac / Linux

```bash
./gradlew bootRun
```

정상적으로 실행되면 서버는 기본적으로 아래 주소에서 실행됩니다.

```text
http://localhost:8080
```

> 본 프로젝트는 백엔드 API 중심으로 구현되어 있으며, Postman 등의 API 클라이언트를 통해 기능을 확인할 수 있습니다.

