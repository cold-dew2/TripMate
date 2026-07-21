# Backend

Spring Boot 기반 REST API 프로젝트입니다.

Controller, Service, Repository 계층을 분리하여 역할에 맞게 설계하였습니다.

---

# 프로젝트 구조

```text
src/main/java/com/example/backend

├─ global
│  ├─ config
│  ├─ exception
│  └─ security
│
├─ member
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ exception
│  ├─ repository
│  └─ service
│
└─ ...
```

---

# 패키지 설명

## controller

클라이언트 요청을 받아 Service에 전달하고 결과를 응답합니다.

```text
요청 받기
↓
DTO 변환
↓
Service 호출
↓
응답 반환
```

---

## dto

요청(Request) 및 응답(Response) 데이터를 관리합니다.

```text
클라이언트
↓
Request DTO
↓
Service
↓
Response DTO
↓
클라이언트
```

---

## service

비즈니스 로직을 처리합니다.

예시

* 회원가입
* 로그인
* 회원 조회
* 회원 수정
* 상품 등록
* 주문 생성

---

## repository

데이터베이스 접근을 담당합니다.

예시

* 저장
* 조회
* 수정
* 삭제

---

## entity

DB 테이블과 매핑되는 객체를 관리합니다.

```text
Entity
↓
JPA
↓
Database Table
```

---

## exception

도메인별 예외를 관리합니다.

예시

```text
member
 └─ exception

product
 └─ exception

order
 └─ exception
```

---

## global

프로젝트 전역 설정을 관리합니다.

### config

공통 설정

### security

보안 설정

### exception

전역 예외 처리

---

# 요청 처리 흐름

```text
Client

↓

Controller

↓

Request DTO

↓

Service

↓

Repository

↓

Database

↓

Response DTO

↓

Controller

↓

Client
```

---

# 예외 처리 흐름

```text
Service

↓

Domain Exception 발생

↓

GlobalExceptionHandler

↓

ErrorResponse 생성

↓

JSON 응답 반환
```

---

# 계층별 역할

| 계층         | 역할         |
| ---------- | ---------- |
| Controller | 요청 및 응답 처리 |
| DTO        | 데이터 전달     |
| Service    | 비즈니스 로직 처리 |
| Repository | DB 접근      |
| Entity     | 테이블 매핑     |
| Exception  | 예외 관리      |
| Global     | 공통 설정 관리   |

---

# 설계 원칙

* Controller는 요청과 응답만 담당
* Service는 비즈니스 로직 담당
* Repository는 데이터 접근 담당
* DTO와 Entity를 분리
* 도메인별 Exception 분리
* 전역 예외 처리 적용
