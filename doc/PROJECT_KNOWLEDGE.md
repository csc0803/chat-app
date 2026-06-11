# PROJECT_KNOWLEDGE.md — 即時聊天室

> 多人即時聊天室的技術規範與踩坑筆記。實作時請以此檔為準；任務拆解見 `TASKS.md`。

## 目錄

1. [專案概述](#專案概述)
2. [技術棧](#技術棧)
3. [專案結構](#專案結構)
4. [資料庫 Schema](#資料庫-schema)
5. [Entity 設計](#entity-設計)
6. [Repository 層](#repository-層)
7. [Spring Security 設計](#spring-security-設計)
8. [WebSocket 設計](#websocket-設計)
9. [REST API 端點](#rest-api-端點)
10. [Redis 用途](#redis-用途)
11. [部署架構（Docker / Nginx）](#部署架構docker--nginx)
12. [環境變數](#環境變數)
13. [測試設定（@DataJpaTest + H2）](#測試設定datajpatest--h2)
14. [開發順序建議](#開發順序建議)
15. [注意事項](#注意事項)

---

## 專案概述

多人即時聊天室，支援多房間、歷史訊息查詢、用戶在線狀態顯示。

---

## 技術棧

| 層級 | 技術 | 版本建議 |
|------|------|----------|
| Frontend | React + Vite | React 18, Vite 5 |
| WebSocket Client | STOMP.js + SockJS | stomp.js 7, sockjs-client 1.6 |
| Backend | Spring Boot | 3.x (Java 17+) |
| WebSocket Server | Spring WebSocket + STOMP | 內建於 Spring Boot |
| Security | Spring Security 6 + JWT (jjwt 0.11.5) | 內建於 Spring Boot 3.x |
| ORM | Spring Data JPA + Hibernate | 內建於 Spring Boot |
| 資料庫 | MySQL | 8.0 |
| Cache / 狀態 | Redis | 7.x |
| 反向代理 | Nginx | latest stable |
| 容器化 | Docker + Docker Compose | Docker 24+ |

---

## 專案結構

```
chat-app/
├── docker-compose.yml
├── nginx/
│   └── nginx.conf
├── frontend/
│   ├── Dockerfile
│   ├── index.html
│   ├── vite.config.js
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── components/
│       │   ├── ChatRoom.jsx       # 主聊天畫面
│       │   ├── MessageList.jsx    # 訊息列表
│       │   ├── MessageInput.jsx   # 輸入框
│       │   ├── RoomList.jsx       # 房間列表
│       │   └── UserList.jsx       # 在線用戶列表
│       ├── hooks/
│       │   ├── useWebSocket.js    # WebSocket 連線管理
│       │   └── useAuth.js         # 登入狀態管理
│       └── api/
│           └── index.js           # REST API calls (axios)
└── backend/
    ├── Dockerfile
    └── src/main/java/com/chun/chat/
        ├── ChatApplication.java
        ├── config/
        │   ├── WebSocketConfig.java     # STOMP endpoint 設定
        │   ├── SecurityConfig.java      # SecurityFilterChain + BCrypt bean
        │   └── RedisConfig.java
        ├── security/
        │   ├── JwtAuthFilter.java       # HTTP 層 JWT filter (OncePerRequestFilter)
        │   ├── JwtChannelInterceptor.java  # WebSocket 層 STOMP CONNECT 攔截
        │   └── JwtUtil.java             # token 產生 / 驗證 / 解析
        ├── controller/
        │   ├── ChatController.java      # @MessageMapping 處理 WS 訊息
        │   ├── AuthController.java      # /api/auth/register, /api/auth/login
        │   └── RoomController.java      # /api/rooms CRUD
        ├── service/
        │   ├── MessageService.java
        │   ├── RoomService.java
        │   └── UserService.java
        ├── repository/
        │   ├── MessageRepository.java
        │   ├── RoomRepository.java
        │   ├── RoomMemberRepository.java
        │   └── UserRepository.java
        └── model/
            ├── User.java
            ├── Room.java
            ├── Message.java
            ├── RoomMember.java
            └── RoomMemberId.java       # @EmbeddedId 複合主鍵 key class
```

> 套件根：`com.chun.chat`。

---

## 資料庫 Schema

```sql
-- ============================================================
-- Chat App — Database Initialization Script
-- ============================================================

CREATE DATABASE IF NOT EXISTS chatdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE chatdb;

-- ------------------------------------------------------------
-- users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at    TIMESTAMP    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- rooms
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
  id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100)  NOT NULL,
  created_by  BIGINT        NOT NULL,
  created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
  deleted_at  TIMESTAMP     NULL,
  CONSTRAINT fk_rooms_user FOREIGN KEY (created_by) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- room_members
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS room_members (
  room_id   BIGINT    NOT NULL,
  user_id   BIGINT    NOT NULL,
  is_admin  BOOLEAN   NOT NULL DEFAULT FALSE,
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (room_id, user_id),
  CONSTRAINT fk_rm_room FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_rm_user FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 規則 5 查詢用：找最早加入且仍在 room 的 user
CREATE INDEX idx_rm_room_joined ON room_members(room_id, joined_at ASC);

-- ------------------------------------------------------------
-- messages
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS messages (
  id       BIGINT    AUTO_INCREMENT PRIMARY KEY,
  room_id  BIGINT    NOT NULL,
  user_id  BIGINT    NOT NULL,
  content  TEXT      NOT NULL,
  sent_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES rooms(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分頁查詢複合索引：先過濾房間，再按時間倒序
CREATE INDEX idx_messages_room_sent ON messages(room_id, sent_at DESC);
```

---

## Entity 設計

### 共通規則

- **主鍵一律用物件型別**：`Long`（對應 `BIGINT`）而非 `long`。JPA 靠「id 是否為 null」判斷 entity 是新建（INSERT）還是已存在（UPDATE），primitive 的預設值 `0` 會破壞這個判斷。這條沒有例外。
- **`@GeneratedValue` 用 `IDENTITY`，不要用 `AUTO`**：MySQL 的 `AUTO_INCREMENT` 對應 `GenerationType.IDENTITY`。Hibernate 6 對 `AUTO` 預設走 sequence table 機制，跟 `AUTO_INCREMENT` 對不上，insert 會出問題。
- **可能為 NULL 的欄位用物件型別**：例如 `deleted_at`（schema 允許 NULL）用 `LocalDateTime`。NOT NULL 的欄位（如 `is_deleted`、`is_admin`）可用 primitive `boolean`，但全用物件型別（防呆、防 schema 變動）也是常見團隊慣例。
- **用 `@Getter @Setter`，不要用 `@Data`**：`@Data` 生成的 `equals/hashCode/toString` 在 entity 上有害——toString 會印出 `password_hash`（安全問題）、加上關聯後會觸發 lazy loading 甚至無限遞迴。
- **`@ManyToOne` 一律手動設 `fetch = FetchType.LAZY`**：預設是 EAGER，會在列表查詢時造成 N+1 / 多餘 join。需要關聯資料時再用 `JOIN FETCH`、`@EntityGraph` 或 DTO 投影精準撈。
- **`@JoinColumn(name = ...)` 的 name 指向 DB 的 FK 欄位**，與 Java 欄位名無關。改 Java 欄位名（如 `user` → `creator`）時，`@JoinColumn` 的 name 維持不變（如 `created_by`）。
- **boolean 欄位的命名坑**：primitive `boolean isDeleted` 經 Lombok 生成的 getter 是 `isDeleted()`、setter 是 `setDeleted()`，Hibernate 內省推導出的 property 名是 `deleted`，預設會對到 `deleted` 欄位而非 `is_deleted`。**必須明確標** `@Column(name = "is_deleted", nullable = false)`。
- **欄位約束與 schema 對齊**：`length` 對齊 `VARCHAR(n)`（如 `rooms.name` 是 `VARCHAR(100)`，entity 要 `length = 100`）；`String` 對應 `TEXT` 欄位時需標 `columnDefinition = "TEXT"` 或 `@Lob`（預設會變成 `VARCHAR(255)`）。
- **時間欄位用 `@PrePersist` 設值**，跟 schema 的 `DEFAULT CURRENT_TIMESTAMP` 不衝突（insert 時已帶值，DB default 不會被用到）。`createdAt` 建議加 `updatable = false`。

### 複合主鍵（room_members）

`room_members` 是複合主鍵（room_id + user_id）且帶額外欄位（is_admin、joined_at），不能用單純的 `@ManyToMany`，要拆成獨立 entity：

- **entity 命名單數**（`RoomMember`），table 名複數（`room_members`），與 `User`/`users`、`Room`/`rooms` 一致。
- 用 `@EmbeddedId` + 一個 `@Embeddable` 的 key class（`RoomMemberId`，含 `roomId`、`userId`）。
- 兩個關聯用 `@ManyToOne(fetch = LAZY)` + `@MapsId("...")` + `@JoinColumn`。`@MapsId` 告訴 JPA「這個 FK 同時是複合主鍵的一部分」，使 user_id / room_id 既是 FK 又是 PK，不會變成多餘欄位。`@MapsId` 的字串參數要對到 `RoomMemberId` 裡的欄位名（typo 編譯不報錯但啟動會炸）。
- 被 `@MapsId` 接管的 `@JoinColumn` 不用再標 `nullable = false`（主鍵隱含 not null）。
- **key class（RoomMemberId）必須實作 `equals()` 和 `hashCode()`**（JPA 硬性要求，用來判斷主鍵同一性，缺了會造成 findById 撈不到、重複 insert 等詭異行為），且需要無參數建構子。可手寫（用 `Objects.equals` / `Objects.hash`，兩者欄位必須一致），或用 Lombok `@EqualsAndHashCode`（用在 key class 上安全，因為沒有關聯欄位）。

### 軟刪除

`users`、`rooms` 有 `is_deleted` / `deleted_at`，`messages` 沒有（entity 不要亂加）。entity 端可用 `@PrePersist` 把 `isDeleted` 設為 false。查詢過濾策略見下方 Repository 章節。

---

## Repository 層

### 四個 Repository 定義

```java
// UserRepository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

// RoomRepository
public interface RoomRepository extends JpaRepository<Room, Long> {
}

// RoomMemberRepository（複合主鍵，ID 型別是 RoomMemberId，不是 Long）
public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {
}

// MessageRepository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);
}
```

### 關鍵設計決策

- **`JpaRepository<T, ID>` 的 ID 型別要對應 entity 的 `@Id`**：`RoomRepository` 用 `Long`（不是 `RoomMemberId`）；只有 `RoomMemberRepository` 用複合鍵 `RoomMemberId`。
- **`findByUsername` 回傳 `Optional<User>`**：username 是 unique，最多一筆，登入時用 Optional 表達「可能查無此人」。
- **`MessageRepository` 的分頁查詢回傳 `Page<Message>`**：`Page` 帶總筆數、總頁數等 metadata，前端分頁 UI 會用到。參數帶 `Pageable`，controller 接 `?page=0&size=20` 轉成 `Pageable` 傳入。需 import `org.springframework.data.domain.Page` 與 `Pageable`。
- **`findByRoomIdOrderBySentAtDesc` 對齊索引**：查詢條件（room_id 過濾 + sent_at 倒序）剛好吻合 schema 的複合索引 `idx_messages_room_sent (room_id, sent_at DESC)`，DB 可直接走索引、不需額外排序。
- **`findByRoomId` 不會 join Room 表**：Spring Data 解析成「Room 關聯的 id」，生成的 SQL 直接用 `room_id` FK 欄位，不會把整個 Room 撈出來。
- **前端需 reverse**：後端用 `sent_at DESC` 撈出「新→舊」，但聊天室畫面是「舊在上、新在下」，前端拿到後要 reverse 再顯示。

### 待擴充（之後功能會用到）

- **`RoomMemberRepository`**：查房間成員、判斷成員/admin、「規則 5」最早加入成員（用 `joinedAt` 排序，對應 `idx_rm_room_joined`）。注意欄位包在 `@EmbeddedId` 裡，方法名要用底線穿透巢狀路徑，例如 `findById_RoomId(Long roomId)`（`id` 是 `@EmbeddedId` 欄位名，`RoomId` 是 `RoomMemberId` 裡的欄位）。
- **軟刪除過濾**：目前 `findByUsername` 等查詢會撈到 `is_deleted = true` 的資料。二選一處理：方法名加條件（`findByUsernameAndIsDeletedFalse`），或在 entity 上用 Hibernate `@SQLRestriction("is_deleted = false")`（舊版 `@Where`）統一過濾。

---

## Spring Security 設計

### 版本注意

使用 **Spring Security 6**（隨 Spring Boot 3.x），寫法與舊版不同：
- 不繼承 `WebSecurityConfigurerAdapter`
- 改用 `@Bean SecurityFilterChain` 方式配置

### 依賴（pom.xml）

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
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.11.5</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.11.5</version>
  <scope>runtime</scope>
</dependency>
```

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()   // WS handshake 由 ChannelInterceptor 負責驗證
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### JwtAuthFilter.java（HTTP 層）

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### JwtUtil.java

```java
@Component
public class JwtUtil {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration-ms}") private long expirationMs;

    public String generateToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build()
            .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) { return false; }
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
```

### WebSocket 層認證（ChannelInterceptor）

HTTP filter chain 管不到 STOMP frame，需另外攔截 `CONNECT` 指令：

```java
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 從 STOMP header 或 query string 取 token
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token == null || !jwtUtil.validateToken(token)) {
                throw new MessagingException("Unauthorized");
            }
            String username = jwtUtil.extractUsername(token);
            accessor.setUser(() -> username);
        }
        return message;
    }
}
```

在 `WebSocketConfig` 中註冊：

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(jwtChannelInterceptor);
}
```

### 認證流程總覽

```
HTTP Request  →  JwtAuthFilter  →  SecurityContext  →  Controller
WS CONNECT    →  JwtChannelInterceptor  →  accessor.setUser()  →  ChatController (@MessageMapping)
```

在 `ChatController` 中取得當前用戶：

```java
@MessageMapping("/chat.send")
public void sendMessage(@Payload ChatMessage message, Principal principal) {
    String username = principal.getName();
    // ...
}
```

### 密碼處理

- 儲存時用 `BCryptPasswordEncoder` hash，**不存明文**
- 登入時：`passwordEncoder.matches(rawPassword, storedHash)`

---

## WebSocket 設計

### STOMP Endpoint

- 連線入口：`/ws`（SockJS fallback）
- Message broker prefix：`/topic`（廣播）、`/queue`（點對點）
- Application destination prefix：`/app`

### 訊息流程

```
Client → /app/chat.send → ChatController → SimpMessagingTemplate
                                         → /topic/room.{roomId} → 所有訂閱者
```

### 訂閱 Topic

| Topic | 用途 |
|-------|------|
| `/topic/room.{roomId}` | 房間訊息廣播 |
| `/topic/room.{roomId}.users` | 房間在線人數更新 |
| `/queue/errors` | 個人錯誤通知 |

### 事件類型（MessageType enum）

- `CHAT` — 一般聊天訊息
- `JOIN` — 用戶加入房間
- `LEAVE` — 用戶離開房間

---

## REST API 端點

### Auth

| Method | Path | 說明 |
|--------|------|------|
| POST | `/api/auth/register` | 註冊 |
| POST | `/api/auth/login` | 登入，回傳 JWT |

### Rooms

| Method | Path | 說明 |
|--------|------|------|
| GET | `/api/rooms` | 取得所有房間 |
| POST | `/api/rooms` | 建立新房間 |
| GET | `/api/rooms/{id}/messages` | 取得歷史訊息（分頁） |

### 認證方式

- 所有非 auth 路由需帶 `Authorization: Bearer {jwt}` header。
- WebSocket 連線時透過 query string 帶 token：`/ws?token={jwt}`

---

## Redis 用途

- Key：`online:{roomId}` — Set 型別，存放在線 username
- TTL：連線斷開時移除；定期 heartbeat 維持
- 用途：顯示房間在線人數，不查 DB

---

## 部署架構（Docker / Nginx）

### Docker Compose 服務

```yaml
services:
  nginx:       # port 80, 反向代理
  frontend:    # port 3000 (internal)
  backend:     # port 8080 (internal)
  mysql:       # port 3306 (internal), volume: mysql_data
  redis:       # port 6379 (internal)
```

### Nginx 路由規則

- `/api/*` → backend:8080
- `/ws/*` → backend:8080（需設定 Upgrade header）
- `/*` → frontend:3000

---

## 環境變數

### Backend（application.yml / env）

```
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
REDIS_HOST, REDIS_PORT
JWT_SECRET, JWT_EXPIRATION_MS
```

### Frontend（.env）

```
VITE_API_BASE_URL=/api
VITE_WS_URL=/ws
```

---

## 測試設定（@DataJpaTest + H2）

### 核心原則：正式與測試設定必須是兩個獨立檔案

```
src/main/resources/application.yml   ← 正式（MySQL，ddl-auto: validate）
src/test/resources/application.yml   ← 測試（H2，ddl-auto: create-drop）
```

跑測試時，`src/test/resources/application.yml` 會**覆蓋**（非合併）main 的設定。**絕對不要把兩份設定塞進同一個檔案**——同一份 YAML 出現兩個頂層 `spring:` 是非法的，後者會蓋掉前者，造成設定四不像，且可能讓正式啟動時連不上 MySQL。

### 測試 application.yml（src/test/resources/）

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

### 踩過的坑（重要，避免重蹈）

1. **`ddl-auto` 衝突**：正式用 `validate`（schema 由 init.sql 建，JPA 只驗證），但測試的 H2 是空的，`validate` 會報 `Schema-validation: missing table`。測試一定要改 `create-drop`（自動建表、用完丟）。

2. **方言覆蓋的 key 必須對齊**：正式 yml 用 `spring.jpa.properties.hibernate.dialect: MySQLDialect`。測試要覆蓋它，**必須用同一個 key** `properties.hibernate.dialect: H2Dialect`。若用不同 key（如 `database-platform`），兩者不會互相覆蓋，MySQLDialect 殘留，Hibernate 仍以 MySQL 方言產生 DDL → 每個 `create table` 結尾加 `engine=InnoDB` → H2 不認得 → 建表全失敗 → 後續加外鍵報 `Table not found`。最容易卡很久的坑。

3. **不要用 `MODE=MySQL`**：曾嘗試讓 H2 模擬 MySQL，反而引來 `engine=InnoDB` 語法錯誤。讓 H2 用原生模式 + H2Dialect 最乾淨。

4. **pom 測試依賴**：只需要 `spring-boot-starter-test`（含 JUnit5 / AssertJ / Mockito / Spring Test）一個 starter，**沒有**每個 starter 配 `-test` 的版本（那些 artifact 不存在）。另需 `com.h2database:h2`（scope test）。web starter 是 `spring-boot-starter-web`（不是 `webmvc`）。

5. **無害警告可忽略**：Mockito self-attaching、Java agent dynamic loading、JVM class sharing 等 WARNING 不影響測試，現代 Spring Boot + 新 JDK 必出現。`engine=InnoDB` 若被降為 WARN（Hibernate 容忍）也無害，但若導致後續表不存在就要解方言問題。

6. **`@TestPropertySource` 是最高優先序的覆蓋手段**：若 yml 覆蓋一直不生效，可直接在測試類別上用 `@TestPropertySource(properties = {...})` 強制設定 `ddl-auto`、`dialect`、`datasource.url`，繞過所有 yml 載入順序的不確定性。搭配 `@AutoConfigureTestDatabase(replace = ANY)` 確保用內嵌 H2。

### 測試資料準備要點

- **測試類別命名 `XxxTest`**，不要跟被測 repository 同名（`MessageRepositoryTest`，不是 `MessageRepository`），否則 `@Autowired` 會解析混淆。
- **import 路徑**：`@DataJpaTest` 與 `TestEntityManager` 都在 `org.springframework.boot.test.autoconfigure.orm.jpa` 底下。讓 IDE 自動 import 避免打錯。
- **FK 鏈要從底層往上 persist**：Message 依賴 Room 和 User，Room 依賴 User（creator）。順序：`persist(user)` → `persist(room)` → `persist(message)` → `flush()`。少 persist 任一層都會違反 FK 約束。
- **persist 後 entity 才有 id**（IDENTITY 主鍵在 insert 時生成），persist 前 `getId()` 是 null。
- **`@PrePersist` 會自動填時間**：entity 的 `createdAt` / `sentAt` 有 `@PrePersist`，測試不用手動 set。
- **測排序需手動設不同時間**：若要驗證 `OrderBySentAtDesc`，因 `@PrePersist` 無條件覆寫 `sentAt = now()`，多筆時間幾乎相同無法驗證順序。需先把 `@PrePersist` 改成 `if (sentAt == null)` 才填，再手動給不同時間。否則 assert 用 `containsExactlyInAnyOrder` 避開順序依賴。
- **`@DataJpaTest` 的 transaction 內 session 還開著**，存取 LAZY 關聯（如 `getUser().getUsername()`）不會觸發 `LazyInitializationException`。

### 未來：環境一致性

H2 的 `MODE=MySQL` 只是近似，不是真 MySQL（型別、函數、定序仍有差異），「H2 測過 ≠ MySQL 上一定一樣」。需要測試環境與正式完全一致時，正解是 **Testcontainers**（測試時用 Docker 跑真正的 MySQL 8 容器）。現階段先用 H2 + H2Dialect 把測試跑通即可。

---

## 開發順序建議

1. DB schema + Docker Compose 基礎設定
2. Spring Boot：User entity、JWT 認證、/api/auth endpoints
3. Spring Boot：Room & Message entity、REST endpoints
4. Spring Boot：WebSocketConfig、ChatController（STOMP）
5. React：登入/註冊頁
6. React：房間列表、建立房間
7. React：聊天室 UI + useWebSocket hook
8. Redis：在線用戶狀態
9. Nginx 整合、Docker 全部串起來

> 對應的細部任務拆解（含目標檔案路徑、相依、驗收）見 `TASKS.md`（T01–T44）。

---

## 注意事項

- WebSocket 連線需在 SecurityConfig 中放行 `/ws/**`
- CORS 在開發環境需要設定，正式環境透過 Nginx 同源不需要
- 歷史訊息分頁用 `Pageable`，依 `sent_at DESC` 排序，前端需 reverse 顯示
- JWT 過期後 WebSocket 連線不會自動斷，需在 server 端 validate session
- MySQL 時區設定：`spring.datasource.url` 加上 `?serverTimezone=UTC`
