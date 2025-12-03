import request from '@/utils/request'

/**
 * 获取热门标签（用于标签云）
 */
export function getHotTags(limit = 30) {
  return request({
    url: '/tag/hot',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取所有标签
 */
export function getAllTags() {
  return request({
    url: '/tag/list',
    method: 'get'
  })
}

/**
 * 获取文章的标签
 */
export function getArticleTags(articleId) {
  return request({
    url: `/tag/article/${articleId}`,
    method: 'get'
  })
}
