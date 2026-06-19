import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Translate',
      component: () => import('../views/Translate.vue'),
      meta: { title: 'AI 文档翻译' }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '登录 - AI 文档翻译' }
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
