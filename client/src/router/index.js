import { createRouter, createWebHashHistory } from 'vue-router';
import { useAuth } from '../composables/useAuth.js';
import { bindRouter } from '../api/request.js';

import LoginView from '../views/LoginView.vue';
import ProofListView from '../views/ProofListView.vue';
import BuyView from '../views/BuyView.vue';

// meta.title 存 i18n key，由 App.vue 头部经 t() 翻译展示
const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/', name: 'proofs', component: ProofListView, meta: { title: 'menu.proofs' } },
  { path: '/buy', name: 'buy', component: BuyView, meta: { title: 'menu.buy' } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
];

// 用 hash 路由，静态部署 / 开发都无需服务器端重写规则
const router = createRouter({ history: createWebHashHistory(), routes });

// 登录守卫：未登录访问受保护页面跳登录页（带 redirect），已登录访问登录页跳存证页
router.beforeEach((to) => {
  const { isLoggedIn } = useAuth();
  if (!to.meta.public && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.path === '/login' && isLoggedIn()) {
    return { path: '/' };
  }
});

// 让 request.js 的 401 处理能拿到 router 跳转
bindRouter(router);

export default router;
