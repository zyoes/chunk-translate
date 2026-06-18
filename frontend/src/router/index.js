import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Translate',
      component: () => import('../views/Translate.vue'),
      meta: { title: 'AI 文档翻译' }
    }
  ]
})

export default router
