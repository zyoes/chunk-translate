/**
 * Vue Router 路由配置。
 * 使用 HTML5 History 模式，所有路由懒加载。
 * meta.title 用于设置浏览器标签页标题。
 */
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ==================== 核心功能页 ====================
    {
      path: '/',
      name: 'Translate',
      component: () => import('../views/Translate.vue'),
      meta: { title: 'AI 文档翻译' }
    },
    // ==================== 管理后台 ====================
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../views/AdminView.vue'),
      meta: { title: '后台管理 - AI 文档翻译' }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录 - AI 文档翻译' }
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('../views/ForgotPassword.vue'),
      meta: { title: '重置密码 - AI 文档翻译' }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { title: '个人主页 - AI 文档翻译' }
    },
    {
      path: '/oauth/callback',
      name: 'OAuthCallback',
      component: () => import('../views/OAuthCallback.vue'),
      meta: { title: '登录回调' }
    }
  ]
})

export default router
