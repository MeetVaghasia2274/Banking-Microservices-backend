# 🏦 Personal Finance Management System — Antigravity Blueprint

> Paste this file into Google Antigravity's Agent Manager as your project brief.
> Everything here is 100% free — no cloud costs, no paid services.

---

## 🎯 Project Goal

Build a **microservices-based Personal Finance Management System** using Java 17, Spring Boot, Redis, Kafka, Docker, and MySQL — all running locally on your machine using Docker Compose.

---

## 🏗️ Architecture Overview

```
Client (Postman)
      │
      ▼
[API Gateway :8080]          ← Spring Cloud Gateway
      │
      ├──▶ [User Service :8081]         ← Auth, JWT, Redis token blacklist
      ├──▶ [Account Service :8082]      ← Accounts, Balance (Redis cache)
      ├──▶ [Transaction Service :8083]  ← Transfers, History → Kafka producer
      └──▶ [Notification Service :8084] ← Kafka consumer, logs alerts

Infrastructure (Docker):
  MySQL      :3306   ← User, Account, Transaction databases
  Redis      :6379   ← Cache + JWT blacklist + Rate limiting
  Kafka      :9092   ← Event streaming
  Zookeeper  :2181   ← Required by Kafka
```

---

## 📦 Services to Build

### 1. User Service (Port 8081)
**Purpose:** Handle registration, login, and logout securely.

Features to implement:
- `POST /api/users/register` — Register with name, email, password
- `POST /api/users/login` — Returns JWT token on success
- `POST /api/users/logout` — Blacklists JWT in Redis
- `GET /api/users/profile` — Returns user info (JWT protected)

Tech used:
- Spring Boot, Spring Security, JWT (jjwt library)
- MySQL (users table)
- Redis (store blacklisted tokens with TTL = token expiry time)

Database table:
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 2. Account Service (Port 8082)
**Purpose:** Manage bank accounts and balances.

Features to implement:
- `POST /api/accounts` — Create a new account for a user
- `GET /api/accounts/{userId}` — Get all accounts (Redis cached)
- `GET /api/accounts/balance/{accountId}` — Get balance (Redis cached, 5 min TTL)
- `PUT /api/accounts/balance/{accountId}` — Update balance (invalidate Redis cache)

Tech used:
- Spring Boot, Spring Data JPA, Hibernate
- MySQL (accounts table)
- Redis (cache-aside pattern for balance)

Database table:
```sql
CREATE TABLE accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  account_number VARCHAR(20) UNIQUE NOT NULL,
  account_type ENUM('SAVINGS','CURRENT') DEFAULT 'SAVINGS',
  balance DECIMAL(15,2) DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Redis caching pattern:
```
Key:   "balance:{accountId}"
Value: balance amount as String
TTL:   300 seconds (5 minutes)
Invalidate on every balance update
```

---

### 3. Transaction Service (Port 8083)
**Purpose:** Handle money transfers and transaction history.

Features to implement:
- `POST /api/transactions/transfer` — Transfer money between accounts
- `GET /api/transactions/{accountId}` — Get transaction history

Tech used:
- Spring Boot, Spring Data JPA
- MySQL (transactions table)
- Kafka Producer (publish event after every transaction)

Kafka event published after transfer:
```json
Topic: "transaction-events"
Payload: {
  "transactionId": "uuid",
  "fromAccount": "ACC001",
  "toAccount": "ACC002",
  "amount": 500.00,
  "type": "TRANSFER",
  "timestamp": "2026-01-01T10:00:00"
}
```

Database table:
```sql
CREATE TABLE transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_account_id BIGINT,
  to_account_id BIGINT,
  amount DECIMAL(15,2) NOT NULL,
  type ENUM('CREDIT','DEBIT','TRANSFER') NOT NULL,
  description VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 4. Notification Service (Port 8084)
**Purpose:** Listen to Kafka events and log/send alerts.

Features to implement:
- Kafka Consumer on topic `transaction-events`
- On receiving event: log the notification (simulate email/SMS)
- `GET /api/notifications/{userId}` — Get all notifications for user

Tech used:
- Spring Boot, Spring Kafka
- MySQL (notifications table)

Database table:
```sql
CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  message TEXT,
  read_status BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 5. API Gateway (Port 8080)
**Purpose:** Single entry point — routes requests to correct service.

Features to implement:
- Route `/api/users/**` → User Service
- Route `/api/accounts/**` → Account Service
- Route `/api/transactions/**` → Transaction Service
- Route `/api/notifications/**` → Notification Service
- Redis-based Rate Limiting: max 5 login attempts per IP per minute

Tech used:
- Spring Cloud Gateway
- Redis (rate limiting filter)

---

## 🐳 docker-compose.yml

Create this file in the root of your project:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: finance-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: financedb
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    container_name: finance-redis
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: finance-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: finance-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

volumes:
  mysql-data:
```

Start all infrastructure with:
```bash
docker-compose up -d
```

---

## 📁 Suggested Folder Structure

```
finance-system/
├── docker-compose.yml
├── api-gateway/
│   ├── src/main/java/...
│   └── pom.xml
├── user-service/
│   ├── src/main/java/...
│   └── pom.xml
├── account-service/
│   ├── src/main/java/...
│   └── pom.xml
├── transaction-service/
│   ├── src/main/java/...
│   └── pom.xml
└── notification-service/
    ├── src/main/java/...
    └── pom.xml
```

Each service is an **independent Spring Boot project** with its own `pom.xml`.

---

## 🧰 Maven Dependencies per Service

### All services need:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
</dependency>
```

### User Service additionally needs:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.11.5</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Transaction Service additionally needs:
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

### API Gateway needs:
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

---

## 🔑 Redis Usage Summary

| Feature | Service | Redis Key Pattern | TTL |
|---|---|---|---|
| JWT Blacklist | User Service | `blacklist:{token}` | Token expiry |
| Balance Cache | Account Service | `balance:{accountId}` | 5 minutes |
| Rate Limiting | API Gateway | `rate:{ip}` | 1 minute |

---

## 🚀 Build Order (Do this sequence)

```
Week 1:  Set up Docker Compose → Start User Service → Login/Register + JWT working
Week 2:  Account Service → MySQL + Redis caching for balance
Week 3:  Transaction Service → Kafka producer → Notification Service consumer
Week 4:  API Gateway → Wire all services → Rate limiting → Test end-to-end in Postman
Week 5:  Write README with architecture diagram → Postman collection → Push to GitHub
```

---

## 📝 Antigravity Prompts to Use

Use these prompts in Antigravity's Agent Manager one by one:

**Prompt 1 — Scaffold project:**
```
Create a Spring Boot 3.x microservice called "user-service" using Java 17 and Maven.
Add dependencies for Spring Web, Spring Data JPA, Spring Security, MySQL, Redis, 
Lombok, and JWT (jjwt 0.11.5). Set up the package structure:
com.finance.userservice with sub-packages: controller, service, repository, model, config, security
```

**Prompt 2 — User registration & login:**
```
In the user-service, create a User entity mapped to a MySQL "users" table with fields:
id, name, email, password, createdAt. Implement UserRepository (JPA), UserService,
and UserController with endpoints POST /api/users/register and POST /api/users/login.
Login should return a JWT token. Use BCrypt for password hashing.
```

**Prompt 3 — JWT blacklisting with Redis:**
```
Add a logout endpoint POST /api/users/logout to user-service. When called with a valid JWT,
store the token in Redis with key "blacklist:{token}" and TTL equal to the token's remaining 
expiry time. Add a filter that rejects any request using a blacklisted token.
```

**Prompt 4 — Account service with Redis caching:**
```
Create a Spring Boot service called "account-service". Add an Account entity with fields:
id, userId, accountNumber, accountType (SAVINGS/CURRENT), balance, createdAt.
Implement GET /api/accounts/balance/{accountId} that first checks Redis cache with key
"balance:{accountId}" (TTL 5 min). If not cached, fetch from MySQL and store in Redis.
Invalidate cache on every balance update.
```

**Prompt 5 — Kafka transaction events:**
```
In transaction-service, after a successful money transfer between two accounts,
publish a Kafka event to topic "transaction-events" with JSON payload containing:
transactionId, fromAccount, toAccount, amount, type, timestamp.
In notification-service, create a Kafka consumer that listens to "transaction-events"
and saves a notification message to MySQL notifications table.
```

---

## 🧪 Testing Your APIs (Postman)

Test in this order after each service is built:

```
1. POST http://localhost:8080/api/users/register
   Body: {"name":"John","email":"john@test.com","password":"pass123"}

2. POST http://localhost:8080/api/users/login
   Body: {"email":"john@test.com","password":"pass123"}
   → Copy the JWT token from response

3. POST http://localhost:8080/api/accounts
   Header: Authorization: Bearer {token}
   Body: {"userId":1,"accountType":"SAVINGS"}

4. GET http://localhost:8080/api/accounts/balance/1
   Header: Authorization: Bearer {token}
   → Check Redis for cached value

5. POST http://localhost:8080/api/transactions/transfer
   Body: {"fromAccountId":1,"toAccountId":2,"amount":500}
   → Check Notification Service logs for Kafka event
```

---

## 🧪 Unit & Integration Tests (Don't Skip This)

Interviewers will ask "did you write tests?" — this section makes sure your answer is yes.

### User Service — Unit Test example
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("John", "john@test.com", "pass123");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);

        String result = userService.register(request);

        assertEquals("User registered successfully", result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("John", "john@test.com", "pass123");

        assertThrows(RuntimeException.class, () -> userService.register(request));
    }
}
```

### Account Service — Unit Test example
```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldReturnCachedBalanceFromRedis() {
        when(redisTemplate.opsForValue().get("balance:1")).thenReturn("5000.00");

        BigDecimal balance = accountService.getBalance(1L);

        assertEquals(new BigDecimal("5000.00"), balance);
        verify(accountRepository, never()).findById(any()); // DB was NOT hit
    }
}
```

### Integration Test example (User Service)
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnJwtOnValidLogin() throws Exception {
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"john@test.com\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
```

### Antigravity prompt for tests:
```
For the user-service, write JUnit 5 unit tests using Mockito for the UserService class.
Cover these cases: successful registration, duplicate email registration, successful login,
wrong password login. Also write one Spring Boot integration test for POST /api/users/login
using MockMvc. Use @ExtendWith(MockitoExtension.class) for unit tests.
```

Add this Maven dependency to each service that needs tests:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 📌 GitHub README must include

- Architecture diagram (use draw.io or Excalidraw — both free)
- Tech stack badge list
- How to run locally (docker-compose up + each service)
- Postman collection link
- What you learned / challenges faced

---

## 💡 Resume Bullet Point (copy this)

> "Built a microservices-based Personal Finance Management System using **Java 17, Spring Boot 3, and Spring Cloud Gateway**. Implemented event-driven transaction processing with **Apache Kafka**, secured APIs with **JWT/OAuth2** with Redis-based token blacklisting, applied **cache-aside pattern** with Redis for account balance, containerized all 5 services using **Docker Compose**, and deployed on **AWS EC2 with RDS**."

---

*Total cost: ₹0. Everything runs locally in Docker. AWS only needed when you choose to deploy (Free Tier covers it).*