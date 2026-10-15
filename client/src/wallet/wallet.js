/**
 * 钱包交互封装：基于浏览器原生 EIP-1193（window.ethereum，MetaMask 等），不引入 ethers。
 * 失败时抛出带机器可读 message 的错误（NO_WALLET / WRONG_CHAIN / NO_ACCOUNT / TX_TIMEOUT），
 * 调用方用 mapWalletError(e, t) 转成本地化文案展示；EIP-1193 用户拒绝错误 code=4001 同样映射。
 */
import { CHAIN_ID, DOC_TOKEN_ADDRESS, SIGN_PREFIX } from '../config.js';
import { toBigInt } from '../utils/wei.js';
import { encodeBalanceOf, encodeBuyTokens, encodeEthPerToken, encodeBurn, decodeUint256 } from './abi.js';

/** 获取钱包 provider，未安装钱包抛 NO_WALLET */
function getProvider() {
  if (typeof window === 'undefined' || !window.ethereum) {
    throw new Error('NO_WALLET');
  }
  return window.ethereum;
}

/**
 * UTF-8 文本转 0x 十六进制（纯函数，可单测）。
 * personal_sign 的第一个参数是消息文本的 hex（钱包内部再按 EIP-191 加前缀哈希），不能传已哈希值。
 *
 * @param {string} text 明文字符串
 * @returns {string} 0x 开头十六进制
 */
export function textToHex(text) {
  return '0x' + Array.from(new TextEncoder().encode(text))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * 请求账户授权（首次会弹钱包授权窗口），返回第一个账户地址
 * @returns {Promise<string>} 钱包地址
 */
export async function getAccounts() {
  const accounts = await getProvider().request({ method: 'eth_requestAccounts' });
  if (!accounts || !accounts.length) {
    throw new Error('NO_ACCOUNT');
  }
  return accounts[0];
}

/**
 * 确保钱包网络是目标链（Sepolia）；不一致时尝试让钱包切换，失败 / 未添加该链抛 WRONG_CHAIN
 */
export async function ensureChain() {
  const hex = await getProvider().request({ method: 'eth_chainId' });
  const id = parseInt(hex, 16);
  if (id === CHAIN_ID) return;
  try {
    await getProvider().request({
      method: 'wallet_switchEthereumChain',
      params: [{ chainId: '0x' + CHAIN_ID.toString(16) }],
    });
  } catch {
    throw new Error('WRONG_CHAIN');
  }
}

/**
 * 对登录消息 "ChainDocProof:" + address 做 EIP-191 个人签名（personal_sign），与后端 AuthService 约定一致
 *
 * @param {string} address 钱包地址
 * @returns {Promise<string>} 0x 开头十六进制签名
 */
export async function signLoginMessage(address) {
  return getProvider().request({
    method: 'personal_sign',
    params: [textToHex(SIGN_PREFIX + address), address],
  });
}

/**
 * 发起 DocToken 链上销毁（burn 打进 0 地址，购买次数用），钱包弹确认并付 gas
 *
 * @param {string} amountWei 销毁数量 wei 十进制字符串
 * @returns {Promise<string>} 交易 hash
 */
export async function burnDocToken(amountWei) {
  const data = encodeBurn(amountWei);
  const accounts = await getProvider().request({ method: 'eth_requestAccounts' });
  return getProvider().request({
    method: 'eth_sendTransaction',
    params: [{ from: accounts[0], to: DOC_TOKEN_ADDRESS, data, value: '0x0' }],
  });
}

/**
 * 查询地址的 DocToken 余额（eth_call balanceOf）
 *
 * @param {string} address 钱包地址
 * @returns {Promise<string>} 余额 wei 十进制字符串
 */
export async function docTokenBalanceOf(address) {
  const data = encodeBalanceOf(address);
  const hex = await getProvider().request({
    method: 'eth_call',
    params: [{ to: DOC_TOKEN_ADDRESS, data }, 'latest'],
  });
  return decodeUint256(hex);
}

/**
 * wei 十进制字符串转 0x 十六进制（eth_sendTransaction 的 value / msg.value 用）（纯函数，可单测）
 *
 * @param {string} wei 非负 wei 十进制字符串
 * @returns {string} 0x 开头十六进制
 */
export function weiToHex(wei) {
  return '0x' + toBigInt(wei).toString(16);
}

/**
 * 读取 DocToken 链上汇率（eth_call ethPerToken()，链上公开值，1 枚 DocToken = X wei ETH）
 *
 * @returns {Promise<string>} 汇率 wei 十进制字符串
 */
export async function docTokenEthPerToken() {
  const hex = await getProvider().request({
    method: 'eth_call',
    params: [{ to: DOC_TOKEN_ADDRESS, data: encodeEthPerToken() }, 'latest'],
  });
  return decodeUint256(hex);
}

/**
 * 查询钱包 ETH 原生余额（eth_getBalance；返回值是 hex wei 数字，不是 ABI 编码，直接 BigInt 转换）
 *
 * @param {string} address 钱包地址
 * @returns {Promise<string>} 余额 wei 十进制字符串
 */
export async function ethBalanceOf(address) {
  const hex = await getProvider().request({
    method: 'eth_getBalance',
    params: [address, 'latest'],
  });
  return BigInt(hex).toString();
}

/**
 * 用 ETH 兑换 DocToken（DocToken buyTokens）：交易随附 ETH（msg.value = 应付金额），
 * 合约按固定汇率兑换，多付的 ETH 由合约自动找零退回
 *
 * @param {string} tokenAmountWei 兑换数量 wei 十进制字符串
 * @param {string} costWei 应付 ETH wei 十进制字符串（= 数量 wei × 汇率 ÷ 10^18，与合约 buyTokens 一致）
 * @returns {Promise<string>} 交易 hash
 */
export async function buyDocTokenWithEth(tokenAmountWei, costWei) {
  const data = encodeBuyTokens(tokenAmountWei);
  const accounts = await getProvider().request({ method: 'eth_requestAccounts' });
  return getProvider().request({
    method: 'eth_sendTransaction',
    params: [{ from: accounts[0], to: DOC_TOKEN_ADDRESS, data, value: weiToHex(costWei) }],
  });
}

/**
 * 轮询交易回执直到链上确认（测试网公共 RPC 确认可能较慢）
 *
 * @param {string} txHash 交易 hash
 * @param {number} intervalMs 轮询间隔，默认 4 秒
 * @param {number} timeoutMs  超时，默认 10 分钟，超时抛 TX_TIMEOUT
 * @returns {Promise<object>} 交易回执（status 0x1 成功 / 0x0 失败）
 */
export async function waitForTx(txHash, intervalMs = 4000, timeoutMs = 10 * 60 * 1000) {
  const start = Date.now();
  for (;;) {
    const receipt = await getProvider().request({
      method: 'eth_getTransactionReceipt',
      params: [txHash],
    });
    if (receipt) return receipt;
    if (Date.now() - start > timeoutMs) {
      throw new Error('TX_TIMEOUT');
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }
}

/**
 * 把钱包交互错误映射为本地化提示文案（纯函数，可单测）
 *
 * @param {Error} e 错误（可能来自 EIP-1193 或本模块）
 * @param {Function} t i18n 的 t 函数
 * @returns {string} 展示用文案
 */
export function mapWalletError(e, t) {
  const msg = (e && e.message) || '';
  if (msg === 'NO_WALLET') return t('wallet.noWallet');
  if (msg === 'WRONG_CHAIN') return t('wallet.wrongChain');
  if (msg === 'NO_ACCOUNT') return t('wallet.noAccount');
  if (msg === 'TX_TIMEOUT') return t('wallet.txTimeout');
  if (e && e.code === 4001) return t('wallet.rejected');
  return msg || t('wallet.failed');
}
