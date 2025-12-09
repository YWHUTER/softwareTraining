import request from '@/utils/request'

export const getArticleList = (params) => {
  return request({
    url: '/article/list',
    method: 'get',
    params
  })
}

export const getArticleDetail = (id) => {
  return request({
    url: `/article/detail/${id}`,
    method: 'get'
  })
}

export const createArticle = (data) => {
  return request({
    url: '/article/create',
    method: 'post',
    data
  })
}

export const updateArticle = (id, data) => {
  return request({
    url: `/article/update/${id}`,
    method: 'put',
    data
  })
}

export const deleteArticle = (id) => {
  return request({
    url: `/article/delete/${id}`,
    method: 'delete'
  })
}

export const toggleLike = (id) => {
  return request({
    url: `/article/like/${id}`,
    method: 'post'
  })
}

export const toggleFavorite = (id) => {
  return request({
    url: `/article/favorite/${id}`,
    method: 'post'
  })
}

export const togglePinned = (id, isPinned) => {
  return request({
    url: `/article/pin/${id}`,
    method: 'put',
    params: { isPinned }
  })
}

export const approveArticle = (id, isApproved) => {
  return request({
    url: `/article/approve/${id}`,
    method: 'put',
    params: { isApproved }
  })
}

export const getPublicStats = () => {
  return request({
    url: '/article/public/stats',
    method: 'get'
  })
}
