import request from '@/utils/request'

/**
 * 获取个性化推荐
 * @param {number} count 推荐数量
 * @param {number[]} excludeIds 排除的文章ID
 */
export function getPersonalizedRecommendations(count = 10, excludeIds = []) {
  return request({
    url: '/recommendation/personalized',
    method: 'get',
    params: {
      count,
      excludeIds: excludeIds.join(',')
    }
  })
}

/**
 * 获取相似文章推荐
 * @param {number} articleId 文章ID
 * @param {number} count 推荐数量
 */
export function getSimilarArticles(articleId, count = 6) {
  return request({
    url: `/recommendation/similar/${articleId}`,
    method: 'get',
    params: { count }
  })
}

/**
 * 获取热门推荐
 * @param {number} count 推荐数量
 */
export function getHotRecommendations(count = 10) {
  return request({
    url: '/recommendation/hot',
    method: 'get',
    params: { count }
  })
}

/**
 * 检查推荐服务状态
 */
export function checkRecommendationHealth() {
  return request({
    url: '/recommendation/health',
    method: 'get'
  })
}

// ==================== 视频推荐 API ====================

/**
 * 获取个性化视频推荐
 * @param {number} count 推荐数量
 * @param {number[]} excludeIds 排除的视频ID
 * @param {number} categoryId 分类ID(可选)
 */
export function getPersonalizedVideoRecommendations(count = 10, excludeIds = [], categoryId = null) {
  const params = {
    count,
    excludeIds: excludeIds.join(',')
  }
  if (categoryId) {
    params.categoryId = categoryId
  }
  return request({
    url: '/recommendation/video/personalized',
    method: 'get',
    params
  })
}

/**
 * 获取热门视频推荐
 * @param {number} count 推荐数量
 * @param {number} categoryId 分类ID(可选)
 */
export function getHotVideoRecommendations(count = 10, categoryId = null) {
  const params = { count }
  if (categoryId) {
    params.categoryId = categoryId
  }
  return request({
    url: '/recommendation/video/hot',
    method: 'get',
    params
  })
}

/**
 * 获取相似视频推荐
 * @param {number} videoId 视频ID
 * @param {number} count 推荐数量
 */
export function getSimilarVideos(videoId, count = 10) {
  return request({
    url: `/recommendation/video/similar/${videoId}`,
    method: 'get',
    params: { count }
  })
}
