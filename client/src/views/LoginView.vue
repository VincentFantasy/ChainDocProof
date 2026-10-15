<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { clientApi } from '../api/client.js';
import { useAuth } from '../composables/useAuth.js';
import { getAccounts, ensureChain, signLoginMessage, mapWalletError } from '../wallet/wallet.js';
import LangSwitcher from '../components/LangSwitcher.vue';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const { login } = useAuth();

const loading = ref(false);
const errorMsg = ref('');

/**
 * 连接钱包并登录：请求账户 → 校验 Sepolia 网络 → EIP-191 签名 → 后端换 token
 */
async function connect() {
  if (loading.value) return;
  loading.value = true;
  errorMsg.value = '';
  try {
    const address = await getAccounts();
    await ensureChain();
    const signature = await signLoginMessage(address);
    const data = await clientApi.login(address, signature);
    login(data.token, address);
    router.push(route.query.redirect || '/');
  } catch (e) {
    // 签名 / 登录的业务错误直接展示后端文案，钱包错误做本地化映射
    errorMsg.value = (e && e.isBusinessError) ? e.message : mapWalletError(e, t);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="lang-corner">
      <LangSwitcher />
    </div>
    <el-card class="login-card">
      <template #header>
        <div class="card-title">{{ t('common.appName') }}</div>
      </template>

      <p class="desc">{{ t('login.desc') }}</p>

      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-button type="primary" size="large" :loading="loading" class="submit" @click="connect">
        <el-icon v-if="!loading" style="margin-right: 6px"><Wallet /></el-icon>
        {{ loading ? t('login.connecting') : t('login.connect') }}
      </el-button>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  position: relative;
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1f2937 0%, #2563eb 100%);
}
.lang-corner { position: absolute; top: 16px; right: 24px; color: #e2e8f0; }
.lang-corner :deep(.lang-btn) { color: #e2e8f0; }
.lang-corner :deep(.lang-btn:hover) { background: rgba(255, 255, 255, 0.15); }
.login-card { width: 420px; }
.card-title { font-size: 18px; font-weight: 600; text-align: center; }
.desc { color: #64748b; font-size: 13px; line-height: 1.7; }
.submit { width: 100%; }
</style>
