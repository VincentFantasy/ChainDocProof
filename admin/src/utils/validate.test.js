import { describe, it, expect } from 'vitest';
import { isAddress, isPositiveAmount, isAmount } from './validate.js';

const VALID = '0x' + 'ab'.repeat(20);

describe('isAddress', () => {
  it('合法地址', () => {
    expect(isAddress(VALID)).toBe(true);
    expect(isAddress('0x' + 'A'.repeat(40))).toBe(true);
    expect(isAddress('  ' + VALID + '  ')).toBe(true); // 前后空格可容忍
  });
  it('非法地址', () => {
    expect(isAddress('0x123')).toBe(false);
    expect(isAddress('0x' + 'ab'.repeat(19))).toBe(false); // 38 位
    expect(isAddress('0x' + 'ab'.repeat(21))).toBe(false); // 42 位
    expect(isAddress('ab'.repeat(20))).toBe(false); // 缺 0x
    expect(isAddress('0x' + 'g'.repeat(40))).toBe(false); // 非十六进制
    expect(isAddress('')).toBe(false);
    expect(isAddress(null)).toBe(false);
    expect(isAddress(12345)).toBe(false);
  });
});

describe('isPositiveAmount', () => {
  it('正数通过', () => {
    expect(isPositiveAmount('10')).toBe(true);
    expect(isPositiveAmount('0.5')).toBe(true);
    expect(isPositiveAmount(' 3 ')).toBe(true);
  });
  it('非法不通过', () => {
    expect(isPositiveAmount('0')).toBe(false);
    expect(isPositiveAmount('-1')).toBe(false);
    expect(isPositiveAmount('')).toBe(false);
    expect(isPositiveAmount('abc')).toBe(false);
    expect(isPositiveAmount('1.2.3')).toBe(false);
    expect(isPositiveAmount(null)).toBe(false);
    expect(isPositiveAmount(10)).toBe(false); // 只接受字符串
  });
});

describe('isAmount', () => {
  it('正数且小数位 <= 18 通过', () => {
    expect(isAmount('0.0001')).toBe(true);
    expect(isAmount('1')).toBe(true);
    expect(isAmount('1000')).toBe(true);
    expect(isAmount('0.' + '0'.repeat(17) + '1')).toBe(true); // 恰好 18 位小数
    expect(isAmount(' 0.5 ')).toBe(true);
  });
  it('非法不通过', () => {
    expect(isAmount('0')).toBe(false);
    expect(isAmount('-1')).toBe(false);
    expect(isAmount('')).toBe(false);
    expect(isAmount('abc')).toBe(false);
    expect(isAmount('0.' + '0'.repeat(18) + '1')).toBe(false); // 19 位小数（wei 精度外）
    expect(isAmount('1.2.3')).toBe(false);
    expect(isAmount('1e-4')).toBe(false); // 科学计数法不接受
    expect(isAmount(null)).toBe(false);
    expect(isAmount(0.0001)).toBe(false); // 只接受字符串
  });
});
