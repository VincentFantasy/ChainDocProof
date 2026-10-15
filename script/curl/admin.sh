#!/usr/bin/env bash
# =============================================================
# 管理端接口 curl 调试命令（Git Bash / Linux / macOS）
# 前置条件: 后端已在 127.0.0.1:8080 启动；已执行 script/sql/init_admin.sql
# 默认账户: admin / admin123（注意连续输错密码会全局锁定 60s）
# =============================================================
BASE=${BASE:-http://127.0.0.1:8080}
USERNAME=${USERNAME:-admin}
PASSWORD=${PASSWORD:-admin123}
TO_ADDR=${TO_ADDR:-"0x0000000000000000000000000000000000000000"}  # mint 目标钱包
AMOUNT=${AMOUNT:-10000000000000000000}                            # 发放数量（wei = 10 DocToken）
RATE=${RATE:-0.0002}                                              # 新汇率（1 枚 DocToken = RATE ETH）
COST=${COST:-1}                                                   # 新存证费用（每次存证消耗 COST 枚 DocToken）

# ---------- 1. 登录，拿 token ----------
RESP=$(curl -s -X POST "$BASE/admin/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
echo "$RESP"
TOKEN=$(echo "$RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -z "$TOKEN" ] && { echo "登录失败"; exit 1; }
AUTH="Authorization: Bearer $TOKEN"

# ---------- 2. 查询普通用户（地址 / 余额 / 累计购买）----------
curl -s "$BASE/admin/users" -H "$AUTH"; echo

# ---------- 3. 查询存证记录（可按钱包地址筛选）----------
curl -s "$BASE/admin/proofs" -H "$AUTH"; echo
# curl -s "$BASE/admin/proofs?address=$TO_ADDR" -H "$AUTH"; echo

# ---------- 4. 查询 DocToken 管理端发放记录 ----------
curl -s "$BASE/admin/tokens/mints" -H "$AUTH"; echo

# ---------- 5. 查询 DocToken 的 ETH 链上兑换次数（读合约 buyCount）----------
curl -s "$BASE/admin/tokens/exchanges" -H "$AUTH"; echo

# ---------- 6. 查询当前 ETH 兑换汇率（读链上合约 ethPerToken，wei）----------
curl -s "$BASE/admin/tokens/exchange-rate" -H "$AUTH"; echo

# ---------- 7. 调整 ETH 兑换汇率（1 枚 DocToken = RATE ETH，十进制字符串，最多 18 位小数）----------
curl -s -X POST "$BASE/admin/tokens/exchange-rate" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"rate\":\"$RATE\"}"; echo

# ---------- 8. 查询当前存证费用（读链上合约 DocProof.costPerProof，wei）----------
curl -s "$BASE/admin/proofs/cost" -H "$AUTH"; echo

# ---------- 9. 调整存证费用（每次存证消耗 COST 枚 DocToken，十进制字符串，最多 18 位小数）----------
curl -s -X POST "$BASE/admin/proofs/cost" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"amount\":\"$COST\"}"; echo

# ---------- 10. 给指定钱包 mint 发放 DocToken ----------
curl -s -X POST "$BASE/admin/tokens/mint" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"toAddress\":\"$TO_ADDR\",\"amount\":\"$AMOUNT\"}"; echo
