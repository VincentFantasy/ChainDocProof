import http from './request.js';

// 管理端接口封装：响应拦截器已解包 Result，这里返回的 Promise 直接 resolve 出 data
export const adminApi = {
  /** 账密登录，返回 { token } */
  login: (username, password) => http.post('/admin/auth/login', { username, password }),

  /** 统计概览：用户/存证/购买/发放 + 链上兑换次数 */
  getStats: () => http.get('/admin/stats'),

  /** 普通用户列表 */
  getUsers: () => http.get('/admin/users'),

  /** 存证记录，可按钱包地址筛选 */
  getProofs: (address) => http.get('/admin/proofs', { params: address ? { address } : {} }),

  /** DocToken 管理端发放记录 */
  getMints: () => http.get('/admin/tokens/mints'),

  /** DocToken ETH 兑换次数（链上） */
  getExchanges: () => http.get('/admin/tokens/exchanges'),

  /** 查询当前 ETH 兑换汇率（链上），返回 { ethPerToken: wei 字符串 } */
  getExchangeRate: () => http.get('/admin/tokens/exchange-rate'),

  /** 调整 ETH 兑换汇率，rate 为 1 枚 DocToken 对应 ETH 的十进制字符串，返回 { txHash } */
  setExchangeRate: (rate) => http.post('/admin/tokens/exchange-rate', { rate }),

  /** 给指定钱包 mint DocToken，amount 为 DocToken 十进制字符串 */
  mint: (toAddress, amount) => http.post('/admin/tokens/mint', { toAddress, amount }),

  /** 次数流水，可按钱包地址 / 类型（PURCHASE/STORE/VERIFY）筛选 */
  getCreditLogs: (address, type) =>
    http.get('/admin/credit-logs', {
      params: { ...(address ? { address } : {}), ...(type ? { type } : {}) },
    }),
};

export default adminApi;
