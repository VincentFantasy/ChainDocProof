import { ref } from 'vue';

// 登录 token 存 localStorage（键名固定），模块级 ref 让所有组件共享同一份状态。
// 不引入 pinia：管理端只有一个登录态，轻量 ref 足够。
const TOKEN_KEY = 'admin_token';

function initialToken() {
  try {
    return (typeof localStorage !== 'undefined' && localStorage.getItem(TOKEN_KEY)) || '';
  } catch {
    return '';
  }
}

const token = ref(initialToken());

/**
 * 登录态管理：token 存取 + 是否已登录判断
 * @returns {{ token: import('vue').Ref, isLoggedIn: Function, login: Function, clear: Function }}
 */
export function useAuth() {
  function save(t) {
    token.value = t || '';
    try {
      if (t) localStorage.setItem(TOKEN_KEY, t);
      else localStorage.removeItem(TOKEN_KEY);
    } catch {
      // localStorage 不可用时仅保留内存态
    }
  }
  function isLoggedIn() {
    return !!token.value;
  }
  return {
    token,
    isLoggedIn,
    login: (t) => save(t),
    clear: () => save(''),
  };
}
