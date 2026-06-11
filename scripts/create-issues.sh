#!/usr/bin/env bash
#
# create-issues.sh — 用 GitHub CLI 一鍵建立即時聊天室專案的 labels / milestones / issues
#
# 前置需求：
#   1. 安裝 gh        : https://cli.github.com/
#   2. 登入           : gh auth login
#   3. 在 repo 目錄下執行，或設定 REPO 變數（owner/repo）
#
# 特性：可重複執行（idempotent）。已存在的 label/milestone/issue 會跳過，不會重複建立。
#
set -euo pipefail

# ── repo 偵測（也可手動覆寫：REPO=owner/name ./create-issues.sh）────────────
REPO="${REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
echo "Target repo: $REPO"
echo

# ── 1. 建立 Labels ──────────────────────────────────────────────────────────
echo "==> Creating labels..."
create_label() {
  gh label create "$1" --color "$2" --description "$3" --repo "$REPO" --force >/dev/null \
    && echo "  label: $1"
}
create_label infra     6e7781 "Docker / Nginx / DB 初始化 / 環境變數"
create_label backend   0969da "Spring Boot entity / repository / service / controller"
create_label security  d1242f "JWT / Spring Security / ChannelInterceptor"
create_label websocket 8250df "STOMP / broker / ChatController"
create_label frontend  1a7f37 "React 元件 / hooks / api"
create_label redis     bc4c00 "在線狀態 / presence"
create_label test      bf8700 "@DataJpaTest / 整合測試 / Testcontainers"
create_label docs      0e8a16 "文件 / README"
echo

# ── 2. 建立 Milestones ──────────────────────────────────────────────────────
echo "==> Creating milestones..."
ensure_milestone() {
  local title="$1"
  local existing
  existing=$(gh api "repos/$REPO/milestones?state=all" \
              --jq ".[] | select(.title==\"$title\") | .number" 2>/dev/null | head -n1)
  if [ -n "$existing" ]; then
    echo "  milestone exists: $title (#$existing)"
  else
    gh api "repos/$REPO/milestones" -f title="$title" >/dev/null \
      && echo "  milestone created: $title"
  fi
}
ensure_milestone "M0 專案基礎建設"
ensure_milestone "M1 認證系統"
ensure_milestone "M2 房間與訊息（REST）"
ensure_milestone "M3 即時通訊"
ensure_milestone "M4 前端核心"
ensure_milestone "M5 前端聊天室"
ensure_milestone "M6 在線狀態"
ensure_milestone "M7 整合與部署"
echo

# ── 3. 建立 Issues ──────────────────────────────────────────────────────────
echo "==> Creating issues..."
create_issue() {
  local title="$1" labels="$2" milestone="$3" body="$4"
  # 去重：若已有同名 issue（任何狀態）則跳過
  if gh issue list --repo "$REPO" --state all --limit 200 --json title --jq '.[].title' \
       | grep -qxF "$title"; then
    echo "  skip (exists): $title"
    return
  fi
  gh issue create --repo "$REPO" \
    --title "$title" \
    --label "$labels" \
    --milestone "$milestone" \
    --body "$body" >/dev/null \
    && echo "  created: $title"
}

# ── M0 專案基礎建設 ──
create_issue "[infra] 建立 monorepo 目錄骨架（frontend / backend / nginx）" \
  "infra" "M0 專案基礎建設" \
  $'**驗收重點**\n- 目錄結構對齊 PROJECT_KNOWLEDGE.md\n- root 放 docker-compose.yml'

create_issue "[infra] 撰寫 init.sql 建立 chatdb schema" \
  "infra" "M0 專案基礎建設" \
  $'**驗收重點**\n- 4 張表 + 兩個複合索引（idx_messages_room_sent、idx_rm_room_joined）\n- 字元集 utf8mb4_unicode_ci\n相依：#1'

create_issue "[infra] docker-compose 定義五服務骨架" \
  "infra" "M0 專案基礎建設" \
  $'**驗收重點**\n- nginx / frontend / backend / mysql / redis\n- mysql 掛 volume 並跑 init.sql\n- 內部 port 不對外\n相依：#1'

create_issue "[backend] Spring Boot 專案初始化（pom + application.yml）" \
  "backend" "M0 專案基礎建設" \
  $'**驗收重點**\n- 依賴：spring-boot-starter-web（非 webmvc）、data-jpa、security、redis\n- jjwt 0.11.5 三件套（api / impl / jackson）\n- spring.datasource.url 加 ?serverTimezone=UTC\n- ddl-auto: validate\n相依：#1'

create_issue "[frontend] React + Vite 專案初始化" \
  "frontend" "M0 專案基礎建設" \
  $'**驗收重點**\n- Vite 5 / React 18\n- .env 設 VITE_API_BASE_URL=/api、VITE_WS_URL=/ws\n相依：#1'

create_issue "[test] 設定測試環境 application.yml（H2 + create-drop）" \
  "test" "M0 專案基礎建設" \
  $'**驗收重點**\n- 獨立檔放 src/test/resources（不可與 main 合併）\n- 方言用同一個 key 覆蓋成 H2Dialect\n- 不要用 MODE=MySQL\n- h2 依賴 scope test\n相依：#4'

# ── M1 認證系統 ──
create_issue "[backend] User entity" \
  "backend" "M1 認證系統" \
  $'**驗收重點**\n- Long id + GenerationType.IDENTITY\n- is_deleted 明標 @Column(name="is_deleted")\n- @Getter @Setter（非 @Data）\n- @PrePersist 設 createdAt / isDeleted\n相依：#6'

create_issue "[backend] UserRepository（findByUsername → Optional）" \
  "backend" "M1 認證系統" \
  $'**驗收重點**\n- findByUsername 回傳 Optional<User>\n- 預留軟刪除過濾（...AndIsDeletedFalse 或 @SQLRestriction）\n相依：#7'

create_issue "[test] UserRepository @DataJpaTest" \
  "test" "M1 認證系統" \
  $'**驗收重點**\n- persist→flush 後才有 id（IDENTITY）\n- 驗 findByUsername 命中／查無\n相依：#8'

create_issue "[security] JwtUtil（generate / validate / extractUsername）" \
  "security" "M1 認證系統" \
  $'**驗收重點**\n- @Value 注入 secret / expiration\n- 簽章 HS256\n- key 用 BASE64 decode\n相依：#4'

create_issue "[security] SecurityConfig（SecurityFilterChain + BCrypt bean）" \
  "security" "M1 認證系統" \
  $'**驗收重點**\n- 不繼承 WebSecurityConfigurerAdapter\n- SessionCreationPolicy.STATELESS\n- 放行 /api/auth/** 與 /ws/**\n- addFilterBefore 註冊 JwtAuthFilter\n相依：#10'

create_issue "[security] JwtAuthFilter（OncePerRequestFilter）" \
  "security" "M1 認證系統" \
  $'**驗收重點**\n- 解析 Authorization: Bearer\n- 驗 token 後塞 SecurityContext\n相依：#10, #11'

create_issue "[backend] UserService（register / BCrypt hash）" \
  "backend" "M1 認證系統" \
  $'**驗收重點**\n- 存 hash 不存明文\n- 登入用 passwordEncoder.matches(raw, hash)\n相依：#8, #11'

create_issue "[backend] AuthController（/register, /login 回傳 JWT）" \
  "backend,security" "M1 認證系統" \
  $'**驗收重點**\n- login 成功回 token\n- username 重複的處理\n相依：#13, #10'

# ── M2 房間與訊息（REST） ──
create_issue "[backend] Room entity" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- name 標 length=100\n- @ManyToOne(LAZY) creator，@JoinColumn(name="created_by") 不變\n- 軟刪除欄位\n相依：#7'

create_issue "[backend] Message entity" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- content 標 columnDefinition="TEXT"（避免變 VARCHAR(255)）\n- room / user 皆 @ManyToOne(LAZY)\n- 無軟刪除欄位\n相依：#15'

create_issue "[backend] RoomMemberId（@Embeddable 複合主鍵）" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- 實作 equals / hashCode + 無參建構子\n- 欄位 roomId / userId\n相依：#1'

create_issue "[backend] RoomMember entity（@EmbeddedId + @MapsId）" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- 兩個 @MapsId 字串對到 RoomMemberId 欄位（typo 啟動會炸）\n- 被 @MapsId 接管的 JoinColumn 不再標 nullable\n- entity 單數命名 RoomMember\n相依：#16, #17'

create_issue "[backend] RoomRepository / MessageRepository / RoomMemberRepository" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- MessageRepo：Page<Message> findByRoomIdOrderBySentAtDesc(Long, Pageable)\n- RoomMemberRepo 的 ID 型別是 RoomMemberId（非 Long）\n相依：#18'

create_issue "[test] MessageRepository 分頁與排序測試" \
  "test" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- FK 鏈 user→room→message 依序 persist 再 flush\n- 驗排序需把 @PrePersist 改成 if(sentAt==null) 才填，再手動給不同時間\n相依：#19, #9'

create_issue "[backend] RoomService / MessageService" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- 建房同時寫入 RoomMember(admin=true)\n- 歷史訊息查詢回 Page\n相依：#19'

create_issue "[backend] RoomController（GET/POST rooms, GET messages 分頁）" \
  "backend" "M2 房間與訊息（REST）" \
  $'**驗收重點**\n- ?page&size → Pageable\n- 回傳 Page metadata\n- 需帶 Authorization: Bearer\n相依：#21, #12'

# ── M3 即時通訊 ──
create_issue "[websocket] WebSocketConfig（STOMP endpoint /ws + SockJS）" \
  "websocket" "M3 即時通訊" \
  $'**驗收重點**\n- broker prefix /topic、/queue\n- app destination prefix /app\n- configureClientInboundChannel 註冊 interceptor\n相依：#11'

create_issue "[security] JwtChannelInterceptor（攔截 CONNECT）" \
  "security,websocket" "M3 即時通訊" \
  $'**驗收重點**\n- 只在 StompCommand.CONNECT 驗 token\n- accessor.setUser(() -> username)\n- 失敗丟 MessagingException\n相依：#10, #23'

create_issue "[websocket] MessageType enum（CHAT / JOIN / LEAVE）" \
  "websocket" "M3 即時通訊" \
  $'**驗收重點**\n- 三個值：CHAT / JOIN / LEAVE\n相依：#1'

create_issue "[websocket] ChatController（@MessageMapping /chat.send）" \
  "websocket" "M3 即時通訊" \
  $'**驗收重點**\n- 用 Principal 取 username\n- 持久化後 SimpMessagingTemplate 廣播到 /topic/room.{roomId}\n相依：#24, #21, #25'

create_issue "[test] WebSocket 連線整合測試（含 JWT handshake）" \
  "test" "M3 即時通訊" \
  $'**驗收重點**\n- 無 token 應拒連\n- 有效 token 可收發\n相依：#26'

# ── M4 前端核心 ──
create_issue "[frontend] api/index.js（axios 實例 + 攔截器注入 Bearer）" \
  "frontend" "M4 前端核心" \
  $'**驗收重點**\n- baseURL 用 VITE_API_BASE_URL\n- 攔截器自動帶 token\n相依：#5'

create_issue "[frontend] useAuth hook（登入狀態管理）" \
  "frontend" "M4 前端核心" \
  $'**驗收重點**\n- 存／清 token\n- 提供 isAuthed\n相依：#28'

create_issue "[frontend] 登入 / 註冊頁" \
  "frontend" "M4 前端核心" \
  $'**驗收重點**\n- 串 /api/auth/*\n- 成功後導向房間列表\n相依：#29, #14'

create_issue "[frontend] RoomList + 建立房間" \
  "frontend" "M4 前端核心" \
  $'**驗收重點**\n- 串 GET/POST /api/rooms\n- 建房後進入房間\n相依：#28, #22'

# ── M5 前端聊天室 ──
create_issue "[frontend] useWebSocket hook（STOMP + SockJS 連線管理）" \
  "frontend" "M5 前端聊天室" \
  $'**驗收重點**\n- 連線帶 token\n- 訂閱 /topic/room.{roomId}\n- 斷線重連\n相依：#23, #29'

create_issue "[frontend] ChatRoom 主畫面組裝" \
  "frontend" "M5 前端聊天室" \
  $'**驗收重點**\n- 整合 MessageList / MessageInput / UserList\n相依：#32'

create_issue "[frontend] MessageList（含歷史分頁載入）" \
  "frontend" "M5 前端聊天室" \
  $'**驗收重點**\n- 後端 sent_at DESC 撈回後 reverse 顯示\n- 上滑載入更舊頁\n相依：#33, #22'

create_issue "[frontend] MessageInput（送 /app/chat.send）" \
  "frontend" "M5 前端聊天室" \
  $'**驗收重點**\n- Enter 送出\n- 空訊息阻擋\n相依：#33'

# ── M6 在線狀態 ──
create_issue "[backend] RedisConfig" \
  "redis" "M6 在線狀態" \
  $'**驗收重點**\n- 連線設定走環境變數（REDIS_HOST / REDIS_PORT）\n相依：#4'

create_issue "[redis] 在線用戶 Set 管理（online:{roomId}）" \
  "redis" "M6 在線狀態" \
  $'**驗收重點**\n- JOIN 加入 / LEAVE 或斷線移除\n- heartbeat 維持 TTL\n相依：#36, #26'

create_issue "[websocket] 廣播在線人數到 /topic/room.{roomId}.users" \
  "websocket,redis" "M6 在線狀態" \
  $'**驗收重點**\n- 人數變動即時推播\n相依：#37'

create_issue "[frontend] UserList（訂閱在線人數）" \
  "frontend" "M6 在線狀態" \
  $'**驗收重點**\n- 即時更新在線名單\n相依：#38, #32'

# ── M7 整合與部署 ──
create_issue "[infra] backend / frontend Dockerfile" \
  "infra" "M7 整合與部署" \
  $'**驗收重點**\n- 多階段建置\n- frontend build 後由 nginx 或 node 服務\n相依：#4, #5'

create_issue "[infra] nginx.conf 路由（/api、/ws、/）" \
  "infra" "M7 整合與部署" \
  $'**驗收重點**\n- /ws 設 Upgrade / Connection header\n- /api、/ 反向代理\n相依：#40'

create_issue "[infra] 環境變數整合（backend env + compose）" \
  "infra" "M7 整合與部署" \
  $'**驗收重點**\n- DB / Redis / JWT 變數打通\n- 同源免 CORS\n相依：#3, #41'

create_issue "[test] 端對端煙霧測試（compose up 後走完註冊→建房→收發訊息）" \
  "test" "M7 整合與部署" \
  $'**驗收重點**\n- 全棧可啟動\n- 核心流程通\n相依：#42'

create_issue "[docs] 更新 README（啟動方式、架構圖）" \
  "docs" "M7 整合與部署" \
  $'**驗收重點**\n- 一鍵啟動說明\n相依：#43'

echo
echo "==> Done. 共處理 8 labels / 8 milestones / 44 issues。"
