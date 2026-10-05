# Facility Access Management

## 1. 프로젝트 소개
Facility Access Management는 사용자가 특정 시설에 대한 접근을 요청하고,
관리자가 해당 요청을 검토하여 승인 또는 반려한 뒤 실제 접근 가능 여부까지 관리하는 백엔드 시스템입니다.

시설 접근 업무에서는 단순히 요청 데이터를 저장하는 것뿐만 아니라,
현재 요청 상태에 따라 가능한 동작을 제한하고 사용자 권한과 승인 유효기간을 함께 확인해야 합니다.
이를 고려하여 접근 요청 생성, 승인·반려·취소, 접근 가능 여부 판정, 접근 이력 기록까지 하나의 업무 흐름으로 구현했습니다.

단순 CRUD 구현에 그치지 않고 잘못된 상태에서의 요청 재처리 방지, 사용자 권한 검증, 승인 상태에 따른 업무 규칙 처리 등 실제 서비스에서 발생할 수 있는 상황을 비즈니스 로직으로 구현하는 데 중점을 두었습니다.

또한 MyBatis를 활용해 상태, 사용자, 시설, 기간 등 여러 검색 조건을 조합한 조회 기능을 구현하며 Java/Spring 애플리케이션 개발뿐 아니라 SQL 작성과 데이터 조회 로직까지 직접 다루었습니다.

## 2. 기술 스택
JAVA Spring/Framework Mybatis MySQL Junit

## 3. 핵심 기능
- **RESTful API 지향**  
자원과 행위를 분리한 API를 설계하여 RESTful한 API를 지향했습니다.
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

<br>

- **접근 요청 및 승인 관리**  
사용자의 시설 접근 요청을 등록하고, 관리자가 승인·반려할 수 있도록 구현했습니다.
<img width="669" height="184" alt="image" src="https://github.com/user-attachments/assets/ab800c48-b8a4-4599-95f4-268599709a7e" />


- **권한 및 상태 기반 접근 제어**  
사용자 권한, 요청 상태, 승인 유효기간 등을 검증하여 실제 시설 접근 가능 여부를 판단합니다.
<img width="1200" height="766" alt="image" src="https://github.com/user-attachments/assets/716aee6b-82de-4a5a-96db-d72606112978" />



- **MyBatis 기반 조건 검색**  
상태, 사용자, 시설, 기간 등의 조건을 조합하여 접근 요청 및 이력을 조회할 수 있도록 구현했습니다.
<img width="662" height="258" alt="image" src="https://github.com/user-attachments/assets/387a2158-a8e0-45f7-91ba-2ce12cc13b12" />



- **복합 인덱스를 통한 조회 쿼리 성능 개선**  
기존의 쿼리와 비교하여 12배 이상 빨라진 성능개선을 이루었습니다.
<img width="1333" height="740" alt="image" src="https://github.com/user-attachments/assets/a7e216b8-4788-4696-b65c-163d32b3d0fd" />





## 4. 아키텍쳐
### 4.1 ERD
<details>
  <summary><strong>ERD 펼쳐보기</strong></summary>

  <br>

  <img width="634" height="573" alt="image" src="https://github.com/user-attachments/assets/88bba476-cd98-44aa-ba21-715c8da95eaa" />

</details>

### 4.2 디렉터리 구조
<details>
  <summary><strong>디렉터리 구조 펼쳐보기</strong></summary>

  <br>

```text id="9brc58"
facility-access-management
├── src
│   ├── main
│   │   ├── java/com/kgt/facility_access_management
│   │   │   ├── auth                  # 로그인 · 로그아웃 · 세션 인증
│   │   │   │   ├── controller
│   │   │   │   ├── service
│   │   │   │   └── dto
│   │   │   │
│   │   │   ├── user                  # 사용자 정보 및 역할 관리
│   │   │   │   ├── domain
│   │   │   │   └── mapper
│   │   │   │
│   │   │   ├── facility              # 시설 조회 · 등록 · 수정 · 비활성화
│   │   │   │   ├── controller
│   │   │   │   ├── service
│   │   │   │   ├── mapper
│   │   │   │   ├── domain
│   │   │   │   └── dto
│   │   │   │
│   │   │   ├── access                # 접근 신청 · 승인/반려 · 접근 판정 · 로그
│   │   │   │   ├── controller
│   │   │   │   ├── service
│   │   │   │   ├── mapper
│   │   │   │   ├── domain
│   │   │   │   └── dto
│   │   │   │
│   │   │   └── common                # 인증/권한 인터셉터 및 공통 설정
│   │   │       ├── config
│   │   │       └── interceptor
│   │   │
│   │   └── resources
│   │       ├── application.properties # DB · MyBatis 설정
│   │       └── mapper                 # MyBatis SQL Mapper XML
│   │
│   └── test
│       └── java                       # 상태 전이 · 접근 판정 · 검색 테스트
│
├── build.gradle
├── settings.gradle
└── README.md
```

</details>



## 5. 시작 가이드
### Requirements

프로젝트 실행을 위해 다음 환경이 필요합니다.

- Java 17+
- MySQL 8.x

### Installation

```bash
git clone [GitHub Repository URL]
cd facility-access-management
```

MySQL에서 `portfolio-core-test-data.sql`을 실행하여 데이터베이스와 테이블을 생성합니다.

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

