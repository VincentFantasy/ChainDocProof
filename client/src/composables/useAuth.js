import { ref } from 'vue';

// 登录态：token + 钱包地址（地址仅用于展示与链上查询，后端鉴权只认 token）。
// 存 localStorage，模块级 ref 让所有组件共享同一份状态。
// 不引入 pinia：用户端只有登录态与次数两块共享状态，轻量 ref 足够。
const TOKEN_KEY = 'client_token';
const ADDRESS_KEY = 'client_address';

function initial(key) {
  try {
    return (typeof localStorage !== 'undefined' && localStorage.getItem(key)) || '';
  } catch {
    return '';
  }
}

const token = ref(initial(TOKEN_KEY));
const address = ref(initial(ADDRESS_KEY));

/**
 * 登录态管理：token / 钱包地址 存取 + 是否已登录判断
 * @returns {{ token: import('vue').Ref, address: import('vue').Ref, isLoggedIn: Function, login: Function, clear: Function }}
 */
export function useAuth() {
  function save(t, addr) {
    token.value = t || '';
    address.value = addr || '';
    try {
      if (t) localStorage.setItem(TOKEN_KEY, t);
      else localStorage.removeItem(TOKEN_KEY);
      if (addr) localStorage.setItem(ADDRESS_KEY, addr);
      else localStorage.removeItem(ADDRESS_KEY);
    } catch {
      // localStorage 不可用时仅保留内存态
    }
  }
  function isLoggedIn() {
    return !!token.value;
  }
  return {
    token,
    address,
    isLoggedIn,
    login: (t, addr) => save(t, addr),
    clear: () => save('', ''),
  };
}
