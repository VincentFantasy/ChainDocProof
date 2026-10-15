/**
 * 表单校验工具（纯函数，可单测）
 */

/** 是否为合法的以太坊地址：0x 开头 + 40 位十六进制 */
export function isAddress(value) {
  if (typeof value !== 'string') return false;
  return /^0x[0-9a-fA-F]{40}$/.test(value.trim());
}

/** 是否为合法的正数量：非空、可解析为 >0 的数，且不含非法字符 */
export function isPositiveAmount(value) {
  if (typeof value !== 'string') return false;
  const v = value.trim();
  if (!v || !/^\d+(\.\d+)?$/.test(v)) return false;
  const n = Number(v);
  return Number.isFinite(n) && n > 0;
}

/** 是否为合法的 wei 十进制数量（汇率 / 存证费用等）：正数且最多 18 位小数（与后端 wei 精度一致，超出会被后端拒绝） */
export function isAmount(value) {
  if (typeof value !== 'string') return false;
  const v = value.trim();
  if (!v || !/^\d+(\.\d{1,18})?$/.test(v)) return false;
  const n = Number(v);
  return Number.isFinite(n) && n > 0;
}
