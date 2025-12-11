import request from '@/utils/request'

// 获取视频列表
export function getVideoList(params) {
  return request({
    url: '/video/list',
    method: 'get',
    params
  })
}

// 获取视频详情
export function getVideoDetail(id) {
  return request({
    url: `/video/detail/${id}`,
    method: 'get'
  })
}

// 获取视频分类
export function getVideoCategories() {
  return request({
    url: '/video/categories',
    method: 'get'
  })
}

// 获取热门视频
export function getHotVideos(count = 10) {
  return request({
    url: '/video/hot',
    method: 'get',
    params: { count }
  })
}

// 上传视频文件
export function uploadVideoFile(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/video/upload/file',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  })
}

// 上传缩略图
export function uploadThumbnail(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/video/upload/thumbnail',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 完整上传视频(文件+信息)
export function uploadVideoComplete(data, onProgress) {
  const formData = new FormData()
  formData.append('video', data.video)
  if (data.thumbnail) {
    formData.append('thumbnail', data.thumbnail)
  }
  formData.append('title', data.title)
  if (data.description) {
    formData.append('description', data.description)
  }
  if (data.categoryId) {
    formData.append('categoryId', data.categoryId)
  }
  if (data.channelName) {
    formData.append('channelName', data.channelName)
  }
  if (data.duration) {
    formData.append('duration', data.duration)
  }
  if (data.durationSeconds) {
    formData.append('durationSeconds', data.durationSeconds)
  }
  
  return request({
    url: '/video/upload/complete',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
  })
}

// 创建视频记录(仅信息，视频URL已有)
export function createVideo(data) {
  return request({
    url: '/video/create',
    method: 'post',
    data
  })
}

// 更新视频信息
export function updateVideo(id, data) {
  return request({
    url: `/video/update/${id}`,
    method: 'put',
    data
  })
}

// 删除视频
export function deleteVideo(id) {
  return request({
    url: `/video/delete/${id}`,
    method: 'delete'
  })
}

// 点赞/取消点赞
export function toggleVideoLike(id) {
  return request({
    url: `/video/like/${id}`,
    method: 'post'
  })
}

// 审核视频(管理员)
export function approveVideo(id, isApproved) {
  return request({
    url: `/video/approve/${id}`,
    method: 'put',
    params: { isApproved }
  })
}
