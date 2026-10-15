import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as icons from '@element-plus/icons-vue';

import App from './App.vue';
import router from './router/index.js';
import i18n from './i18n/index.js';

const app = createApp(App);

// Element Plus 全量引入（用户端体量小，全量引入足够）；
// 组件语言由 App.vue 的 el-config-provider 按当前语言响应式提供
app.use(ElementPlus);
// 全局注册图标，模板里可直接用 <Wallet /> 等
for (const [name, comp] of Object.entries(icons)) {
  app.component(name, comp);
}
app.use(i18n);
app.use(router);
app.mount('#app');
