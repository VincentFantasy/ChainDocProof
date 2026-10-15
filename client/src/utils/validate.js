/**
 * 表单校验工具（纯函数，可单测）
 */

/** 是否为合法的以太坊地址：0x 开头 + 40 位十六进制 */
export function isAddress(value) {
  if (typeof value !== 'string') return false;
  return /^0x[0-9a-fA-F]{40}$/.test(value.trim());
}

/**
 * 是否为合法的购买次数：1 ~ max 之间的整数（el-input-number 清空调 null，这里也拒绝）
 *
 * @param value 输入值（字符串或数字）
 * @param max   上限（默认 1000，与后端 app.credit.purchase-max 一致）
 */
export function isQuantity(value, max = 1000) {
  if (typeof value === 'number') {
    return Number.isInteger(value) && value >= 1 && value <= max;
  }
  if (typeof value !== 'string') return false;
  const v = value.trim();
  if (!/^\d+$/.test(v)) return false;
  const n = Number(v);
  return Number.isFinite(n) && n >= 1 && n <= max;
}
