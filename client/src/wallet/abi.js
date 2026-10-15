/**
 * 最小 ABI 编解码（纯函数，可单测）。
 * 用户端需要：ERC20 balanceOf(address)、burn(uint256)（购买次数时销毁自己的 DocToken），
 * 以及 DocToken 的 buyTokens(uint256)（ETH 兑换）、ethPerToken()（汇率 getter），
 * 手写编码即可，不引入 ethers。
 * 选择子为函数签名 keccak256 的前 4 字节，与 EIP 标准实现一致。
 */

const MAX_UINT256 = 2n ** 256n - 1n;

/** burn(uint256) 选择子（销毁自己的 DocToken） */
export const BURN_SELECTOR = '0x42966c68';
/** balanceOf(address) 选择子 */
export const BALANCE_OF_SELECTOR = '0x70a08231';
/** buyTokens(uint256) 选择子（DocToken ETH 兑换） */
export const BUY_TOKENS_SELECTOR = '0x3610724e';
/** ethPerToken() 选择子（DocToken 汇率 getter） */
export const ETH_PER_TOKEN_SELECTOR = '0xd22bc441';

/**
 * 地址编码为 32 字节 word（左补 24 个 0）
 *
 * @param {string} address 0x + 40 位十六进制
 * @returns {string} 64 位十六进制（无 0x 前缀）
 * @throws 非法地址
 */
function encodeAddressWord(address) {
  const s = String(address).trim();
  if (!/^0x[0-9a-fA-F]{40}$/.test(s)) {
    throw new Error('非法地址: ' + s);
  }
  return '0'.repeat(24) + s.slice(2).toLowerCase();
}

/**
 * uint256 编码为 32 字节 word（左补 0）
 *
 * @param value 非负十进制字符串 / 整数
 * @returns {string} 64 位十六进制（无 0x 前缀）
 * @throws 非法输入或超过 2^256-1
 */
function encodeUint256Word(value) {
  if (typeof value === 'number') {
    if (!Number.isInteger(value) || value < 0) {
      throw new Error('非法 uint256: ' + value);
    }
  } else {
    const s = String(value).trim();
    if (!/^\d+$/.test(s)) {
      throw new Error('非法 uint256: ' + s);
    }
  }
  const n = typeof value === 'number' ? BigInt(value) : BigInt(String(value).trim());
  if (n > MAX_UINT256) {
    throw new Error('uint256 溢出: ' + value);
  }
  return n.toString(16).padStart(64, '0');
}

/**
 * 编码 DocToken burn(amountWei)，返回完整 calldata（选择子 + 1 个 32 字节 word）。
 * 购买次数时用户销毁自己的 DocToken（打进 0 地址）
 *
 * @param {string} amountWei 销毁数量 wei 十进制字符串
 * @returns {string} 0x 开头的十六进制 calldata
 */
export function encodeBurn(amountWei) {
  return BURN_SELECTOR + encodeUint256Word(amountWei);
}

/**
 * 编码 ERC20 balanceOf(owner) 的 eth_call 数据
 *
 * @param {string} owner 查询余额的地址
 * @returns {string} 0x 开头的十六进制调用数据
 */
export function encodeBalanceOf(owner) {
  return BALANCE_OF_SELECTOR + encodeAddressWord(owner);
}

/**
 * 编码 DocToken buyTokens(tokenAmountWei) 的 calldata（ETH 兑换，合约按固定汇率兑换、多付自动找零）
 *
 * @param {string} tokenAmountWei 兑换数量 wei 十进制字符串（1 枚 DocToken = 10^18 wei）
 * @returns {string} 0x 开头十六进制 calldata
 */
export function encodeBuyTokens(tokenAmountWei) {
  return BUY_TOKENS_SELECTOR + encodeUint256Word(tokenAmountWei);
}

/**
 * 编码 DocToken ethPerToken() 的 eth_call 数据（读链上汇率，1 枚 DocToken = X ETH）
 *
 * @returns {string} 0x 开头十六进制调用数据（无参数，仅选择子）
 */
export function encodeEthPerToken() {
  return ETH_PER_TOKEN_SELECTOR;
}

/**
 * 解码 eth_call 返回值中的 uint256，返回十进制字符串（纯函数，可单测）
 *
 * @param {string} hex 0x 开头十六进制（长度 ≤ 64，空串视为 0）
 * @returns {string} 十进制字符串
 * @throws 非十六进制或超过 32 字节
 */
export function decodeUint256(hex) {
  if (typeof hex !== 'string' || !/^0x[0-9a-fA-F]*$/.test(hex)) {
    throw new Error('非法十六进制: ' + hex);
  }
  const s = hex.slice(2);
  if (s.length > 64) {
    throw new Error('uint256 超长: ' + hex);
  }
  return (s ? BigInt('0x' + s) : 0n).toString();
}
