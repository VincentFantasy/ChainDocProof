import { describe, it, expect } from 'vitest';
import { safeJsonParse, unwrapResult } from './request.js';

describe('safeJsonParse（BigInteger 保精度）', () => {
  it('小整数保持为数字', () => {
    const o = safeJsonParse('{"buyCount":7}');
    expect(o.buyCount).toBe(7);
  });

  it('超过 15 位的裸整数转为字符串，避免精度丢失', () => {
    const o = safeJsonParse('{"mintedTotalWei":1000000000000000000000000,"totalUsers":3}');
    expect(o.mintedTotalWei).toBe('1000000000000000000000000');
    expect(o.totalUsers).toBe(3);
  });

  it('数组里的裸大整数同样保精度', () => {
    const o = safeJsonParse('{"amounts":[10000000000000000000,2]}');
    expect(o.amounts[0]).toBe('10000000000000000000');
    expect(o.amounts[1]).toBe(2);
  });

  it('字符串里的长数字不受影响', () => {
    const o = safeJsonParse('{"txHash":"0x123456789012345678901234567890"}');
    expect(o.txHash).toBe('0x123456789012345678901234567890');
  });

  it('空 / 非字符串原样返回', () => {
    expect(safeJsonParse('')).toBe('');
    expect(safeJsonParse(42)).toBe(42);
  });
});

describe('unwrapResult（解包 Result{code,message,data}）', () => {
  it('code===0 返回 data', () => {
    expect(unwrapResult({ code: 0, message: 'ok', data: { a: 1 } })).toEqual({ a: 1 });
  });

  it('code===0 且 data 为 null 返回 null', () => {
    expect(unwrapResult({ code: 0, message: 'ok', data: null })).toBe(null);
  });

  it('code!==0 抛出带 code/message 的错误', () => {
    const fn = () => unwrapResult({ code: 429, message: '登录失败次数过多，请 60 秒后再试' });
    expect(fn).toThrow('登录失败次数过多，请 60 秒后再试');
    try {
      unwrapResult({ code: 401, message: '用户名或密码错误' });
      expect.unreachable();
    } catch (e) {
      expect(e.code).toBe(401);
      expect(e.isBusinessError).toBe(true);
    }
  });

  it('无 code 字段（非 Result）原样返回', () => {
    expect(unwrapResult({ foo: 1 })).toEqual({ foo: 1 });
    expect(unwrapResult(null)).toBe(null);
    expect(unwrapResult(undefined)).toBe(undefined);
  });
});
