import { describe, it, expect } from 'vitest';
import { textToHex, mapWalletError, weiToHex } from './wallet.js';

describe('textToHex（UTF-8 文本转 hex，personal_sign 入参）', () => {
  it('ASCII 文本', () => {
    expect(textToHex('A')).toBe('0x41');
    expect(textToHex('hello')).toBe('0x68656c6c6f');
    expect(textToHex('')).toBe('0x');
  });

  it('多字节 UTF-8 字符', () => {
    expect(textToHex('你好')).toBe('0xe4bda0e5a5bd');
  });

  it('登录消息格式：前缀 + 地址', () => {
    expect(textToHex('ChainDocProof:0x1234')).toBe(
      '0x' +
        '436861696e446f6350726f6f663a307831323334'
    );
  });
});

describe('mapWalletError（钱包错误映射本地化文案）', () => {
  const t = (key) => key; // 恒等 t 函数，断言映射到的 key

  it('本模块机器可读错误', () => {
    expect(mapWalletError(new Error('NO_WALLET'), t)).toBe('wallet.noWallet');
    expect(mapWalletError(new Error('WRONG_CHAIN'), t)).toBe('wallet.wrongChain');
    expect(mapWalletError(new Error('NO_ACCOUNT'), t)).toBe('wallet.noAccount');
    expect(mapWalletError(new Error('TX_TIMEOUT'), t)).toBe('wallet.txTimeout');
  });

  it('EIP-1193 用户拒绝（code=4001）', () => {
    const e = new Error('User rejected the request.');
    e.code = 4001;
    expect(mapWalletError(e, t)).toBe('wallet.rejected');
  });

  it('未知错误：有 message 用 message，否则兜底文案', () => {
    expect(mapWalletError(new Error('boom'), t)).toBe('boom');
    expect(mapWalletError(new Error(''), t)).toBe('wallet.failed');
    expect(mapWalletError(null, t)).toBe('wallet.failed');
  });
});

describe('weiToHex（wei 十进制 → 0x 十六进制，msg.value 用）', () => {
  it('0 / 常规值 / 1 ETH 已知向量', () => {
    expect(weiToHex('0')).toBe('0x0');
    expect(weiToHex('1')).toBe('0x1');
    expect(weiToHex('256')).toBe('0x100');
    expect(weiToHex('1000000000000000000')).toBe('0xde0b6b3a7640000'); // 1 ETH
    expect(weiToHex('10000000000000000000')).toBe('0x' + (10000000000000000000n).toString(16)); // 10 ETH，超过 2^53
  });

  it('非法输入抛错', () => {
    expect(() => weiToHex('abc')).toThrow('非法 wei 值');
    expect(() => weiToHex('-1')).toThrow('非法 wei 值');
    expect(() => weiToHex('1.5')).toThrow('非法 wei 值');
    expect(() => weiToHex(null)).toThrow('非法 wei 值');
  });
});
