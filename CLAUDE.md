# CLAUDE.md — 即時聊天室

> 本檔案是 Claude Code 的專案入口，每次啟動自動載入。詳細規範與任務以 `@import` 帶入。

## 專案簡介

多人即時聊天室（多房間、歷史訊息分頁、在線狀態）。
技術棧：React + Vite / Spring Boot 3 + Spring Security 6 / MySQL 8 / Redis 7 / Nginx，全棧用 Docker Compose 串起來。

## 文件導覽

- 技術規範與踩坑筆記：@docs/PROJECT_KNOWLEDGE.md
- 開發任務清單（T01–T44）：@docs/TASKS.md
- 建立 GitHub issues 的腳本：`scripts/create-issues.sh`（手動執行，非自動）

**動工前務必先讀 `PROJECT_KNOWLEDGE.md`**，裡面的設計決策與踩坑筆記是這份專案的硬性約定，不要憑直覺重寫。

## 全域約定（必守）

- **套件根：`com.chun.chat`**。所有 Java 檔案在此之下。
- **語言**：與我溝通用繁體中文；程式碼註解可中英混用。
- **entity**：主鍵用 `Long` + `IDENTITY`；`@Getter @Setter`（禁用 `@Data`）；`@ManyToOne` 一律 `fetch = LAZY`；boolean 欄位明標 `@Column(name = "...")`。細節見規範文件「Entity 設計」。
- **Spring Security 6 寫法**：`@Bean SecurityFilterChain`，不繼承 `WebSecurityConfigurerAdapter`。
- **設定檔分離**：正式 `src/main/resources/application.yml`（MySQL、`ddl-auto: validate`）與測試 `src/test/resources/application.yml`（H2、`create-drop`、`H2Dialect`）是**兩個獨立檔**，絕不合併。

## 開發流程

1. 從 `TASKS.md` 由上往下挑任務（已按相依排序）。
2. 開工前確認該任務 `相依` 欄列出的任務都已完成 `[x]`。
3. 依任務標注的「目標檔案路徑」實作，細節對照 `PROJECT_KNOWLEDGE.md`。
4. 有測試的任務：跑測試綠燈才算完成。
5. 完成後把該任務的 `[ ]` 改成 `[x]`，更新 milestone 進度摘要，再進下一項。

## 常用指令

```bash
# 後端（在 backend/ 下）
./mvnw spring-boot:run          # 啟動
./mvnw test                     # 跑測試

# 前端（在 frontend/ 下）
npm run dev                     # 開發伺服器
npm run build                   # 打包

# 全棧
docker compose up --build       # 一鍵起全部服務（root 目錄）
```

## 注意事項

- 改動跨多檔的設計（如 entity 欄位、API 介面）前，先跟我確認再動手。
- 不要把測試設定塞進正式 `application.yml`；不要用 H2 的 `MODE=MySQL`（細節見規範文件「測試設定」）。
- WebSocket 認證走 `JwtChannelInterceptor`（攔 STOMP CONNECT），不是 HTTP filter；`/ws/**` 在 SecurityConfig 放行。
