import { describe, it, expect } from 'vitest';
import { normalizeLang, LANGS } from './index.js';

describe('normalizeLang（语言值归一化）', () => {
  it('已知语言值原样返回', () => {
    expect(normalizeLang('zh-CN')).toBe('zh-CN');
    expect(normalizeLang('en-US')).toBe('en-US');
  });

  it('非法 / 缺省回退默认中文', () => {
    expect(normalizeLang('zh-TW')).toBe('zh-CN');
    expect(normalizeLang('en-us')).toBe('zh-CN'); // 大小写敏感
    expect(normalizeLang('')).toBe('zh-CN');
    expect(normalizeLang(null)).toBe('zh-CN');
    expect(normalizeLang(undefined)).toBe('zh-CN');
    expect(normalizeLang(123)).toBe('zh-CN');
  });

  it('LANGS 与默认语言一致', () => {
    expect(LANGS.map((l) => l.value)).toEqual(['zh-CN', 'en-US']);
  });
});
