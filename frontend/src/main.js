import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'font-awesome/css/font-awesome.min.css'
import axios from 'axios';

// 创建应用实例
const app = createApp(App);

// 注册 Element Plus 插件
app.use(ElementPlus);

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// 使用路由
app.use(router);

// 挂载应用
app.mount('#app');

// 设置跨域请求时携带 Cookie
axios.defaults.withCredentials = true;

// 设置基础URL和凭证
axios.defaults.baseURL = 'http://';
axios.defaults.withCredentials = true; // 关键：允许发送 cookies
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

// 添加响应拦截器处理会话过期
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      // 处理未认证错误
      console.log('会话已过期，请重新登录');
    }
    return Promise.reject(error);
  }
);
