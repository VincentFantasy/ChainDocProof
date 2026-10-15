import { describe, it, expect } from 'vitest';
import {
  BURN_SELECTOR,
  BALANCE_OF_SELECTOR,
  BUY_TOKENS_SELECTOR,
  ETH_PER_TOKEN_SELECTOR,
  encodeBurn,
  encodeBalanceOf,
  encodeBuyTokens,
  encodeEthPerToken,
  decodeUint256,
} from './abi.js';

const ADDR = '0x' + '00'.repeat(19) + '01'; // 0x...0001

describe('选择子常量（keccak256(函数签名) 前 4 字节）', () => {
  it('burn(uint256) / balanceOf(address)', () => {
    expect(BURN_SELECTOR).toBe('0x42966c68');
    expect(BALANCE_OF_SELECTOR).toBe('0x70a08231');
  });

  it('buyTokens(uint256) / ethPerToken()', () => {
    expect(BUY_TOKENS_SELECTOR).toBe('0x3610724e');
    expect(ETH_PER_TOKEN_SELECTOR).toBe('0xd22bc441');
  });
});

describe('encodeBurn（DocToken 销毁 calldata）', () => {
  it('已知向量：burn(1 wei)', () => {
    expect(encodeBurn('1')).toBe('0x42966c68' + '0'.repeat(63) + '1');
  });

  it('已知向量：1 枚 DocToken = 10^18 wei = 0xde0b6b3a7640000（1 ETH 的 hex）', () => {
    expect(encodeBurn('1000000000000000000')).toBe('0x42966c68' + '0'.repeat(49) + 'de0b6b3a7640000');
  });

  it('大金额（超过 2^53）用 BigInt 编码不丢精度', () => {
    const amount = '1000000000000000000000000'; // 1e24 wei
    expect(encodeBurn(amount)).toBe('0x42966c68' + BigInt(amount).toString(16).padStart(64, '0'));
  });

  it('uint256 最大值边界可编码', () => {
    expect(encodeBurn((2n ** 256n - 1n).toString())).toBe('0x42966c68' + 'f'.repeat(64));
  });

  it('非法金额抛错（含 2^256 溢出边界）', () => {
    expect(() => encodeBurn('abc')).toThrow('非法 uint256');
    expect(() => encodeBurn('-1')).toThrow('非法 uint256');
    expect(() => encodeBurn('1.5')).toThrow('非法 uint256');
    expect(() => encodeBurn((2n ** 256n).toString())).toThrow('uint256 溢出');
  });
});

describe('encodeBalanceOf（ERC20 balanceOf calldata）', () => {
  it('已知向量', () => {
    expect(encodeBalanceOf(ADDR)).toBe('0x70a08231' + '0'.repeat(24) + '00'.repeat(19) + '01');
  });

  it('非法地址抛错', () => {
    expect(() => encodeBalanceOf('not-an-address')).toThrow('非法地址');
  });
});

describe('encodeBuyTokens（DocToken ETH 兑换 calldata）', () => {
  it('已知向量：buyTokens(1 wei)', () => {
    expect(encodeBuyTokens('1')).toBe('0x3610724e' + '0'.repeat(63) + '1');
  });

  it('已知向量：1 枚 DocToken = 10^18 wei = 0xde0b6b3a7640000（1 ETH 的 hex）', () => {
    expect(encodeBuyTokens('1000000000000000000')).toBe('0x3610724e' + '0'.repeat(49) + 'de0b6b3a7640000');
  });

  it('大数量（超过 2^53）用 BigInt 编码不丢精度', () => {
    const amount = '1000000000000000000000000'; // 1e24 wei
    expect(encodeBuyTokens(amount)).toBe('0x3610724e' + BigInt(amount).toString(16).padStart(64, '0'));
  });

  it('uint256 最大值边界可编码', () => {
    expect(encodeBuyTokens((2n ** 256n - 1n).toString())).toBe('0x3610724e' + 'f'.repeat(64));
  });

  it('非法数量抛错（含 2^256 溢出边界）', () => {
    expect(() => encodeBuyTokens('abc')).toThrow('非法 uint256');
    expect(() => encodeBuyTokens('-1')).toThrow('非法 uint256');
    expect(() => encodeBuyTokens((2n ** 256n).toString())).toThrow('uint256 溢出');
  });
});

describe('encodeEthPerToken（汇率读取调用数据）', () => {
  it('无参数函数只返回选择子', () => {
    expect(encodeEthPerToken()).toBe('0xd22bc441');
  });
});

describe('decodeUint256（eth_call 返回值解码）', () => {
  it('0 / 空 / 常规值', () => {
    expect(decodeUint256('0x')).toBe('0');
    expect(decodeUint256('0x0')).toBe('0');
    expect(decodeUint256('0x01')).toBe('1');
    expect(decodeUint256('0x123')).toBe('291');
    expect(decodeUint256('0x' + 'f'.repeat(64))).toBe((2n ** 256n - 1n).toString());
  });

  it('与 encodeUint256Word 往返一致', () => {
    for (const v of ['0', '1', '255', '1000000000000000000000000']) {
      const word = BigInt(v).toString(16).padStart(64, '0');
      expect(decodeUint256('0x' + word)).toBe(v);
    }
  });

  it('非法输入抛错', () => {
    expect(() => decodeUint256('abc')).toThrow('非法十六进制'); // 缺 0x
    expect(() => decodeUint256('0xGG')).toThrow('非法十六进制');
    expect(() => decodeUint256('0x1' + '0'.repeat(64))).toThrow('uint256 超长'); // 33 字节
    expect(() => decodeUint256(123)).toThrow('非法十六进制'); // 非字符串
  });
});
