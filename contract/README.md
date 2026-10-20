# 合约部署说明

测试网：Sepolia（链 ID 11155111），可用 Remix / Hardhat 等任意工具部署。

## 部署顺序

1. 部署 `DocToken.sol`，构造参数 `adminAddr` = 管理端地址，记录合约地址 → 配置 `contract.doc-token`
   - 只有 `adminAddr` 能调用 `mint`（发币）和 `setEthPerToken`（调汇率）
   - `adminAddr` 必须与后端 `chain.admin-private-key` 对应的公钥地址一致（后端 mint 由该钱包发出）
2. 用管理端调用 `mint(后端钱包地址, 库存数量)` 给 DocToken 合约注入兑换库存（或 mint 到任意地址）
   - 兑换库存放在 DocToken 合约自身地址：`mint(<DocToken合约地址>, 库存)`
3. 部署 `DocProof.sol`，构造参数 `adminAddr` = 管理端地址，记录合约地址 → 配置 `contract.doc-proof`
   - 只有 `adminAddr` 能调用 `storeProof` 存证；查询函数（proofCount / getProof / getProofIndexByHash）保持公开
   - `adminAddr` 必须与后端 `chain.admin-private-key` 对应的公钥地址一致（后端 storeProof 由该钱包发出），与第 1 步 DocToken 的 `adminAddr` 同址
4. 后端配置 `chain.admin-private-key` 填写管理端/后端钱包私钥（其地址必须等于第 1 步的 `adminAddr`），
   `chain.gas-price-gwei`、`chain.gas-limit` 按需调整

## 用户/后端钱包准备

- 用户购买次数：用钱包调 DocToken 的 `burn(amount)` 把等值代币**销毁**打进 0 地址（`GET /api/user/credits` 返回单价，金额 = 单价 × 数量），
  再带销毁交易 hash 调 `POST /api/user/credits/purchase`
- 存证：`storeProof` 只写入链上记录、不消耗代币，后端钱包无需持有 DocToken 或 `approve`，仅由该钱包出 gas；链上记录的 `user` 字段为后端传入的存证用户钱包地址（msg.sender 恒为管理端钱包，不用于标识用户）
