import request from '@/utils/request'

// 获取通知列表
export const getNotifications = (params) => {
  return request({
    url: '/notification/list',
    method: 'get',
    params
  })
}

// 获取未读通知数量
export const getUnreadCount = () => {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

// 标记单个通知为已读
export const markAsRead = (id) => {
  return request({
    url: `/notification/read/${id}`,
    method: 'put'
  })
}

// 标记所有通知为已读
export const markAllAsRead = () => {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}
