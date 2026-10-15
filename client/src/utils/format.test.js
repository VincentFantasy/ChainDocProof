import { describe, it, expect } from 'vitest';
import { formatWei, formatBytes, shortAddress, shortHash, formatDate } from './format.js';

describe('formatWei（wei 字符串转 DocToken 展示）', () => {
  it('整数 token', () => {
    expect(formatWei('10000000000000000000')).toBe('10');
    expect(formatWei('1')).toBe('0.000000000000000001'); // 1 wei，最小单位不丢
  });

  it('小数 token 去掉尾部 0', () => {
    expect(formatWei('500000000000000000')).toBe('0.5');
    expect(formatWei('1250000000000000000')).toBe('1.25');
    expect(formatWei('1050000000000000000')).toBe('1.05');
  });

  it('0 / 空 / null / undefined 返回 "0"', () => {
    expect(formatWei('0')).toBe('0');
    expect(formatWei('')).toBe('0');
    expect(formatWei(null)).toBe('0');
    expect(formatWei(undefined)).toBe('0');
  });

  it('大数值用字符串运算不丢精度（超过 2^53）', () => {
    // 25 位 wei：7 位整数 + 18 位小数
    expect(formatWei('1234567890123456789012345')).toBe('1234567.890123456789012345');
  });

  it('负数 / 非数字原样或带符号返回', () => {
    expect(formatWei('-500000000000000000')).toBe('-0.5');
    expect(formatWei('abc')).toBe('abc');
  });

  it('自定义小数位', () => {
    expect(formatWei('1000', 0)).toBe('1000');
    expect(formatWei('15', 2)).toBe('0.15');
  });
});

describe('formatBytes（字节数转人类可读）', () => {
  it('边界与单位', () => {
    expect(formatBytes(null)).toBe('-');
    expect(formatBytes(-1)).toBe('-');
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2.0 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
    expect(formatBytes(50 * 1024 * 1024)).toBe('50.0 MB');
  });
});

describe('shortAddress（地址缩写）', () => {
  it('长地址缩写为前 6 后 4', () => {
    expect(shortAddress('0x1234567890abcdef1234567890abcdef12345678')).toBe('0x1234...5678');
  });

  it('短串 / 空原样返回', () => {
    expect(shortAddress('')).toBe('');
    expect(shortAddress(null)).toBe('');
    expect(shortAddress('0x1234')).toBe('0x1234'); // ≤12 不缩
    expect(shortAddress('0x1234567890')).toBe('0x1234567890'); // 12 位边界（含 0x）不缩
    expect(shortAddress('0x12345678901')).toBe('0x1234...8901'); // 13 位开始缩
  });
});

describe('shortHash（hash / txHash 缩写）', () => {
  it('长串缩写为前 10 后 6', () => {
    expect(shortHash('0x1234567890abcdef1234567890abcdef')).toBe('0x12345678...abcdef');
  });

  it('短串 / 空', () => {
    expect(shortHash('')).toBe('-');
    expect(shortHash(null)).toBe('-');
    expect(shortHash('0x1234567890abcdef')).toBe('0x1234567890abcdef'); // ≤18 不缩
  });
});

describe('formatDate（ISO 时间转本地展示）', () => {
  it('合法 ISO 转 yyyy-MM-dd HH:mm:ss', () => {
    const r = formatDate('2026-10-15T10:20:30');
    expect(r).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
  });

  it('空 / 非法值', () => {
    expect(formatDate(null)).toBe('-');
    expect(formatDate('')).toBe('-');
    expect(formatDate('not-a-date')).toBe('not-a-date');
  });
});
