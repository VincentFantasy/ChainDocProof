import { createRouter, createWebHashHistory } from 'vue-router';
import { useAuth } from '../composables/useAuth.js';
import { bindRouter } from '../api/request.js';

import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import UsersView from '../views/UsersView.vue';
import ProofsView from '../views/ProofsView.vue';
import MintView from '../views/MintView.vue';
import ExchangeRateView from '../views/ExchangeRateView.vue';
import CreditLogsView from '../views/CreditLogsView.vue';

// meta.title 存 i18n key，由 App.vue 头部经 t() 翻译展示
const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { title: 'menu.dashboard' } },
  { path: '/users', name: 'users', component: UsersView, meta: { title: 'menu.users' } },
  { path: '/proofs', name: 'proofs', component: ProofsView, meta: { title: 'menu.proofs' } },
  { path: '/mint', name: 'mint', component: MintView, meta: { title: 'menu.mint' } },
  { path: '/exchange-rate', name: 'exchange-rate', component: ExchangeRateView, meta: { title: 'menu.exchangeRate' } },
  { path: '/credit-logs', name: 'credit-logs', component: CreditLogsView, meta: { title: 'menu.creditLogs' } },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
];

// 用 hash 路由，静态部署 / 开发都无需服务器端重写规则
const router = createRouter({ history: createWebHashHistory(), routes });

// 登录守卫：未登录访问受保护页面跳登录页（带 redirect），已登录访问登录页跳概览
router.beforeEach((to) => {
  const { isLoggedIn } = useAuth();
  if (!to.meta.public && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && isLoggedIn()) {
    return { path: '/dashboard' };
  }
});

// 让 request.js 的 401 处理能拿到 router 跳转
bindRouter(router);

export default router;
