import { describe, it, expect } from 'vitest';
import { safeJsonParse, unwrapResult } from './request.js';

describe('safeJsonParse（BigInteger 保精度）', () => {
  it('小整数保持为数字', () => {
    const o = safeJsonParse('{"quantity":7}');
    expect(o.quantity).toBe(7);
  });

  it('16 位以上裸整数转字符串，避免精度丢失', () => {
    const o = safeJsonParse('{"unitPriceDocToken":10000000000000000000,"balance":3}');
    expect(o.unitPriceDocToken).toBe('10000000000000000000');
    expect(o.balance).toBe(3);
  });

  it('恰好 15 位不转（边界），16 位才转', () => {
    const o = safeJsonParse('{"a":123456789012345,"b":1234567890123456}');
    expect(o.a).toBe(123456789012345);
    expect(o.b).toBe('1234567890123456');
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

  it('负大整数也保精度', () => {
    const o = safeJsonParse('{"x":-10000000000000000000}');
    expect(o.x).toBe('-10000000000000000000');
  });

  it('空 / 非字符串原样返回', () => {
    expect(safeJsonParse('')).toBe('');
    expect(safeJsonParse(42)).toBe(42);
  });
});

describe('unwrapResult（解包 Result{code,message,data}）', () => {
  it('code===0 返回 data', () => {
    expect(unwrapResult({ code: 0, message: 'ok', data: { balance: 5 } })).toEqual({ balance: 5 });
  });

  it('code===0 且 data 为 null 返回 null', () => {
    expect(unwrapResult({ code: 0, message: 'ok', data: null })).toBe(null);
  });

  it('code!==0 抛出带 code/message 的业务错误', () => {
    const fn = () => unwrapResult({ code: 429, message: '上传过于频繁，请 60 秒后再试' });
    expect(fn).toThrow('上传过于频繁，请 60 秒后再试');
    try {
      unwrapResult({ code: 409, message: '该交易已使用' });
      expect.unreachable();
    } catch (e) {
      expect(e.code).toBe(409);
      expect(e.isBusinessError).toBe(true);
    }
  });

  it('无 code 字段（非 Result）原样返回', () => {
    expect(unwrapResult({ foo: 1 })).toEqual({ foo: 1 });
    expect(unwrapResult(null)).toBe(null);
    expect(unwrapResult(undefined)).toBe(undefined);
  });
});
