# ChainDocProof 链上资产存证服务

用户上传文件 → 后端流式计算 SHA-256（文件不落盘）→ 写入以太坊 Sepolia 测试网存证合约（hash + 时间戳）。之后可重新上传文件对比链上 hash，验证文件是否被篡改。

经济闭环：ETH 可链上兑换 DocToken（`DocToken.buyTokens(tokenAmount)`，调用方指定兑换数量，多付的 ETH 找零退回），管理端也可 `mint` 到指定钱包；用户用 DocToken 购买"次数"时**链上销毁**等值代币（`DocToken.burn(amount)` 打进 0 地址，金额 = 单价 × 数量，带 txHash 确认，后端核验 `Transfer(用户→0x0)` 后发放），存证 / 校验各消耗 1 次次数（扣 `t_user.credit_balance`，链上不再销毁）——即购买即全额销毁的预付费模型。

## 变更记录

| 日期 | 版本 | 变更内容 |
| --- | --- | --- |
| 2026-10-12 | v1-MVP | 首个需求完成：后端（Spring Boot 3 + JDK 21 + Spring JDBC + web3j）、Solidity 存证合约（DocToken / DocProof）、数据库 schema、48 个单元测试、初始化 SQL 与 curl 调试脚本。用户端/管理端前端本期不实现。 |
| 2026-10-12 | v1.1 | DocToken 构造器改为显式指定管理端地址 `constructor(address adminAddr)`（原来 owner 默认为部署者），部署时必须填入管理端地址且须与后端 `chain.admin-private-key` 对应地址一致，否则 mint 会被 onlyOwner 拒绝；同步更新部署文档。 |
| 2026-10-13 | v1.2 | `buyTokens` 支持指定兑换数量：签名由 `buyTokens()` 改为 `buyTokens(uint256 tokenAmount)`，按固定汇率计算应付 ETH（tokenAmount × ethPerToken），ETH 不足 revert、多付找零退回；`BuyTokens` 事件的 ethAmount 改为实际消耗额（不含找零）。已重新编译生成 `DocToken_sol_DocToken.bin`，需重新部署 DocToken 合约并更新 `contract.doc-token` 配置。 |
| 2026-10-13 | v1.3 | 管理端前端（Vue 3 + Vite + JavaScript + Element Plus）：登录、统计概览、用户 / 存证 / 发放历史 / 兑换次数 / 次数流水 6 大模块，列表前端分页；新增 2 个后端接口 `GET /admin/stats`、`GET /admin/credit-logs`。 |
| 2026-10-14 | v1.4 | ① 管理端汇率管理：新增 `GET /admin/tokens/exchange-rate`（读链上合约 `ethPerToken`）与 `POST /admin/tokens/exchange-rate`（调合约 `setEthPerToken`，后端校验 >0 且 ≤ 1000 ETH、最多 18 位小数），前端新增"兑换汇率"页面（查看当前汇率 + 二次确认后调整）；② 管理端中英文切换：引入 vue-i18n，全部界面文案（含 Element Plus 组件语言）随右上角切换器即时切换，选择持久化到 localStorage；③ 各 Controller 方法入口增加 debug 日志（方法名 + 非敏感入参，密码 / 钱包签名不打印）。 |
| 2026-10-14 | v1.5 | 管理端存证费用管理：新增 `GET /admin/proofs/cost`（读链上合约 `DocProof.costPerProof`）与 `POST /admin/proofs/cost`（调合约 `setCostPerProof`，后端校验 >0 且 ≤ 1000 DocToken、最多 18 位小数），前端新增"存证费用"页面（查看当前费用 + 二次确认后调整）；同时移除从未使用的 `app.proof.cost-doc-token` 配置（存证消耗始终以链上 `costPerProof` 为准）。 |
| 2026-10-14 | v1.6 | DocProof 构造器改为显式指定管理端地址 `constructor(address _token, uint256 _costPerProof, address _owner)`（原来 owner 默认为部署者），部署时必须填入管理端地址且须与后端 `chain.admin-private-key` 对应地址一致，否则 `setCostPerProof` 会被 onlyOwner 拒绝；已重新编译生成 `DocProof_sol_DocProof.bin`，需重新部署 DocProof 合约并更新 `contract.doc-proof` 配置；同步更新部署文档。 |
| 2026-10-15 | v1.7 | 用户端前端（`client/`，Vue 3 + Vite + Element Plus + vue-i18n，与管理端同技术栈）：① 钱包登录——MetaMask 等 EIP-1193 钱包 `eth_requestAccounts` 授权 + Sepolia 网络校验（不一致自动请求切换）+ 对 `ChainDocProof:<地址>` 做 `personal_sign` 签名登录，token 失效（401）自动清登录态回签名页；② 次数查看与购买——余额卡片（剩余次数 / 单价 / 钱包 DocToken 余额 eth_call 实时查询 / 后端收款地址复制），BigInt 计算转账金额，钱包链上转 DocToken 给收款地址、确认上链后自动提交 txHash 完成购买（txHash 重复提交给幂等提示），余额不足预检与交易失败 / 用户拒绝等错误本地化提示；③ 文件存证两步流程——上传（前端 50MB 校验 + 60s 冷却倒计时）→ 存证上链（扣 1 次，Etherscan 交易链接）；④ 校验——重传文件比对链上 hash（扣 1 次，弹窗提示是否被篡改）；⑤ 存证列表与中英文界面切换。不新增后端接口；钱包交互用原生 `window.ethereum` + 手写 ERC20 transfer/balanceOf ABI 编码纯函数（不引入 ethers）。新增 58 个前端单测（ABI 编解码已知向量与边界、wei 乘法溢出边界、格式化工具、地址/数量校验、safeJsonParse/unwrapResult、钱包错误映射、语言归一化）。 |
| 2026-10-15 | v1.8 | 用户端新增"ETH 兑换 DocToken"：购买页增加兑换卡片——eth_call 直读链上汇率（`DocToken.ethPerToken()`）与 `eth_getBalance` 查钱包 ETH 余额（均可手动刷新），输入 DocToken 数量后 BigInt 计算应付 ETH（数量 × 10^18 × 汇率，与合约 `buyTokens` 算法一致），钱包发送 `buyTokens(uint256)`（msg.value = 应付，多付由合约自动找零）、确认上链后自动刷新汇率 / ETH / DocToken 三项余额；ETH 余额不足预检、交易失败 / 用户拒绝等错误本地化，中英双语；顺手修正购买卡片交易 hash 链接误用地址前缀的问题。不新增后端接口；新增 `encodeBuyTokens`（0x3610724e）/ `encodeEthPerToken`（0xd22bc441）/ `tokensToWei` / `weiToHex` 纯函数。前端单测 58 → 71 个（新增选择子与编码已知向量、tokensToWei 2^256 边界与溢出、weiToHex 边界）。 |
| 2026-10-15 | v1.9 | 修复购买次数偶发"链上未找到匹配的 DocToken 转账记录"：根因是前端在钱包自己的 RPC 上确认交易后立刻提交购买，后端用配置的公共 RPC（默认 publicnode）单次查询交易回执，两个节点对刚出块的交易存在数秒索引延迟，后端查不到回执即误判为无转账。`ChainGateway.hasTransfer` 改为轮询验证（间隔 2s、最多 10 次 ≈ 20s，小于前端 60s 请求超时）：查不到回执 / 回执 logs 为空 → 等下一轮重查；回执失败（revert）、logs 非空但无匹配（金额 / 转账方 / 收款方不符）→ 立即失败不再轮询。新增 `ChainGatewayTest` 9 个单测（首轮即匹配、延迟后出现回执、logs 补齐、各类不匹配立即失败、轮询上限、revert 回执、RPC 异常包装为 500），后端单测 62 → 71 个。 |
| 2026-10-15 | v1.10 | 修复 ETH 兑换 DocToken 金额虚高 10^18 倍（如汇率 0.0001 ETH/枚、兑换 10 枚时界面显示 1000000000000000 ETH，实际应为 0.001 ETH）：根因是 `buyTokens` 定价公式单位不一致——`ethPerToken` 是"每枚 DocToken 对应的 ETH（wei）"，而 `tokenAmount` 是 18 位小数 wei，`tokenAmount × ethPerToken` 漏除 10^18；用户端 BigInt 计算照抄了该错误公式，导致界面显示金额与实际发出的 msg.value 同时虚高 10^18 倍（按原公式交易必然因 ETH 不足 revert）。修复：① 合约 `buyTokens` 定价改为 `tokenAmount * ethPerToken / 10^18`（已重新编译生成 `DocToken_sol_DocToken.bin`）；② 用户端新增 `mulDivWei`（先乘后除，乘法先做 2^256-1 溢出检查、除法向下取整，与 Solidity 无符号整数语义一致）计算应付 ETH，`BuyView` 改用 `数量 wei × 汇率 ÷ 10^18`。**注意：合约公式变更需重新部署**——重新部署 DocToken 后须同步重新部署 DocProof（构造器绑定 DocToken 地址），更新后端 `contract.doc-token` / `contract.doc-proof` 配置与用户端 `client/src/config.js` 的两个地址常量，并在管理端重新设置汇率、向新合约地址 mint 库存。前端新增 11 个单测（用户反馈场景 10 枚×0.0001→0.001 ETH、向下取整、2^256-1 边界、除 0 / 溢出 / 非法输入），前端单测 71 → 79 个。 |
| 2026-10-15 | v1.11 | DocToken 消耗模型改为"购买时销毁"：① 合约 `DocToken` 新增 `burn(uint256)`（`Transfer(用户→0x0)`，删除无人调用的 `burnFrom`）；`DocProof` 去掉 `token/costPerProof/owner/onlyOwner/setCostPerProof/CostUpdated`，构造函数改为无参，`storeProof` 只写链上记录不再销毁代币；② 后端购买次数改为链上核验 `Transfer(用户→0 地址, 单价×数量)`（`hasTransfer` 的 to 由管理地址改为 0 地址常量），`AdminService`/`AdminController` 删除存证费用查询/调整接口与 `ProofCostReq`，`ChainGateway` 删除 `getCostPerProof/setCostPerProof`；③ 用户端购买页由"转 DocToken 给后端收款地址"改为"钱包 `burn` 销毁 DocToken"（新增 `encodeBurn`(0x42966c68)/`burnDocToken`，删除 `encodeTransfer`/收款地址 UI 与文案）；④ 管理端删除"存证费用"页面、路由、API 与文案；⑤ 单测同步更新（后端删存证费用 5 用例、Purchase 校验改 0 地址；前端 ABI 测试转 burn 向量）。**注意：DocToken（新增 burn）与 DocProof（构造函数变更）必须重新部署**，两个新合约地址须同步更新后端 `contract.doc-token`/`doc-proof` 与用户端 `client/src/config.js` 两常量，新 DocToken 需重新 mint 库存并重置汇率；管理钱包不再持有用于销毁的 DocToken。 |
| 2026-10-20 | v1.12 | DocProof 限制为管理端专用并记录真实用户：① 新增 `address public owner` 与 `onlyOwner`，构造函数由无参改为 `constructor(address adminAddr)`（拒绝 0 地址，与 DocToken 风格一致），仅 `storeProof` 加 `onlyOwner`；三个查询函数（proofCount / getProof / getProofIndexByHash）保持公开——只读调用走 eth_call、msg.sender 可任意伪造，加 onlyOwner 无安全意义，还会挡掉第三方链上核查。② `storeProof` 签名改为 `(bytes32 hash, uint256 timestamp, address user)`（拒绝 0 地址用户）：原 `Proof.owner` 字段存 msg.sender，而调用方恒为后端钱包、字段无信息量，改为由后端传入存证用户钱包地址（结构体字段改名 `user`，`ProofStored` 事件同步改名）；后端 `ChainGateway.storeProof`/`ProofService.store` 同步传参，`ProofServiceTest` 2 处 mock 更新。重新编译生成 `DocProof_sol_DocProof.bin`（solc 0.8.24 无优化器，与 DocToken bin 的编译参数校验一致；核对时发现旧 DocProof bin 是带 `owner()` 选择器的历史中间版残留，与当时源码不符，一并修正）。**注意：DocProof 必须重新部署**，构造参数填管理端地址（与 DocToken `adminAddr` 同址），部署后更新后端 `contract.doc-proof`；用户端 `client/src/config.js` 的 `DOC_PROOF_ADDRESS` 目前为空且无代码引用，暂不同步。 |

## 项目结构

```
├── contract/          # Solidity 合约（DocToken / DocProof，每个函数有中文注释）
├── doc/
│   ├── story/         # 需求文档
│   └── plan/          # 实施计划
├── script/
│   ├── curl/          # curl 调试命令（user.sh / admin.sh）
│   └── sql/           # init_admin.sql（初始化管理员）
├── admin/             # 管理端前端（Vue 3 + Vite + Element Plus）
├── client/            # 用户端前端（Vue 3 + Vite + Element Plus，钱包登录 / 购买次数 / 存证 / 校验）
└── server/            # 后端（Spring Boot，包根 dev.vincent.chaindocproof）
    ├── config/        # 配置绑定与 Bean（Web3j、Clock、拦截器注册）
    ├── dao/           # 数据访问层（Spring JDBC）
    ├── interceptor/   # 登录鉴权拦截器
    ├── model/         # 实体与 DTO
    ├── service/       # 业务逻辑（Auth/Purchase/Proof/Admin/ChainGateway）
    ├── support/       # 通用支撑（业务异常、token、hash、地址校验、全局异常）
    └── web/           # Controller 与全局异常处理
```

## 技术选型

- 后端：Java 21 / Spring Boot 3.4 / Spring JDBC / web3j 4.12.3 / BCrypt（spring-security-crypto）
- 数据库：MySQL 8（表名 `t_` 前缀）
- 链：Sepolia 测试网（chainId 11155111），RPC 可配置
- 用户鉴权：钱包对 `ChainDocProof:<地址>` 做 EIP-191 签名，后端 ecrecover 验证后发 token（`Authorization: Bearer <token>`）
- 管理端前端：Vue 3 + Vite + JavaScript + Element Plus + vue-i18n（hash 路由，axios 统一解包 `{code,message,data}`，中英文界面切换）
- 用户端前端：Vue 3 + Vite + JavaScript + Element Plus + vue-i18n（同管理端技术栈；钱包交互用原生 EIP-1193 `window.ethereum` + 手写 ERC20 ABI 编码，不引入 ethers）

## 部署步骤

1. **数据库**：MySQL 中执行
   ```sql
   CREATE DATABASE chaindocproof DEFAULT CHARACTER SET utf8mb4;
   ```
   然后执行 `server/src/main/resources/schema.sql` 建表，执行 `script/sql/init_admin.sql` 创建管理端账户（默认 `admin / admin123`，**务必修改**）。

2. **部署合约**（Sepolia，可用 Remix，顺序不能颠倒）：
   - 先部署 `contract/DocToken.sol`，构造参数 `adminAddr` = 管理端地址（只有该地址能 mint 发币、调汇率；**必须与 `chain.admin-private-key` 对应地址一致**，否则后端 mint 会被拒），记下合约地址；
   - 用管理端调 DocToken 的 `mint(<DocToken 合约地址>, 库存数量)` 给合约 mint 兑换库存（`buyTokens` 从合约自身余额发货，库存不足会 revert）；
   - 再部署 `contract/DocProof.sol`，构造参数 `adminAddr` = 管理端地址（只有该地址能调用 `storeProof` 存证；与 DocToken 的 `adminAddr` 同址，须与 `chain.admin-private-key` 对应地址一致），记下合约地址。

   **合约地址共需填 2 处，重新部署合约时逐一同步更新：**
   1. 后端 `server/src/main/resources/application.properties`：`contract.doc-token` / `contract.doc-proof`；
   2. 用户端 `client/src/config.js`：`DOC_TOKEN_ADDRESS` / `DOC_PROOF_ADDRESS`（用户端查 DocToken 余额 / 链上汇率、发 ETH 兑换与 burn 销毁交易直接读这两个常量）。
   管理端只通过后端接口访问合约，无需填地址。

3. **配置后端** `server/src/main/resources/application.properties`：
   - `spring.datasource.*`：数据库连接；
   - `chain.rpc-url` / `chain.chain-id`：Sepolia RPC 与链 ID；
   - `chain.admin-private-key`：后端/管理端钱包私钥（DocToken 转入该钱包地址对应"购买次数"，`storeProof`/`mint` 由该钱包出 gas）；
   - `contract.doc-token` / `contract.doc-proof`：上面两个合约地址。

4. **启动**：
   ```bash
   cd server
   mvn spring-boot:run
   ```

## 配置项（application.properties）

| 配置 | 说明 | 默认 |
| --- | --- | --- |
| chain.rpc-url | 节点 RPC | Sepolia publicnode |
| chain.chain-id | 链 ID | 11155111 |
| chain.admin-private-key | 后端钱包私钥 | 空（必须填） |
| chain.gas-price-gwei / chain.gas-limit | gas 配置（须 ≥ 存证实际开销约 69 万，且 上限×gasPrice ≤ 后端钱包余额，否则 insufficient funds） | 30 / 850000 |
| contract.doc-token / contract.doc-proof | 合约地址 | 空（必须填） |
| app.credit.unit-price-doc-token | 购买 1 次次数所需 DocToken（wei） | 10 枚 |
| app.credit.purchase-max | 单笔购买次数上限 | 1000 |
| app.upload.cooldown-seconds | 同一用户两次上传最小间隔（秒） | 60 |
| app.admin.login-lock-seconds | 管理端登录失败全局锁定（秒） | 60 |
| app.token.ttl-hours | 登录 token 有效期（小时） | 168 |

## 接口一览

统一返回 `{"code":0,"message":"ok","data":...}`，`code != 0` 即业务错误（401 未登录 / 404 不存在 / 409 冲突 / 429 限流）。

### 用户端（`/api/user/**`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/user/auth/login | 钱包签名登录 `{address, signature}` → `{token}` |
| GET | /api/user/credits | 查询剩余次数 |
| POST | /api/user/credits/purchase | 购买次数 `{quantity, txHash}`（先链上 `burn` 销毁 DocToken 打进 0 地址） |
| POST | /api/user/proofs/upload | 上传文件（multipart `file`，冷却 60s，返回 PENDING 记录） |
| POST | /api/user/proofs/{id}/store | 存证上链（扣 1 次） |
| POST | /api/user/proofs/{id}/verify | 校验文件是否被篡改（multipart `file`，扣 1 次，data=true 未篡改） |
| GET | /api/user/proofs | 我的存证列表 |

### 管理端（`/admin/**`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /admin/auth/login | 账密登录（失败全局锁定 60s） |
| GET | /admin/users | 普通用户列表（余额/累计购买） |
| GET | /admin/proofs?address= | 存证记录（可按用户筛选） |
| GET | /admin/tokens/mints | DocToken 管理端发放记录 |
| GET | /admin/tokens/exchanges | DocToken 的 ETH 链上兑换次数（读合约） |
| GET | /admin/tokens/exchange-rate | 当前 ETH 兑换汇率 `{ethPerToken}`（读合约，wei） |
| POST | /admin/tokens/exchange-rate | 调整汇率 `{rate}`（1 枚 DocToken = rate ETH，十进制字符串，最多 18 位小数，>0 且 ≤ 1000）→ `{txHash}` |
| GET | /admin/stats | 统计概览（用户数 / 存证数 / 累计购买 / 发放次数与总量 / 链上兑换次数） |
| GET | /admin/credit-logs?address=&type= | 次数流水（可按地址 / 类型筛选：购买 / 存证 / 校验消耗） |
| POST | /admin/tokens/mint | 给指定钱包 mint DocToken `{toAddress, amount}` |

调试命令见 `script/curl/user.sh` 与 `script/curl/admin.sh`。

## 测试

```bash
cd server
mvn test
```

覆盖：上传冷却 59s/60s 边界、SHA-256 已知向量与缓冲区边界、存证扣次数/链上失败回补、校验一致与篡改、购买数量边界/txHash 幂等/链上销毁金额校验、管理端登录锁定边界/mint 参数边界、统计与流水查询边界、汇率查询/调整（非正数、1000 ETH 上下界）边界、钱包签名登录全分支（真实 web3j 签名）。

### 管理端前端（`admin/`）

```bash
cd admin
npm install
npm test        # Vitest 纯函数单测（format/validate/safeJsonParse/unwrapResult/i18n 中英文案一致性）
npm run build   # 生产构建
npm run dev     # 开发（代理 /admin → 127.0.0.1:8080）
```

### 用户端前端（`client/`）

```bash
cd client
npm install
npm test        # Vitest 纯函数单测 77 个（ERC20 ABI 编解码/wei 乘除/format/validate/safeJsonParse/unwrapResult/钱包错误映射/i18n）
npm run build   # 生产构建
npm run dev     # 开发（代理 /api → 127.0.0.1:8080，需安装 MetaMask 并切到 Sepolia）
```
