import axios from 'axios'

// Python 算法服务地址 - 直接调用，不经过 Java 后端
const ALGORITHM_BASE_URL = 'http://localhost:5000'

// 创建专用于算法服务的 axios 实例
const algorithmRequest = axios.create({
  baseURL: ALGORITHM_BASE_URL,
  timeout: 10000
})

/**
 * 获取用户完整画像
 */
export function getUserProfile(userId) {
  return algorithmRequest.get(`/api/profile/${userId}`).then(res => res.data)
}

/**
 * 获取用户兴趣标签
 */
export function getUserInterests(userId, topN = 10) {
  return algorithmRequest.get(`/api/profile/${userId}/interests`, {
    params: { top_n: topN }
  }).then(res => res.data)
}

/**
 * 获取用户活跃时间模式
 */
export function getUserActivity(userId) {
  return algorithmRequest.get(`/api/profile/${userId}/activity`).then(res => res.data)
}

/**
 * 获取相似用户
 */
export function getSimilarUsers(userId, topN = 5) {
  return algorithmRequest.get(`/api/profile/${userId}/similar-users`, {
    params: { top_n: topN }
  }).then(res => res.data)
}
