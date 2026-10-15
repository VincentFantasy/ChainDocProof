import { describe, it, expect } from 'vitest';
import { toBigInt, mulWei, mulDivWei, tokensToWei, WEI_PER_TOKEN } from './wei.js';

describe('toBigInt（非负整数转 BigInt）', () => {
  it('合法字符串 / 数字', () => {
    expect(toBigInt('10')).toBe(10n);
    expect(toBigInt(' 12 ')).toBe(12n); // 首尾空格容忍
    expect(toBigInt(0)).toBe(0n);
    expect(toBigInt(5)).toBe(5n);
    expect(toBigInt('00010')).toBe(10n);
  });

  it('非法输入抛错', () => {
    expect(() => toBigInt(-1)).toThrow('非法 wei 值');
    expect(() => toBigInt(1.5)).toThrow('非法 wei 值');
    expect(() => toBigInt('abc')).toThrow('非法 wei 值');
    expect(() => toBigInt('-5')).toThrow('非法 wei 值');
    expect(() => toBigInt('')).toThrow('非法 wei 值');
    expect(() => toBigInt('1.0')).toThrow('非法 wei 值');
    expect(() => toBigInt(null)).toThrow('非法 wei 值');
  });
});

describe('mulWei（wei 字符串乘法）', () => {
  it('普通乘法', () => {
    expect(mulWei('10', '2')).toBe('20');
    expect(mulWei('3', '4')).toBe('12');
    expect(mulWei('1000000000000000000', '10')).toBe('10000000000000000000'); // 10 个 18 位小数 token
  });

  it('任一为 0 返回 "0"', () => {
    expect(mulWei('0', '1000000000000000000000000')).toBe('0');
    expect(mulWei('10', '0')).toBe('0');
  });

  it('超过 2^256-1 抛溢出错误', () => {
    const MAX_UINT256 = (2n ** 256n - 1n).toString();
    expect(() => mulWei(MAX_UINT256, '2')).toThrow('wei 乘法溢出');
    expect(() => mulWei('115792089237316195423570985008687907853269984665640564039457584007913129639935', '4')).toThrow('wei 乘法溢出');
  });

  it('乘积恰好为 2^256-1 不报错（边界）', () => {
    const MAX_UINT256 = 2n ** 256n - 1n;
    expect(mulWei(MAX_UINT256.toString(), '1')).toBe(MAX_UINT256.toString());
  });

  it('非法输入抛错', () => {
    expect(() => mulWei('abc', '2')).toThrow('非法 wei 值');
    expect(() => mulWei('-1', '2')).toThrow('非法 wei 值');
    expect(() => mulWei('1.5', '2')).toThrow('非法 wei 值');
  });
});

describe('mulDivWei（先乘后除 a × b ÷ d，ETH 兑换金额计算）', () => {
  const RATE_0_0001_ETH = '100000000000000'; // 0.0001 ETH 的 wei 表示

  it('用户反馈场景：10 枚 × 0.0001 ETH/枚 = 0.001 ETH（1e15 wei）', () => {
    const qty10Wei = tokensToWei(10); // 10e18
    expect(mulDivWei(qty10Wei, RATE_0_0001_ETH, WEI_PER_TOKEN)).toBe('1000000000000000');
  });

  it('1 枚 × 0.0001 ETH/枚 = 0.0001 ETH（1e14 wei）', () => {
    expect(mulDivWei(tokensToWei(1), RATE_0_0001_ETH, WEI_PER_TOKEN)).toBe(RATE_0_0001_ETH);
  });

  it('任一为 0 返回 "0"', () => {
    expect(mulDivWei('0', RATE_0_0001_ETH, WEI_PER_TOKEN)).toBe('0');
    expect(mulDivWei(tokensToWei(10), '0', WEI_PER_TOKEN)).toBe('0');
  });

  it('除法向下取整（与 Solidity 无符号整数除法一致）', () => {
    // (1e18+1) × 1e14 / 1e18 = 1e14 + 0.0000001e-3 → 截断为 1e14
    expect(mulDivWei('1000000000000000001', RATE_0_0001_ETH, WEI_PER_TOKEN)).toBe(RATE_0_0001_ETH);
    // 10 × 3 ÷ 4 = 7（7.5 截断）
    expect(mulDivWei('10', '3', '4')).toBe('7');
    // 10 ÷ 3 = 3
    expect(mulDivWei('10', '1', '3')).toBe('3');
  });

  it('乘积恰好为 2^256-1 不报错（边界）', () => {
    const MAX_UINT256 = 2n ** 256n - 1n;
    expect(mulDivWei(MAX_UINT256.toString(), '1', '1')).toBe(MAX_UINT256.toString());
  });

  it('乘积超过 2^256-1 抛溢出错误（除法不豁免乘法溢出检查）', () => {
    const MAX_UINT256 = (2n ** 256n - 1n).toString();
    expect(() => mulDivWei(MAX_UINT256, '2', WEI_PER_TOKEN)).toThrow('wei 乘法溢出');
  });

  it('除数为 0 抛错', () => {
    expect(() => mulDivWei('10', '2', 0n)).toThrow('除数不能为 0');
    expect(() => mulDivWei('10', '2', '0')).toThrow('除数不能为 0');
  });

  it('非法输入抛错', () => {
    expect(() => mulDivWei('abc', '2', WEI_PER_TOKEN)).toThrow('非法 wei 值');
    expect(() => mulDivWei('-1', '2', WEI_PER_TOKEN)).toThrow('非法 wei 值');
    expect(() => mulDivWei('1.5', '2', WEI_PER_TOKEN)).toThrow('非法 wei 值');
    expect(() => mulDivWei('10', null, WEI_PER_TOKEN)).toThrow('非法 wei 值');
  });
});

describe('tokensToWei（代币数量 → wei，×10^18）', () => {
  it('常规换算', () => {
    expect(tokensToWei(1)).toBe('1000000000000000000');
    expect(tokensToWei('10')).toBe('10000000000000000000');
    expect(tokensToWei('0')).toBe('0');
    expect(tokensToWei(' 5 ')).toBe('5000000000000000000'); // 首尾空格容忍
  });

  it('最大不溢出边界（floor((2^256-1)/10^18) 枚恰好不超 uint256）', () => {
    const maxTokens = (2n ** 256n - 1n) / 10n ** 18n;
    expect(tokensToWei(maxTokens.toString())).toBe((maxTokens * 10n ** 18n).toString());
  });

  it('超过边界抛溢出错误', () => {
    const maxTokens = (2n ** 256n - 1n) / 10n ** 18n;
    expect(() => tokensToWei((maxTokens + 1n).toString())).toThrow('wei 换算溢出');
  });

  it('非法输入抛错', () => {
    expect(() => tokensToWei(-1)).toThrow('非法 wei 值');
    expect(() => tokensToWei('1.5')).toThrow('非法 wei 值');
    expect(() => tokensToWei('abc')).toThrow('非法 wei 值');
    expect(() => tokensToWei(null)).toThrow('非法 wei 值');
  });
});
