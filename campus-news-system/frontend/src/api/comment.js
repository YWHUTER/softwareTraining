import request from '@/utils/request'

export const getCommentList = (articleId) => {
  return request({
    url: '/comment/list',
    method: 'get',
    params: { articleId }
  })
}

export const createComment = (data) => {
  return request({
    url: '/comment/create',
    method: 'post',
    data
  })
}

export const deleteComment = (id) => {
  return request({
    url: `/comment/delete/${id}`,
    method: 'delete'
  })
}
