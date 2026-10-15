/**
 * 展示用格式化工具（纯函数，可单测）。
 * 金额相关不做 wei 换算，仅把后端返回的 wei（BigInteger）字符串转成 DocToken 展示，
 * 全程用字符串运算，避免 JS Number 超过 2^53 时精度丢失，也不引入大数库。
 */

/**
 * 把 wei（18 位小数）转成 DocToken 十进制字符串展示
 *
 * @param value   wei 值（字符串或数字；后端 BigInteger 经安全解析后多为字符串）
 * @param decimals 小数位数，默认 18
 * @returns {string} 去掉尾部多余的 0，例如 "10" / "0.5" / "1.25"
 */
export function formatWei(value, decimals = 18) {
  if (value === null || value === undefined || value === '') {
    return '0';
  }
  let s = String(value).trim();
  let negative = false;
  if (s.startsWith('-')) {
    negative = true;
    s = s.slice(1);
  }
  if (!/^\d+$/.test(s)) {
    // 非纯整数（意外输入），原样返回，避免误算
    return (negative ? '-' : '') + s;
  }
  const padLen = decimals + 1;
  if (s.length < padLen) {
    s = s.padStart(padLen, '0');
  }
  const intPart = s.slice(0, s.length - decimals).replace(/^0+(?=\d)/, '') || '0';
  const fracPart = s.slice(s.length - decimals).replace(/0+$/, '');
  let result = intPart;
  if (fracPart) {
    result += '.' + fracPart;
  }
  return (negative ? '-' : '') + result;
}

/** 字节数转人类可读大小 */
export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return '-';
  const n = Number(bytes);
  if (!Number.isFinite(n) || n < 0) return '-';
  if (n < 1024) return n + ' B';
  const units = ['KB', 'MB', 'GB', 'TB'];
  let v = n;
  let i = -1;
  do {
    v /= 1024;
    i += 1;
  } while (v >= 1024 && i < units.length - 1);
  return v.toFixed(1) + ' ' + units[i];
}

/** 地址缩写：0x123456...abcd */
export function shortAddress(address) {
  if (!address) return '';
  const s = String(address);
  if (s.length <= 12) return s;
  return s.slice(0, 6) + '...' + s.slice(-4);
}

/** 长 hash / txHash 缩写 */
export function shortHash(hash) {
  if (!hash) return '-';
  const s = String(hash);
  if (s.length <= 18) return s;
  return s.slice(0, 10) + '...' + s.slice(-6);
}

/**
 * 时间格式化：后端 LocalDateTime 序列化为 ISO-8601 字符串，转成 yyyy-MM-dd HH:mm:ss 本地显示
 */
export function formatDate(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  const p = (x) => String(x).padStart(2, '0');
  return (
    d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
    ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
  );
}
