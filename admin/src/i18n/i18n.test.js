import { describe, it, expect } from 'vitest';
import zhCn from './zh-CN.js';
import enUs from './en-US.js';
import { LANGS, normalizeLang } from './index.js';

/** 递归收集文案树的全部叶子 key 路径，如 menu.exchangeRate */
function collectKeys(node, prefix = '') {
  const keys = [];
  for (const [k, v] of Object.entries(node)) {
    const path = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === 'object') {
      keys.push(...collectKeys(v, path));
    } else {
      keys.push(path);
    }
  }
  return keys;
}

describe('i18n 文案字典', () => {
  it('中英文 key 集合一致（防止漏配/多配）', () => {
    const zhKeys = collectKeys(zhCn).sort();
    const enKeys = collectKeys(enUs).sort();
    expect(enKeys).toEqual(zhKeys);
  });

  it('所有文案值均为非空字符串', () => {
    for (const dict of [zhCn, enUs]) {
      for (const path of collectKeys(dict)) {
        const value = path.split('.').reduce((o, k) => o[k], dict);
        expect(typeof value, path).toBe('string');
        expect(value.length, path).toBeGreaterThan(0);
      }
    }
  });
});

describe('normalizeLang', () => {
  it('合法语言值原样返回', () => {
    expect(normalizeLang('zh-CN')).toBe('zh-CN');
    expect(normalizeLang('en-US')).toBe('en-US');
  });

  it('非法/缺省值回退默认中文', () => {
    expect(normalizeLang(undefined)).toBe('zh-CN');
    expect(normalizeLang(null)).toBe('zh-CN');
    expect(normalizeLang('')).toBe('zh-CN');
    expect(normalizeLang('EN')).toBe('zh-CN'); // 大小写敏感，不猜测
    expect(normalizeLang('fr-FR')).toBe('zh-CN');
    expect(normalizeLang(123)).toBe('zh-CN');
  });
});

describe('LANGS', () => {
  it('包含两种语言且 value 唯一、label 非空', () => {
    expect(LANGS).toHaveLength(2);
    expect(LANGS.map((l) => l.value)).toEqual(['zh-CN', 'en-US']);
    for (const l of LANGS) {
      expect(l.label.length).toBeGreaterThan(0);
    }
  });
});
