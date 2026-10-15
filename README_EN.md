# ChainDocProof — On-Chain Document Notarization Service

A user uploads a file, the backend streams it to compute a SHA-256 digest (the file is never written to disk), and the digest plus a timestamp is written to a notarization contract on the Ethereum Sepolia testnet. The file can later be re-uploaded and its hash compared against the on-chain value to verify it has not been tampered with.

Economic loop: ETH can be exchanged on-chain for DocToken (`DocToken.buyTokens(tokenAmount)` — the caller specifies the amount, and any excess ETH is refunded), and the admin can also `mint` tokens to any wallet. Users transfer DocToken to the backend address to buy "credits" (confirmed with a txHash); notarization and verification each consume 1 credit, and each notarization burns the corresponding DocToken (sent to the zero address).

## Project Structure

```
├── contract/          # Solidity contracts (DocToken / DocProof, every function has a comment)
├── doc/
│   ├── story/         # Requirement documents
│   └── plan/          # Implementation plans
├── script/
│   ├── curl/          # curl debug commands (user.sh / admin.sh)
│   └── sql/           # init_admin.sql (initialize the admin account)
├── admin/             # Admin frontend (Vue 3 + Vite + Element Plus)
├── client/            # User frontend (Vue 3 + Vite + Element Plus; wallet login / buy credits / notarize / verify)
└── server/            # Backend (Spring Boot, package root dev.vincent.chaindocproof)
    ├── config/        # Config binding and beans (Web3j, Clock, interceptor registration)
    ├── dao/           # Data access layer (Spring JDBC)
    ├── interceptor/   # Login authentication interceptor
    ├── model/         # Entities and DTOs
    ├── service/       # Business logic (Auth/Purchase/Proof/Admin/ChainGateway)
    ├── support/       # Shared support (business exceptions, token, hash, address validation, global exception handling)
    └── web/           # Controllers and global exception handling
```

## Tech Stack

- Backend: Java 21 / Spring Boot 3.4 / Spring JDBC / web3j 4.12.3 / BCrypt (spring-security-crypto)
- Database: MySQL 8 (table names prefixed with `t_`)
- Chain: Sepolia testnet (chainId 11155111), RPC configurable
- User auth: the wallet signs `ChainDocProof:<address>` with EIP-191; the backend verifies via ecrecover and issues a token (`Authorization: Bearer <token>`)
- Admin frontend: Vue 3 + Vite + JavaScript + Element Plus + vue-i18n (hash routing, axios unwraps `{code,message,data}` centrally, EN/ZH UI switch)
- User frontend: Vue 3 + Vite + JavaScript + Element Plus + vue-i18n (same stack as the admin frontend; wallet interaction uses the native EIP-1193 `window.ethereum` with hand-written ERC20 ABI encoding, no ethers dependency)

## Deployment Steps

1. **Database**: run the following in MySQL:
   ```sql
   CREATE DATABASE chaindocproof DEFAULT CHARACTER SET utf8mb4;
   ```
   Then execute `server/src/main/resources/schema.sql` to create the tables, and run `script/sql/init_admin.sql` to create the admin account (default `admin / admin123` — **change it**).

2. **Deploy the contracts** (on Sepolia, Remix works fine; the deployment order matters):
   - First deploy `contract/DocToken.sol`, with constructor argument `adminAddr` = the admin address (only this address can `mint` tokens and call `setEthPerToken`; it **must match the address of `chain.admin-private-key`**, otherwise backend `mint` calls will be rejected). Record the contract address → set `contract.doc-token`.
   - Using the admin address, call `mint(backend wallet address, stock amount)` to inject exchange stock into the DocToken contract (or mint to any address). To hold exchange stock in the DocToken contract itself, use `mint(<DocToken contract address>, stock)`.
   - Then deploy `contract/DocProof.sol`, with constructor arguments:
     - `_token` = the DocToken contract address;
     - `_costPerProof` = DocToken consumed per notarization in wei (18 decimals, e.g. 1 token = `1000000000000000000`; this is only the initial value, adjustable later on the admin "Proof Cost" page);
     - `_owner` = the admin address (explicitly specified by the deployer; it must match the address of `chain.admin-private-key`, otherwise `setCostPerProof` will be rejected by `onlyOwner`).
     
     Record the contract address → set `contract.doc-proof`.

   **Contract addresses must be filled in 3 places — update all of them on every redeploy:**
   1. The `DocProof` constructor argument `_token` = the DocToken contract address (at DocProof deploy time);
   2. `server/src/main/resources/application.properties`: `contract.doc-token` / `contract.doc-proof`;
   3. `client/src/config.js`: `DOC_TOKEN_ADDRESS` / `DOC_PROOF_ADDRESS` (the user client reads these constants directly for DocToken balance / on-chain rate lookups and ETH-exchange transactions).
   The admin frontend only accesses the contracts through backend APIs, so no address is needed there.

3. **Configure the backend** in `server/src/main/resources/application.properties`:
   - `spring.datasource.*`: database connection;
   - `chain.rpc-url` / `chain.chain-id`: Sepolia RPC and chain ID;
   - `chain.admin-private-key`: the backend/admin wallet private key (DocToken transferred to this wallet address counts as "buying credits"; `storeProof`/`mint` spend gas from this wallet; its address must equal the `adminAddr` used in step 2);
   - `contract.doc-token` / `contract.doc-proof`: the two contract addresses from above;
   - `chain.gas-price-gwei` / `chain.gas-limit`: adjust as needed.

4. **Start**:
   ```bash
   cd server
   mvn spring-boot:run
   ```

## User / Backend Wallet Preparation

- Buying credits: transfer DocToken from your wallet to the **backend wallet address** (returned as `backendAddress` by `GET /api/user/credits`), then call `POST /api/user/credits/purchase` with the transfer transaction hash.
- Notarization: the backend wallet must hold DocToken and have called `approve(DocProof contract, amount)`; when the backend calls `storeProof`, the contract automatically burns the tokens.

## Configuration (application.properties)

| Key | Description | Default |
| --- | --- | --- |
| chain.rpc-url | Node RPC | Sepolia publicnode |
| chain.chain-id | Chain ID | 11155111 |
| chain.admin-private-key | Backend wallet private key | empty (required) |
| chain.gas-price-gwei / chain.gas-limit | Gas settings | 30 / 300000 |
| contract.doc-token / contract.doc-proof | Contract addresses | empty (required) |
| app.credit.unit-price-doc-token | DocToken (wei) required to buy 1 credit | 10 tokens |
| app.credit.purchase-max | Max credits per single purchase | 1000 |
| app.upload.cooldown-seconds | Minimum interval between two uploads by the same user (seconds) | 60 |
| app.admin.login-lock-seconds | Global lock after a failed admin login (seconds) | 60 |
| app.token.ttl-hours | Login token time-to-live (hours) | 168 |

## API Overview

All responses are `{"code":0,"message":"ok","data":...}`; `code != 0` means a business error (401 not logged in / 404 not found / 409 conflict / 429 rate limited).

### User endpoints (`/api/user/**`)

| Method | Path | Description |
| --- | --- | --- |
| POST | /api/user/auth/login | Wallet signature login `{address, signature}` → `{token}` |
| GET | /api/user/credits | Query remaining credits |
| POST | /api/user/credits/purchase | Buy credits `{quantity, txHash}` (transfer DocToken to the backend address first) |
| POST | /api/user/proofs/upload | Upload a file (multipart `file`, 60s cooldown, returns a PENDING record) |
| POST | /api/user/proofs/{id}/store | Notarize on-chain (consumes 1 credit) |
| POST | /api/user/proofs/{id}/verify | Verify whether a file is tampered (multipart `file`, consumes 1 credit, data=true means unmodified) |
| GET | /api/user/proofs | My notarization list |

### Admin endpoints (`/admin/**`)

| Method | Path | Description |
| --- | --- | --- |
| POST | /admin/auth/login | Username/password login (global 60s lock on failure) |
| GET | /admin/users | Regular user list (balance / cumulative purchases) |
| GET | /admin/proofs?address= | Notarization records (filterable by user) |
| GET | /admin/tokens/mints | DocToken admin mint records |
| GET | /admin/tokens/exchanges | On-chain ETH exchange count for DocToken (reads the contract) |
| GET | /admin/tokens/exchange-rate | Current ETH exchange rate `{ethPerToken}` (reads the contract, wei) |
| POST | /admin/tokens/exchange-rate | Adjust the rate `{rate}` (1 DocToken = rate ETH, decimal string, max 18 decimals, >0 and ≤ 1000) → `{txHash}` |
| GET | /admin/proofs/cost | Current notarization cost `{costPerProof}` (reads the contract, wei) |
| POST | /admin/proofs/cost | Adjust the notarization cost `{amount}` (amount DocToken per notarization, decimal string, max 18 decimals, >0 and ≤ 1000) → `{txHash}` |
| GET | /admin/stats | Stats overview (user count / notarization count / cumulative purchases / mint count and total / on-chain exchange count) |
| GET | /admin/credit-logs?address=&type= | Credit ledger (filterable by address / type: purchase / notarization / verification) |
| POST | /admin/tokens/mint | Mint DocToken to a given wallet `{toAddress, amount}` |

Debug commands: see `script/curl/user.sh` and `script/curl/admin.sh`.

## Tests

```bash
cd server
mvn test
```

Coverage: 59s/60s upload cooldown boundaries, SHA-256 known vectors and buffer boundaries, credit deduction / on-chain failure refund for notarization, matching and tampered verification, purchase quantity boundaries / txHash idempotency / on-chain amount validation, admin login lock boundaries / mint parameter boundaries, stats and ledger query boundaries, exchange rate query/adjustment boundaries (non-positive, 1000 ETH upper/lower bounds), notarization cost query/adjustment boundaries (non-positive, 1000 DocToken upper/lower bounds), and all branches of wallet signature login (real web3j signatures).

### Admin frontend (`admin/`)

```bash
cd admin
npm install
npm test        # Vitest pure-function unit tests (format/validate/safeJsonParse/unwrapResult/i18n EN-ZH consistency)
npm run build   # Production build
npm run dev     # Development (proxies /admin → 127.0.0.1:8080)
```

### User frontend (`client/`)

```bash
cd client
npm install
npm test        # 58 Vitest pure-function unit tests (ERC20 ABI encode/decode, wei multiplication, format, validate, safeJsonParse, unwrapResult, wallet error mapping, i18n)
npm run build   # Production build
npm run dev     # Development (proxies /api → 127.0.0.1:8080; requires MetaMask set to Sepolia)
```
