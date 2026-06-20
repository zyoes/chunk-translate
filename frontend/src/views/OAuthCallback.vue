<template>
  <div class="callback-container">
    <el-result
      v-if="!error"
      icon="success"
      title="登录成功"
      sub-title="正在跳转..."
    />
    <el-result
      v-else
      icon="error"
      title="登录失败"
      sub-title="无法获取认证信息，请重试"
    >
      <template #extra>
        <el-button type="primary" @click="retry">重新登录</el-button>
        <el-button @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

/**
 * OAuth2 登录回调页面。
 * 后端 OAuth2 成功后 302 重定向到此页面，accessToken/refreshToken 通过 URL query 参数传递。
 * 将 token 存入 localStorage 后跳转到主页；token 缺失时显示错误提示和重试入口。
 */
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
/** 是否发生错误（token 缺失） */
const error = ref(false)

onMounted(() => {
  const { accessToken, refreshToken } = route.query
  if (accessToken) {
    localStorage.setItem('accessToken', accessToken)
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken)
    }
    router.replace('/')
  } else {
    error.value = true
  }
})

/** 重新发起 GitHub OAuth2 授权流程 */
function retry() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}

/** 跳转回主页 */
function goHome() {
  router.replace('/')
}
</script>

<style scoped>
.callback-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
}
</style>
