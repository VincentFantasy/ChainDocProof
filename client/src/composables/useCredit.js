import { ref } from 'vue';
import { clientApi } from '../api/client.js';

// 次数信息模块级共享：头部展示剩余次数，购买页展示单价；
// 存证 / 校验 / 购买成功后调用 refresh() 刷新。
const credit = ref(null); // { balance, unitPriceDocToken(wei 字符串) }
const loading = ref(false);

/**
 * 次数信息读取与刷新
 * @returns {{ credit: import('vue').Ref, loading: import('vue').Ref, refresh: Function }}
 */
export function useCredit() {
  async function refresh() {
    loading.value = true;
    try {
      credit.value = await clientApi.getCredit();
    } catch {
      // 刷新失败（如网络抖动）保留旧值，头部仅做展示；真正的 401 已由 request.js 统一跳登录
    } finally {
      loading.value = false;
    }
  }
  return { credit, loading, refresh };
}
