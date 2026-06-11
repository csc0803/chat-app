# TASKS.md — 即時聊天室 開發任務清單

> 給 Claude Code 的工作說明：
> 1. **依序執行**：任務已按相依關係由上往下排列，原則上照順序做。
> 2. **尊重相依**：開始某任務前，確認其 `相依` 欄列出的任務都已 `[x]`。
> 3. **完成後打勾**：實作並通過驗收後，把該任務的 `[ ]` 改成 `[x]` 再進下一項。
> 4. **對照規範**：實作細節與踩坑點請參照 `PROJECT_KNOWLEDGE.md`，本檔的「驗收」是濃縮版檢查點。
> 5. **有測試的任務**：寫完跑測試（`./mvnw test` 或對應指令），綠燈才算完成。
>
> 任務 ID 格式 `T01`–`T44`，對應 milestone `M0`–`M7`。

---

## M0 — 專案基礎建設

- [x] **T01 · [infra] 建立 monorepo 目錄骨架**
  - 檔案：`chat-app/`（frontend / backend / nginx 子目錄）、`docker-compose.yml`
  - 相依：—
  - 驗收：目錄結構對齊 PROJECT_KNOWLEDGE.md；root 有 docker-compose.yml

- [ ] **T02 · [infra] 撰寫 init.sql 建立 chatdb schema**
  - 檔案：`init.sql`（或 `mysql/init.sql`，由 compose 掛載）
  - 相依：T01
  - 驗收：users / rooms / room_members / messages 4 表 + 索引 `idx_messages_room_sent`、`idx_rm_room_joined`；charset utf8mb4_unicode_ci

- [ ] **T03 · [infra] docker-compose 定義五服務骨架**
  - 檔案：`docker-compose.yml`
  - 相依：T01
  - 驗收：nginx / frontend / backend / mysql / redis；mysql 掛 volume 並跑 init.sql；內部 port 不對外

- [ ] **T04 · [backend] Spring Boot 專案初始化**
  - 檔案：`backend/pom.xml`、`backend/src/main/resources/application.yml`、`backend/src/main/java/com/chun/chat/ChatApplication.java`
  - 相依：T01
  - 驗收：依賴 web（非 webmvc）/ data-jpa / security / redis + jjwt 0.11.5 三件套；url 加 `?serverTimezone=UTC`；ddl-auto: validate

- [ ] **T05 · [frontend] React + Vite 專案初始化**
  - 檔案：`frontend/package.json`、`frontend/vite.config.js`、`frontend/index.html`、`frontend/src/main.jsx`、`frontend/src/App.jsx`、`frontend/.env`
  - 相依：T01
  - 驗收：Vite 5 / React 18；.env 設 `VITE_API_BASE_URL=/api`、`VITE_WS_URL=/ws`

- [ ] **T06 · [test] 測試環境 application.yml（H2 + create-drop）**
  - 檔案：`backend/src/test/resources/application.yml`
  - 相依：T04
  - 驗收：獨立檔（不可與 main 合併）；方言用同 key 覆蓋成 H2Dialect；不要 MODE=MySQL；h2 scope test

---

## M1 — 認證系統

- [ ] **T07 · [backend] User entity**
  - 檔案：`backend/src/main/java/com/chun/chat/model/User.java`
  - 相依：T06
  - 驗收：Long id + IDENTITY；is_deleted 明標 `@Column(name="is_deleted")`；`@Getter @Setter`（非 @Data）；`@PrePersist` 設 createdAt/isDeleted

- [ ] **T08 · [backend] UserRepository**
  - 檔案：`backend/src/main/java/com/chun/chat/repository/UserRepository.java`
  - 相依：T07
  - 驗收：`findByUsername` 回 `Optional<User>`；預留軟刪除過濾

- [ ] **T09 · [test] UserRepository @DataJpaTest**
  - 檔案：`backend/src/test/java/com/chun/chat/repository/UserRepositoryTest.java`
  - 相依：T08
  - 驗收：persist→flush 後才有 id；驗 findByUsername 命中／查無

- [ ] **T10 · [security] JwtUtil**
  - 檔案：`backend/src/main/java/com/chun/chat/security/JwtUtil.java`
  - 相依：T04
  - 驗收：`@Value` 注入 secret/expiration；HS256；key 用 BASE64 decode

- [ ] **T11 · [security] SecurityConfig**
  - 檔案：`backend/src/main/java/com/chun/chat/config/SecurityConfig.java`
  - 相依：T10
  - 驗收：不繼承 WebSecurityConfigurerAdapter；STATELESS；放行 `/api/auth/**`、`/ws/**`；addFilterBefore 註冊 JwtAuthFilter；BCrypt bean

- [ ] **T12 · [security] JwtAuthFilter（OncePerRequestFilter）**
  - 檔案：`backend/src/main/java/com/chun/chat/security/JwtAuthFilter.java`
  - 相依：T10, T11
  - 驗收：解析 `Bearer`；驗 token 後塞 SecurityContext

- [ ] **T13 · [backend] UserService（register / BCrypt）**
  - 檔案：`backend/src/main/java/com/chun/chat/service/UserService.java`
  - 相依：T08, T11
  - 驗收：存 hash 不存明文；登入用 `matches(raw, hash)`

- [ ] **T14 · [backend] AuthController（/register, /login）**
  - 檔案：`backend/src/main/java/com/chun/chat/controller/AuthController.java`
  - 相依：T13, T10
  - 驗收：login 成功回 JWT；username 重複處理

---

## M2 — 房間與訊息（REST）

- [ ] **T15 · [backend] Room entity**
  - 檔案：`backend/src/main/java/com/chun/chat/model/Room.java`
  - 相依：T07
  - 驗收：name `length=100`；`@ManyToOne(LAZY)` creator + `@JoinColumn(name="created_by")` 不變；軟刪除欄位

- [ ] **T16 · [backend] Message entity**
  - 檔案：`backend/src/main/java/com/chun/chat/model/Message.java`
  - 相依：T15
  - 驗收：content `columnDefinition="TEXT"`；room/user 皆 `@ManyToOne(LAZY)`；無軟刪除欄位

- [ ] **T17 · [backend] RoomMemberId（@Embeddable 複合主鍵）**
  - 檔案：`backend/src/main/java/com/chun/chat/model/RoomMemberId.java`
  - 相依：T01
  - 驗收：實作 equals/hashCode + 無參建構子；欄位 roomId/userId

- [ ] **T18 · [backend] RoomMember entity（@EmbeddedId + @MapsId）**
  - 檔案：`backend/src/main/java/com/chun/chat/model/RoomMember.java`
  - 相依：T16, T17
  - 驗收：兩個 `@MapsId` 字串對到 RoomMemberId 欄位；被接管的 JoinColumn 不再標 nullable；entity 單數命名

- [ ] **T19 · [backend] Room / Message / RoomMember Repository**
  - 檔案：`backend/src/main/java/com/chun/chat/repository/{RoomRepository,MessageRepository,RoomMemberRepository}.java`
  - 相依：T18
  - 驗收：MessageRepo `Page<Message> findByRoomIdOrderBySentAtDesc(Long, Pageable)`；RoomMemberRepo ID 型別 `RoomMemberId`

- [ ] **T20 · [test] MessageRepository 分頁與排序測試**
  - 檔案：`backend/src/test/java/com/chun/chat/repository/MessageRepositoryTest.java`
  - 相依：T19, T09
  - 驗收：FK 鏈 user→room→message 依序 persist+flush；驗排序需 `@PrePersist` 改 `if(sentAt==null)` 再手動給時間

- [ ] **T21 · [backend] RoomService / MessageService**
  - 檔案：`backend/src/main/java/com/chun/chat/service/{RoomService,MessageService}.java`
  - 相依：T19
  - 驗收：建房寫入 RoomMember(admin=true)；歷史訊息查詢回 Page

- [ ] **T22 · [backend] RoomController（rooms CRUD + messages 分頁）**
  - 檔案：`backend/src/main/java/com/chun/chat/controller/RoomController.java`
  - 相依：T21, T12
  - 驗收：`?page&size` → Pageable；回傳 Page metadata；需帶 Bearer

---

## M3 — 即時通訊（WebSocket）

- [ ] **T23 · [websocket] WebSocketConfig（STOMP /ws + SockJS）**
  - 檔案：`backend/src/main/java/com/chun/chat/config/WebSocketConfig.java`
  - 相依：T11
  - 驗收：broker `/topic` `/queue`；app prefix `/app`；configureClientInboundChannel 註冊 interceptor

- [ ] **T24 · [security] JwtChannelInterceptor（攔截 CONNECT）**
  - 檔案：`backend/src/main/java/com/chun/chat/security/JwtChannelInterceptor.java`
  - 相依：T10, T23
  - 驗收：只在 `StompCommand.CONNECT` 驗 token；`accessor.setUser(...)`；失敗丟 MessagingException

- [ ] **T25 · [websocket] MessageType enum（CHAT / JOIN / LEAVE）**
  - 檔案：`backend/src/main/java/com/chun/chat/model/MessageType.java`
  - 相依：T01
  - 驗收：三個值 CHAT / JOIN / LEAVE

- [ ] **T26 · [websocket] ChatController（@MessageMapping /chat.send）**
  - 檔案：`backend/src/main/java/com/chun/chat/controller/ChatController.java`
  - 相依：T24, T21, T25
  - 驗收：用 `Principal` 取 username；持久化後 `SimpMessagingTemplate` 廣播到 `/topic/room.{roomId}`

- [ ] **T27 · [test] WebSocket 連線整合測試（含 JWT handshake）**
  - 檔案：`backend/src/test/java/com/chun/chat/ChatWebSocketTest.java`
  - 相依：T26
  - 驗收：無 token 拒連；有效 token 可收發

---

## M4 — 前端核心

- [ ] **T28 · [frontend] api/index.js（axios + Bearer 攔截器）**
  - 檔案：`frontend/src/api/index.js`
  - 相依：T05
  - 驗收：baseURL 用 `VITE_API_BASE_URL`；攔截器自動帶 token

- [ ] **T29 · [frontend] useAuth hook**
  - 檔案：`frontend/src/hooks/useAuth.js`
  - 相依：T28
  - 驗收：存／清 token；提供 isAuthed

- [ ] **T30 · [frontend] 登入 / 註冊頁**
  - 檔案：`frontend/src/components/{Login,Register}.jsx`（或合併 AuthPage）
  - 相依：T29, T14
  - 驗收：串 `/api/auth/*`；成功後導向房間列表

- [ ] **T31 · [frontend] RoomList + 建立房間**
  - 檔案：`frontend/src/components/RoomList.jsx`
  - 相依：T28, T22
  - 驗收：串 `GET/POST /api/rooms`；建房後進入房間

---

## M5 — 前端聊天室

- [ ] **T32 · [frontend] useWebSocket hook（STOMP + SockJS）**
  - 檔案：`frontend/src/hooks/useWebSocket.js`
  - 相依：T23, T29
  - 驗收：連線帶 token；訂閱 `/topic/room.{roomId}`；斷線重連

- [ ] **T33 · [frontend] ChatRoom 主畫面組裝**
  - 檔案：`frontend/src/components/ChatRoom.jsx`
  - 相依：T32
  - 驗收：整合 MessageList / MessageInput / UserList

- [ ] **T34 · [frontend] MessageList（含歷史分頁載入）**
  - 檔案：`frontend/src/components/MessageList.jsx`
  - 相依：T33, T22
  - 驗收：後端 DESC 撈回後 **reverse** 顯示；上滑載入更舊頁

- [ ] **T35 · [frontend] MessageInput（送 /app/chat.send）**
  - 檔案：`frontend/src/components/MessageInput.jsx`
  - 相依：T33
  - 驗收：Enter 送出；空訊息阻擋

---

## M6 — 在線狀態

- [ ] **T36 · [redis] RedisConfig**
  - 檔案：`backend/src/main/java/com/chun/chat/config/RedisConfig.java`
  - 相依：T04
  - 驗收：連線走環境變數 REDIS_HOST / REDIS_PORT

- [ ] **T37 · [redis] 在線用戶 Set 管理（online:{roomId}）**
  - 檔案：`backend/src/main/java/com/chun/chat/service/PresenceService.java`（新增）
  - 相依：T36, T26
  - 驗收：JOIN 加入 / LEAVE 或斷線移除；heartbeat 維持 TTL

- [ ] **T38 · [websocket] 廣播在線人數到 /topic/room.{roomId}.users**
  - 檔案：ChatController / WebSocket event listener
  - 相依：T37
  - 驗收：人數變動即時推播

- [ ] **T39 · [frontend] UserList（訂閱在線人數）**
  - 檔案：`frontend/src/components/UserList.jsx`
  - 相依：T38, T32
  - 驗收：即時更新在線名單

---

## M7 — 整合與部署

- [ ] **T40 · [infra] backend / frontend Dockerfile**
  - 檔案：`backend/Dockerfile`、`frontend/Dockerfile`
  - 相依：T04, T05
  - 驗收：多階段建置；frontend build 後由 nginx 或 node 服務

- [ ] **T41 · [infra] nginx.conf 路由（/api、/ws、/）**
  - 檔案：`nginx/nginx.conf`
  - 相依：T40
  - 驗收：`/ws` 設 Upgrade/Connection header；`/api`、`/` 反向代理

- [ ] **T42 · [infra] 環境變數整合（backend env + compose）**
  - 檔案：`docker-compose.yml`、`backend/src/main/resources/application.yml`
  - 相依：T03, T41
  - 驗收：DB / Redis / JWT 變數打通；同源免 CORS

- [ ] **T43 · [test] 端對端煙霧測試**
  - 檔案：手動或 `scripts/smoke-test.sh`
  - 相依：T42
  - 驗收：`docker compose up` 後走完 註冊→建房→收發訊息

- [ ] **T44 · [docs] 更新 README**
  - 檔案：`README.md`
  - 相依：T43
  - 驗收：一鍵啟動說明、架構圖

---

## 進度摘要

| Milestone | 任務 | 狀態 |
|-----------|------|------|
| M0 專案基礎建設 | T01–T06 | ☐ |
| M1 認證系統 | T07–T14 | ☐ |
| M2 房間與訊息（REST） | T15–T22 | ☐ |
| M3 即時通訊 | T23–T27 | ☐ |
| M4 前端核心 | T28–T31 | ☐ |
| M5 前端聊天室 | T32–T35 | ☐ |
| M6 在線狀態 | T36–T39 | ☐ |
| M7 整合與部署 | T40–T44 | ☐ |

> 完成一個 milestone 後把對應 ☐ 改成 ☑。
