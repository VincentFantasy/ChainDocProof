import { describe, it, expect } from 'vitest';
import { isAddress, isQuantity } from './validate.js';

const VALID = '0x1234567890abcdef1234567890abcdef12345678';

describe('isAddress（以太坊地址校验）', () => {
  it('合法地址（含大写 hex、首尾空格）', () => {
    expect(isAddress(VALID)).toBe(true);
    expect(isAddress('0xABCDEF1234567890ABCDEF1234567890ABCDEF12')).toBe(true);
    expect(isAddress('  ' + VALID + ' ')).toBe(true);
  });

  it('非法地址', () => {
    expect(isAddress('')).toBe(false);
    expect(isAddress(null)).toBe(false);
    expect(isAddress(123)).toBe(false);
    expect(isAddress('1234567890abcdef1234567890abcdef12345678')).toBe(false); // 缺 0x
    expect(isAddress('0x1234567890abcdef1234567890abcdef1234567')).toBe(false); // 39 位
    expect(isAddress('0x1234567890abcdef1234567890abcdef123456789')).toBe(false); // 41 位
    expect(isAddress('0x1234567890abcdef1234567890abcdef1234567g')).toBe(false); // 非 hex 字符
  });
});

describe('isQuantity（购买次数校验）', () => {
  it('数字入参', () => {
    expect(isQuantity(1)).toBe(true);
    expect(isQuantity(1000)).toBe(true); // 上限边界
    expect(isQuantity(0)).toBe(false);
    expect(isQuantity(1001)).toBe(false); // 超上限
    expect(isQuantity(1.5)).toBe(false); // 非整数
    expect(isQuantity(-3)).toBe(false);
    expect(isQuantity(null)).toBe(false); // el-input-number 清空
    expect(isQuantity('')).toBe(false);
    expect(isQuantity(true)).toBe(false);
  });

  it('字符串入参', () => {
    expect(isQuantity('10')).toBe(true);
    expect(isQuantity(' 5 ')).toBe(true);
    expect(isQuantity('0')).toBe(false);
    expect(isQuantity('1001')).toBe(false);
    expect(isQuantity('abc')).toBe(false);
    expect(isQuantity('1.0')).toBe(false);
  });

  it('自定义上限', () => {
    expect(isQuantity(2000, 5)).toBe(false);
    expect(isQuantity('3', 5)).toBe(true);
    expect(isQuantity(5, 5)).toBe(true);
    expect(isQuantity(6, 5)).toBe(false);
  });
});
