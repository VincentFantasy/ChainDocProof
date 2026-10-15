import axios from 'axios';
import { useAuth } from '../composables/useAuth.js';

/**
 * 安全解析后端 JSON：把超过 15 位的裸整数（BigInteger 序列化结果，如 wei 金额）
 * 加引号转成字符串，避免 JS Number 在 2^53 以上丢失精度。
 * 只匹配出现在对象值 / 数组元素位置（前一个字符为 : [ , 之一）的整数，
 * 不会误伤字符串内容里的数字。
 *
 * @param {string} text 原始响应文本
 * @returns {*} 解析后的对象
 */
export function safeJsonParse(text) {
  if (typeof text !== 'string' || text.length === 0) {
    return text;
  }
  const safe = text.replace(/([:[,]\s*)(-?\d{16,})(?=\s*[,\}\]])/g, '$1"$2"');
  return JSON.parse(safe);
}

/**
 * 解包统一返回结构 Result{code,message,data}（纯函数，可单测）。
 * code===0 返回 data；code!==0 抛出带 code/message 的 Error。
 *
 * @param {*} body 后端返回体
 * @returns {*} data 部分
 */
export function unwrapResult(body) {
  if (body === null || body === undefined || typeof body.code === 'undefined') {
    return body;
  }
  if (body.code === 0) {
    return body.data;
  }
  const err = new Error(body.message || '请求失败');
  err.code = body.code;
  err.isBusinessError = true;
  throw err;
}

const http = axios.create({
  // baseURL 留空：直接请求 /api/**，开发期由 Vite 代理转发到后端 127.0.0.1:8080
  timeout: 60000,
  transformResponse: [
    (data) => {
      if (typeof data !== 'string') return data;
      try {
        return safeJsonParse(data);
      } catch {
        return data;
      }
    },
  ],
});

// 请求拦截：有 token 就带上 Authorization: Bearer <token>
http.interceptors.request.use((config) => {
  const t = useAuth().token.value;
  if (t) {
    config.headers.Authorization = 'Bearer ' + t;
  }
  return config;
});

// 响应拦截：成功分支解包 Result；HTTP 401（token 失效/过期/后端重启）清 token 并跳登录重新签名
let router = null;
export function bindRouter(r) {
  router = r;
}

http.interceptors.response.use(
  (resp) => unwrapResult(resp.data),
  (error) => {
    const status = error.response && error.response.status;
    if (status === 401) {
      useAuth().clear();
      if (router && router.currentRoute.value.path !== '/login') {
        router.push('/login');
      }
    }
    const body = error.response && error.response.data;
    const msg = (body && body.message) || error.message || '网络错误';
    const err = new Error(msg);
    err.code = (body && typeof body.code !== 'undefined') ? body.code : (status || -1);
    throw err;
  }
);

export default http;
