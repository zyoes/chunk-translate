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

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
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

function retry() {
  window.location.href = 'http://localhost:8080/oauth2/authorization/github'
}

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
