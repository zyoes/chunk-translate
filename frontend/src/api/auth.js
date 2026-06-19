import request from './request'

// 邮箱注册
export function register(data) {
  return request.post('/auth/register', data)
}

// 邮箱登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 获取当前用户信息
export function getCurrentUser() {
  return request.get('/auth/me')
}
