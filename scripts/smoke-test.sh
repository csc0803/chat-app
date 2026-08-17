#!/usr/bin/env bash
#
# smoke-test.sh — T43 端對端煙霧測試
#
# 用 docker compose 啟動整套服務，透過 nginx (http://localhost) 走一次
# 註冊 → 登入 → 建立房間 → REST 撈歷史訊息 的核心流程，全部通過才算過。
#
# 用法：
#   JWT_SECRET=xxx ./scripts/smoke-test.sh
#   （不給 JWT_SECRET 就用下面的預設值，僅供本機煙霧測試用，勿用於正式環境）
#
# 特性：跑完（無論成功失敗）都會 docker compose down，不留殘留容器。
#
set -euo pipefail
cd "$(dirname "$0")/.."

export JWT_SECRET="${JWT_SECRET:-dGhpc2lzYXZlcnlsb25nc2VjcmV0a2V5Zm9ydGVzdGluZzEyMw==}"
BASE_URL="http://localhost"
USERNAME="smoketest_$(date +%s)"
PASSWORD="smoketest_pw"

cleanup() {
  echo "==> docker compose down"
  docker compose down
}
trap cleanup EXIT

echo "==> docker compose up -d --build"
docker compose up -d --build

echo "==> 等待 backend 就緒（透過 nginx /api/auth/login 探測，最多 60 秒）"
ready=0
for _ in $(seq 1 30); do
  status=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/auth/login" \
    -X POST -H "Content-Type: application/json" -d '{"username":"__probe__","password":"__probe__"}' || true)
  # backend 起來後，帳密錯誤的 probe 應回 401（非 000/502/503）
  if [ "$status" = "401" ]; then
    ready=1
    break
  fi
  sleep 2
done
if [ "$ready" -ne 1 ]; then
  echo "FAIL: backend 在時限內沒有就緒"
  docker compose logs backend --tail 100
  exit 1
fi
echo "  backend 就緒"

echo "==> 註冊 $USERNAME"
register_status=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/auth/register" \
  -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
[ "$register_status" = "201" ] || { echo "FAIL: register 回 $register_status（預期 201）"; exit 1; }
echo "  201 OK"

echo "==> 登入取得 JWT"
token=$(curl -s "$BASE_URL/api/auth/login" \
  -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
[ -n "$token" ] && [[ "$token" == *.*.* ]] || { echo "FAIL: 登入沒有拿到合法 JWT：$token"; exit 1; }
echo "  拿到 JWT"

echo "==> 建立房間"
room_json=$(curl -s "$BASE_URL/api/rooms" \
  -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $token" \
  -d '{"name":"smoke-room"}')
room_id=$(echo "$room_json" | grep -o '"id":[0-9]*' | head -n1 | grep -o '[0-9]*')
[ -n "$room_id" ] || { echo "FAIL: 建房沒有回傳 id：$room_json"; exit 1; }
echo "  房間 id=$room_id"

echo "==> 撈歷史訊息（應為空分頁，200）"
messages_status=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/rooms/$room_id/messages?page=0&size=20" \
  -H "Authorization: Bearer $token")
[ "$messages_status" = "200" ] || { echo "FAIL: 撈訊息回 $messages_status（預期 200）"; exit 1; }
echo "  200 OK"

echo
echo "==> 煙霧測試全數通過：註冊 → 登入 → 建房 → 撈訊息"
