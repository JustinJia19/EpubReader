import { createRouter, createWebHistory } from 'vue-router';

// 定义路由规则
const routes = [
  {
    path: '/reader',
    name: 'Reader',
    component: () => import('@/components/ReaderComponent.vue'), // 阅读器页面
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/components/LoginComponent.vue'), // 登录页面
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/components/HomeComponent.vue'), // 主页面
  },
  {
    path: '/about-us',
    name: 'about-us',
    component: () => import('@/components/AboutUsComponent.vue'), // 关于我们界面
  },
  {
    path: '/', // 默认路径
    redirect: '/home', // 重定向到主页面
  },
];

// 创建路由实例
const router = createRouter({
  history: createWebHistory(process.env.BASE_URL), // 使用 HTML5 History 模式
  routes, // 路由规则
});

// 导出路由实例
export default router;