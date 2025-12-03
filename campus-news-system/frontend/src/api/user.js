import request from '@/utils/request'

export const getUserList = (params) => {
  return request({
    url: '/user/list',
    method: 'get',
    params
  })
}

// 搜索用户（用于@提及，不需要管理员权限）
export const searchUsers = (params) => {
  return request({
    url: '/user/search',
    method: 'get',
    params
  })
}

export const updateUserStatus = (userId, status) => {
  return request({
    url: `/user/status/${userId}`,
    method: 'put',
    params: { status }
  })
}

// 上传文件
export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 更新用户头像
export const updateAvatar = (avatar) => {
  return request({
    url: '/user/avatar',
    method: 'put',
    params: { avatar }
  })
}

// 更新用户信息
export const updateUserInfo = (data) => {
  return request({
    url: '/user/update',
    method: 'put',
    data
  })
}
