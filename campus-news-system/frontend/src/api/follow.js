import request from '@/utils/request'

/**
 * 关注模块 API
 */

/**
 * 关注/取消关注用户
 * @param {number} userId - 用户ID
 */
export const toggleFollow = (userId) => {
  return request({
    url: `/follow/${userId}`,
    method: 'post'
  })
}

/**
 * 检查是否已关注
 * @param {number} userId - 用户ID
 */
export const checkFollow = (userId) => {
  return request({
    url: `/follow/check/${userId}`,
    method: 'get'
  })
}

/**
 * 获取我的关注列表
 * @param {Object} params - 分页参数
 */
export const getMyFollowing = (params) => {
  return request({
    url: '/follow/following',
    method: 'get',
    params
  })
}

/**
 * 获取指定用户的关注列表
 * @param {number} userId - 用户ID
 * @param {Object} params - 分页参数
 */
export const getUserFollowing = (userId, params) => {
  return request({
    url: `/follow/following/${userId}`,
    method: 'get',
    params
  })
}

/**
 * 获取我的粉丝列表
 * @param {Object} params - 分页参数
 */
export const getMyFollowers = (params) => {
  return request({
    url: '/follow/followers',
    method: 'get',
    params
  })
}

/**
 * 获取指定用户的粉丝列表
 * @param {number} userId - 用户ID
 * @param {Object} params - 分页参数
 */
export const getUserFollowers = (userId, params) => {
  return request({
    url: `/follow/followers/${userId}`,
    method: 'get',
    params
  })
}

/**
 * 获取关注动态（关注的人发布的文章）
 * @param {Object} params - 分页参数
 */
export const getFollowFeed = (params) => {
  return request({
    url: '/follow/feed',
    method: 'get',
    params
  })
}

/**
 * 获取推荐关注的用户
 * @param {number} limit - 数量限制
 */
export const getRecommendUsers = (limit = 5) => {
  return request({
    url: '/follow/recommend',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取用户的关注统计信息
 * @param {number} userId - 用户ID
 */
export const getFollowStats = (userId) => {
  return request({
    url: `/follow/stats/${userId}`,
    method: 'get'
  })
}
