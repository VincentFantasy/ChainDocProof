<script setup>
import { reactive, ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { adminApi } from '../api/admin.js';
import { useAuth } from '../composables/useAuth.js';
import LangSwitcher from '../components/LangSwitcher.vue';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const { login } = useAuth();

const form = reactive({ username: '', password: '' });
const loading = ref(false);
const errorMsg = ref('');

// 从后端锁定提示里提取等待秒数，例如 "登录失败次数过多，请 60 秒后再试" → 60
function parseLockSeconds(msg) {
  const m = /(\d+)\s*秒/.exec(msg || '');
  return m ? parseInt(m[1], 10) : null;
}
const lockSeconds = computed(() => parseLockSeconds(errorMsg.value));

async function doLogin() {
  if (!form.username || !form.password) {
    errorMsg.value = t('login.required');
    return;
  }
  loading.value = true;
  errorMsg.value = '';
  try {
    const data = await adminApi.login(form.username, form.password);
    login(data.token);
    router.push(route.query.redirect || '/dashboard');
  } catch (e) {
    errorMsg.value = e.message || t('login.failed');
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

      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        :description="lockSeconds ? t('login.locked', { seconds: lockSeconds }) : ''"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-form :model="form" @keyup.enter="doLogin" label-position="top">
        <el-form-item :label="t('login.username')">
          <el-input v-model="form.username" :placeholder="t('login.usernamePh')" clearable />
        </el-form-item>
        <el-form-item :label="t('login.password')">
          <el-input v-model="form.password" type="password" :placeholder="t('login.passwordPh')" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit" @click="doLogin">
          {{ t('login.submit') }}
        </el-button>
      </el-form>
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
.login-card { width: 380px; }
.card-title { font-size: 18px; font-weight: 600; text-align: center; }
.submit { width: 100%; }
</style>
