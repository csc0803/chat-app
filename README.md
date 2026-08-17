# 即時聊天室（chat-app）

多人即時聊天室：多房間、歷史訊息分頁查詢、Redis 在線狀態。前端 React + Vite，後端 Spring Boot（REST + STOMP WebSocket），MySQL 儲存、Redis 管在線名單，Nginx 統一入口。

技術細節與踩坑筆記見 [`docs/PROJECT_KNOWLEDGE.md`](docs/PROJECT_KNOWLEDGE.md)；開發任務清單見 [`docs/TASKS.md`](docs/TASKS.md)。

## 架構圖

```
                         ┌────────────┐
      瀏覽器  ──80──────▶│   Nginx    │
                         └─────┬──────┘
                    ┌──────────┼──────────┐
                    │  /api、/ws          │  /（其餘）
                    ▼                     ▼
            ┌───────────────┐     ┌───────────────┐
            │   backend     │     │   frontend    │
            │  Spring Boot  │     │ React (nginx) │
            │  REST + STOMP │     └───────────────┘
            └───┬───────┬───┘
                │       │
                ▼       ▼
          ┌─────────┐ ┌─────────┐
          │  MySQL  │ │  Redis  │
          │ chatdb  │ │ online: │
          │         │ │ {roomId}│
          └─────────┘ └─────────┘
```

- 前端只打 `/api`（REST）與 `/ws`（STOMP over SockJS），同源打進 Nginx，不需要處理 CORS。
- 訊息廣播走 `/topic/room.{roomId}`；在線人數走 `/topic/room.{roomId}.users`（見 `PresenceEventListener`，訂閱即視為加入房間，斷線即視為離開）。
- 在線名單存在 Redis `online:{roomId}` Set，不查 DB。

## 一鍵啟動

前置需求：Docker + Docker Compose。

```bash
JWT_SECRET=$(openssl rand -base64 32) docker compose up -d --build
```

啟動後打開 http://localhost 即可註冊 / 登入 / 建房 / 聊天。

`JWT_SECRET` 必須是 Base64 字串（解碼後 ≥ 32 bytes），未設定時 `docker compose` 會警告並退回空字串，導致簽章失敗，**務必自行提供**。

停止：

```bash
docker compose down
```

## 端對端煙霧測試

`docker compose up` 後，用下面的腳本自動跑一次「註冊 → 登入 → 建房 → 撈歷史訊息」，全部通過才算過；跑完會自動 `docker compose down`，不留殘留容器：

```bash
./scripts/smoke-test.sh
```

## 本機開發（不用 Docker）

後端（需要本機或另開容器的 MySQL + Redis，見 `backend/src/main/resources/application.yaml` 的環境變數）：

```bash
cd backend
./mvnw test    # 跑測試（H2，不需要 MySQL/Redis）
./mvnw spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

## 環境變數

| 變數 | 說明 | 預設 |
|------|------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASS` | MySQL 連線 | `localhost` / `3306` / `chatdb` / `devuser` / `devpassword` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 連線 | `localhost` / `6379` |
| `JWT_SECRET` | JWT 簽章金鑰（Base64，解碼後 ≥ 32 bytes） | 無安全預設，正式環境務必自行提供 |
| `JWT_EXPIRATION_MS` | JWT 過期時間（毫秒） | `86400000`（24h） |

前端建置時期變數見 `frontend/.env`（`VITE_API_BASE_URL`、`VITE_WS_URL`）。
