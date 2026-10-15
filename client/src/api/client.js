import http from './request.js';

// 用户端接口封装：响应拦截器已解包 Result，这里返回的 Promise 直接 resolve 出 data
export const clientApi = {
  /** 钱包签名登录：EIP-191 签名消息 "ChainDocProof:" + address，返回 { token } */
  login: (address, signature) => http.post('/api/user/auth/login', { address, signature }),

  /** 查询次数：{ balance 剩余次数, unitPriceDocToken 单价(wei 字符串) } */
  getCredit: () => http.get('/api/user/credits'),

  /** 购买次数：已销毁 quantity × 单价 的 DocToken（链上 burn 打进 0 地址），提交销毁 txHash（幂等） */
  purchase: (quantity, txHash) => http.post('/api/user/credits/purchase', { quantity, txHash }),

  /** 上传文件（multipart，field 名 file），后端只算 SHA-256 不落盘，返回 PENDING 存证记录；60 秒冷却（429） */
  uploadProof: (file) => {
    const form = new FormData();
    form.append('file', file);
    return http.post('/api/user/proofs/upload', form);
  },

  /** 存证上链：扣 1 次，hash + 上传时间戳写入 DocProof 合约，返回更新后的记录（STORED + txHash） */
  storeProof: (id) => http.post(`/api/user/proofs/${id}/store`),

  /** 验真：重新上传原件比对链上 hash，扣 1 次，data=true 表示未被篡改 */
  verifyProof: (id, file) => {
    const form = new FormData();
    form.append('file', file);
    return http.post(`/api/user/proofs/${id}/verify`, form);
  },

  /** 我的存证列表 */
  listProofs: () => http.get('/api/user/proofs'),
};

export default clientApi;
