/**
 * 用户端公开常量。
 * 链 id 与合约地址均为链上公开信息，用户钱包操作（登录签名、转 DocToken、查余额）需要用到；
 * 与 server/src/main/resources/application.properties 中 chain.* / contract.* 保持一致。
 */

/** Sepolia 测试网 chainId */
export const CHAIN_ID = 11155111;

/** DocToken（ERC20 凭证代币）合约地址 */
export const DOC_TOKEN_ADDRESS = '';

/** DocProof（存证）合约地址，仅用于 Etherscan 链接展示 */
export const DOC_PROOF_ADDRESS = '';

/** Sepolia Etherscan 链接前缀 */
export const EXPLORER_TX_URL = 'https://sepolia.etherscan.io/tx/';
export const EXPLORER_ADDRESS_URL = 'https://sepolia.etherscan.io/address/';

/** EIP-191 登录签名消息前缀，必须与后端 AuthService 一致：消息 = 前缀 + 地址 */
export const SIGN_PREFIX = 'ChainDocProof:';

/** 单文件上传大小上限（字节），与后端 spring.servlet.multipart.max-file-size=50MB 一致 */
export const MAX_FILE_SIZE = 50 * 1024 * 1024;

/** 单笔购买次数上限，与后端 app.credit.purchase-max 一致 */
export const MAX_PURCHASE_QUANTITY = 1000;

/** 上传冷却秒数，与后端 app.upload.cooldown-seconds 一致（上传成功后前端主动倒计时用） */
export const UPLOAD_COOLDOWN_SECONDS = 60;
