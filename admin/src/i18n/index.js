import { computed } from 'vue';
import { createI18n } from 'vue-i18n';
import ElementPlusZhCn from 'element-plus/es/locale/lang/zh-cn';
import ElementPlusEn from 'element-plus/es/locale/lang/en';
import zhCnMessages from './zh-CN.js';
import enUsMessages from './en-US.js';

const LANG_KEY = 'admin_lang';
const DEFAULT_LANG = 'zh-CN';

/** 可选语言列表（切换器展示用，label 用语言自身书写） */
export const LANGS = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en-US', label: 'English' },
];

/**
 * 归一化语言取值（纯函数，可单测）：只接受已知语言值，非法/缺省回退默认中文
 * @param {*} value localStorage 或外部传入的语言值
 * @returns {'zh-CN'|'en-US'}
 */
export function normalizeLang(value) {
  return LANGS.some((l) => l.value === value) ? value : DEFAULT_LANG;
}

function initialLang() {
  try {
    return normalizeLang(
      typeof localStorage !== 'undefined' && localStorage.getItem(LANG_KEY)
    );
  } catch {
    return DEFAULT_LANG;
  }
}

const i18n = createI18n({
  // legacy: false → Composition API（useI18n）模式
  legacy: false,
  locale: initialLang(),
  fallbackLocale: DEFAULT_LANG,
  messages: { 'zh-CN': zhCnMessages, 'en-US': enUsMessages },
});

/** 当前语言（响应式，跟随 i18n 实例的 locale） */
export const lang = computed(() => i18n.global.locale.value);

/** 切换语言并持久化到 localStorage */
export function setLang(value) {
  const next = normalizeLang(value);
  i18n.global.locale.value = next;
  try {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(LANG_KEY, next);
    }
  } catch {
    // localStorage 不可用时仅保留内存态
  }
}

/** Element Plus 语言包（响应式，供 el-config-provider 使用） */
export const elLocale = computed(() => (lang.value === 'en-US' ? ElementPlusEn : ElementPlusZhCn));

export default i18n;
