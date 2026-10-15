import { describe, it, expect } from 'vitest';
import { formatWei, formatBytes, shortAddress, shortHash, formatDate } from './format.js';

// 整数枚 DocToken 对应的 wei 字符串（避免手敲 0 数错位）
const weiOf = (tokens) => String(tokens) + '0'.repeat(18);

describe('formatWei（wei → DocToken，字符串运算不丢精度）', () => {
  it('空值返回 "0"', () => {
    expect(formatWei(null)).toBe('0');
    expect(formatWei(undefined)).toBe('0');
    expect(formatWei('')).toBe('0');
  });

  it('整枚 DocToken', () => {
    expect(formatWei(weiOf(10))).toBe('10');
    expect(formatWei('1000000000000000000')).toBe('1'); // 1e18 wei = 1 枚
  });

  it('小数 DocToken', () => {
    expect(formatWei('5' + '0'.repeat(17))).toBe('0.5'); // 5e17 wei
    expect(formatWei('25' + '0'.repeat(17))).toBe('2.5'); // 2.5e18 wei
  });

  it('超 2^53 的大值用字符串不丢精度', () => {
    expect(formatWei(weiOf(1000))).toBe('1000'); // 1e21 wei
    expect(formatWei(weiOf(1000000000))).toBe('1000000000'); // 1e27 wei，远超安全整数
    // 1e9 枚 + 1 wei：末位非 0，验证小数位不被四舍五入
    expect(formatWei(weiOf(1000000000).slice(0, -1) + '1')).toBe('1000000000.000000000000000001');
  });

  it('大整数整枚原样返回', () => {
    expect(formatWei(weiOf(1234567))).toBe('1234567');
  });

  it('零与尾部 0 裁剪', () => {
    expect(formatWei('0')).toBe('0');
    expect(formatWei(weiOf(1000000))).toBe('1000000');
  });

  it('负数', () => {
    expect(formatWei('-' + '5' + '0'.repeat(17))).toBe('-0.5');
  });

  it('非法输入原样返回（不误算）', () => {
    expect(formatWei('abc')).toBe('abc');
  });
});

describe('formatBytes', () => {
  it('空值返回 -', () => {
    expect(formatBytes(null)).toBe('-');
    expect(formatBytes(undefined)).toBe('-');
  });
  it('字节与进位', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2.0 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
  });
  it('负数返回 -', () => {
    expect(formatBytes(-1)).toBe('-');
  });
});

describe('shortAddress', () => {
  it('正常地址缩略', () => {
    expect(shortAddress('0x1234567890abcdef1234567890abcdef12345678')).toBe('0x1234...5678');
  });
  it('过短原样返回', () => {
    expect(shortAddress('0x1234')).toBe('0x1234');
  });
  it('空值返回空串', () => {
    expect(shortAddress(null)).toBe('');
    expect(shortAddress('')).toBe('');
  });
});

describe('shortHash', () => {
  it('长哈希缩略', () => {
    const h = '0x' + 'ab'.repeat(32);
    expect(shortHash(h)).toBe('0xabababab' + '...' + 'ababab');
  });
  it('空值返回 -', () => {
    expect(shortHash(null)).toBe('-');
  });
});

describe('formatDate', () => {
  it('ISO 时间转本地格式', () => {
    expect(formatDate('2026-10-13T09:30:05')).toMatch(/2026-10-13 09:30:05/);
  });
  it('空值返回 -', () => {
    expect(formatDate(null)).toBe('-');
    expect(formatDate('')).toBe('-');
  });
  it('无法解析原样返回', () => {
    expect(formatDate('not-a-date')).toBe('not-a-date');
  });
});
