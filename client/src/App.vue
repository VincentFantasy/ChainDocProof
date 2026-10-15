<script setup>
import { computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import { useAuth } from './composables/useAuth.js';
import { useCredit } from './composables/useCredit.js';
import { elLocale } from './i18n/index.js';
import { shortAddress } from './utils/format.js';
import LangSwitcher from './components/LangSwitcher.vue';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const { isLoggedIn, address, clear } = useAuth();
const { credit } = useCredit();

// 登录页独立全屏渲染，其余页面套侧边栏布局
const isLoginPage = computed(() => route.path === '/login' && !isLoggedIn());

// 登录态变化时刷新次数信息（头部展示用）
watch(isLoggedIn, (v) => { if (v) useCredit().refresh(); }, { immediate: false });
onMounted(() => { if (isLoggedIn()) useCredit().refresh(); });

function logout() {
  ElMessageBox.confirm(t('common.logoutConfirm'), t('common.tip'), {
    type: 'warning',
    confirmButtonText: t('common.confirm'),
    cancelButtonText: t('common.cancel'),
  })
    .then(() => {
      clear();
      router.push('/login');
    })
    .catch(() => {});
}
</script>

<template>
  <!-- el-config-provider 的 locale 随语言切换响应式更新（Element Plus 组件内置文案） -->
  <el-config-provider :locale="elLocale">
    <router-view v-if="isLoginPage" />

    <el-container v-else class="layout">
      <el-aside width="220px" class="aside">
        <div class="logo">{{ t('common.appName') }}</div>
        <el-menu router :default-active="route.path" class="menu">
          <el-menu-item index="/">
            <el-icon><Document /></el-icon><span>{{ t('menu.proofs') }}</span>
          </el-menu-item>
          <el-menu-item index="/buy">
            <el-icon><Money /></el-icon><span>{{ t('menu.buy') }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container class="body">
        <el-header class="header">
          <span class="title">{{ route.meta.title ? t(route.meta.title) : '' }}</span>
          <div class="header-right">
            <el-tag v-if="address" type="info" effect="plain">{{ shortAddress(address) }}</el-tag>
            <el-tag v-if="credit" type="success" effect="plain">{{ t('header.credits', { n: credit.balance }) }}</el-tag>
            <LangSwitcher />
            <el-button type="danger" text @click="logout">{{ t('common.logout') }}</el-button>
          </div>
        </el-header>
        <el-main class="main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<style>
* { box-sizing: border-box; }
html, body, #app { height: 100%; margin: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }

.layout { height: 100vh; }
.aside { background: #1f2937; }
.aside .logo {
  height: 56px; line-height: 56px; text-align: center;
  color: #fff; font-size: 15px; font-weight: 600;
}
.aside .menu { border-right: none; background: #1f2937; }
.aside .menu .el-menu-item { color: #cbd5e1; }
.aside .menu .el-menu-item.is-active { background: #2563eb; color: #fff; }

.body { display: flex; flex-direction: column; }
.header {
  height: 56px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e5e7eb; background: #fff;
}
.header .title { font-size: 16px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 10px; }
.main { background: #f3f4f6; }
</style>
