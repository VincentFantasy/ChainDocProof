/**
 * DocToken wei（18 位小数）BigInt 运算（纯函数，可单测）。
 * 购买金额 = 数量 × 单价，两者相加可达 1e22 量级，超过 JS Number 的 2^53 精度，必须用 BigInt；
 * 链上 uint256 上限 2^256-1，乘积溢出时抛错避免发出错误金额的交易。
 */

const MAX_UINT256 = 2n ** 256n - 1n;

/** 每枚代币对应的 wei（DocToken 为 18 位小数） */
export const WEI_PER_TOKEN = 10n ** 18n;

/**
 * 把非负十进制字符串 / 整数转成 BigInt（纯函数，可单测）
 *
 * @param value 非负整数（字符串或数字）
 * @returns {bigint}
 * @throws 非法输入（负数、小数、非数字字符）
 */
export function toBigInt(value) {
  if (typeof value === 'number') {
    if (!Number.isInteger(value) || value < 0) {
      throw new Error('非法 wei 值: ' + value);
    }
    return BigInt(value);
  }
  const s = String(value).trim();
  if (!/^\d+$/.test(s)) {
    throw new Error('非法 wei 值: ' + s);
  }
  return BigInt(s);
}

/**
 * 两个非负整数 wei 字符串相乘，返回乘积的 wei 字符串（纯函数，可单测）
 *
 * @param a wei 十进制字符串（如单价 unitPriceDocToken）
 * @param b wei 十进制字符串（如购买数量）
 * @returns {string} 乘积 wei 字符串
 * @throws 非法输入或乘积超过 2^256-1
 */
export function mulWei(a, b) {
  const x = toBigInt(a);
  const y = toBigInt(b);
  if (x === 0n || y === 0n) return '0';
  const r = x * y;
  if (r > MAX_UINT256) {
    throw new Error('wei 乘法溢出');
  }
  return r.toString();
}

/**
 * 先乘后除：a × b ÷ d（BigInt 精确运算，纯函数，可单测）
 * 用于「数量(wei) × 每枚汇率(wei) ÷ 10^18」这类换算（ETH 兑换 DocToken 的应付金额）。
 * 与合约 uint256 语义保持一致：乘法先做 2^256-1 溢出检查（Solidity 同样在乘法处回滚），
 * 除法向下取整（与 Solidity 无符号整数除法一致）。
 *
 * @param a 非负整数（wei 十进制字符串或数字）
 * @param b 非负整数（wei 十进制字符串或数字，如汇率）
 * @param d 正整数除数（BigInt 或十进制字符串，如 10^18）
 * @returns {string} 商的十进制字符串（向下取整）
 * @throws 非法输入、除数为 0 或乘积超过 2^256-1
 */
export function mulDivWei(a, b, d) {
  const x = toBigInt(a);
  const y = toBigInt(b);
  const divisor = typeof d === 'bigint' ? d : toBigInt(d);
  if (divisor === 0n) {
    throw new Error('wei 除法：除数不能为 0');
  }
  if (x === 0n || y === 0n) return '0';
  const product = x * y;
  if (product > MAX_UINT256) {
    throw new Error('wei 乘法溢出');
  }
  return (product / divisor).toString();
}

/**
 * 代币数量（整数）转 wei 字符串（×10^18，DocToken 为 18 位小数）（纯函数，可单测）
 *
 * @param amount 代币数量（非负整数，字符串或数字）
 * @returns {string} wei 十进制字符串
 * @throws 非法输入或乘积超过 2^256-1
 */
export function tokensToWei(amount) {
  const n = toBigInt(amount);
  if (n === 0n) return '0';
  const r = n * WEI_PER_TOKEN;
  if (r > MAX_UINT256) {
    throw new Error('wei 换算溢出');
  }
  return r.toString();
}
