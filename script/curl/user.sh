#!/usr/bin/env bash
# =============================================================
# 用户端接口 curl 调试命令（Git Bash / Linux / macOS）
# 前置条件:
#   1. 后端已在 127.0.0.1:8080 启动
#   2. 钱包已连接 Sepolia，并已把 DocToken 转到该钱包（管理端 mint 或链上 ETH 兑换）
#   3. 用钱包对文案 "ChainDocProof:<你的地址>" 做签名（MetaMask 等钱包的 Sign Message）
# 用法: 修改下方 ADDR / SIG 后，从上到下逐段执行
# =============================================================
BASE=${BASE:-http://127.0.0.1:8080}
ADDR=${ADDR:-"0x0000000000000000000000000000000000000000"}   # 你的钱包地址（小写）
SIG=${SIG:-"0x000"}                                            # 对 ChainDocProof:<地址> 的签名（r||s||v hex）
FILE=${FILE:-contract.pdf}                                     # 待上传/校验的本地文件
TXHASH=${TXHASH:-"0x000"}                                      # 购买次数时的链上转账交易 hash
QUANTITY=${QUANTITY:-10}                                       # 购买次数

# ---------- 1. 钱包签名登录，拿 token ----------
RESP=$(curl -s -X POST "$BASE/api/user/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"address\":\"$ADDR\",\"signature\":\"$SIG\"}")
echo "$RESP"
TOKEN=$(echo "$RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -z "$TOKEN" ] && { echo "登录失败，请检查地址/签名"; exit 1; }
AUTH="Authorization: Bearer $TOKEN"
echo "TOKEN=$TOKEN"

# ---------- 2. 查询剩余次数 ----------
curl -s "$BASE/api/user/credits" -H "$AUTH"; echo

# ---------- 3. 购买次数（先把 DocToken 从钱包转给后端地址，再提交 txHash）----------
# 需转金额 = app.credit.unit-price-doc-token × QUANTITY（wei）
curl -s -X POST "$BASE/api/user/credits/purchase" -H "$AUTH" \
  -H 'Content-Type: application/json' \
  -d "{\"quantity\":$QUANTITY,\"txHash\":\"$TXHASH\"}"; echo

# ---------- 4. 上传文件（冷却 60s，文件不落盘）----------
RESP=$(curl -s -X POST "$BASE/api/user/proofs/upload" -H "$AUTH" \
  -F "file=@$FILE")
echo "$RESP"
PROOF_ID=$(echo "$RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')

# ---------- 5. 存证上链（扣 1 次次数）----------
curl -s -X POST "$BASE/api/user/proofs/$PROOF_ID/store" -H "$AUTH"; echo

# ---------- 6. 校验文件是否被篡改（扣 1 次次数，data=true 表示未篡改）----------
curl -s -X POST "$BASE/api/user/proofs/$PROOF_ID/verify" -H "$AUTH" \
  -F "file=@$FILE"; echo

# ---------- 7. 查询我的存证列表 ----------
curl -s "$BASE/api/user/proofs" -H "$AUTH"; echo
