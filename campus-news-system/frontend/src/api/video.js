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

// 获取用户点赞的视频列表
export function getLikedVideos(params) {
  return request({
    url: '/video/liked',
    method: 'get',
    params
  })
}

// 获取用户上传的视频列表
export function getMyVideos(params) {
  return request({
    url: '/video/my',
    method: 'get',
    params
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

// ========== 视频评论 API ==========

// 获取视频评论列表
export function getVideoComments(videoId, params) {
  return request({
    url: `/video/comment/list/${videoId}`,
    method: 'get',
    params
  })
}

// 发表评论
export function createVideoComment(data) {
  return request({
    url: '/video/comment/create',
    method: 'post',
    data
  })
}

// 获取评论回复
export function getCommentReplies(commentId) {
  return request({
    url: `/video/comment/replies/${commentId}`,
    method: 'get'
  })
}

// 删除评论
export function deleteVideoComment(commentId) {
  return request({
    url: `/video/comment/delete/${commentId}`,
    method: 'delete'
  })
}

// 点赞/取消点赞评论
export function toggleCommentLike(commentId) {
  return request({
    url: `/video/comment/like/${commentId}`,
    method: 'post'
  })
}

// ========== 新增 YouTube 风格 API ==========

// 获取相关视频推荐
export function getRelatedVideos(videoId, count = 10) {
  return request({
    url: `/video/related/${videoId}`,
    method: 'get',
    params: { count }
  })
}

// 获取用户频道信息
export function getChannelInfo(userId) {
  return request({
    url: `/video/channel/${userId}`,
    method: 'get'
  })
}

// 获取频道视频列表
export function getChannelVideos(userId, params) {
  return request({
    url: `/video/channel/${userId}/videos`,
    method: 'get',
    params
  })
}

// 搜索视频（高级搜索）
export function searchVideos(params) {
  return request({
    url: '/video/search',
    method: 'get',
    params
  })
}

// 获取搜索建议
export function getSearchSuggestions(keyword) {
  return request({
    url: '/video/search/suggestions',
    method: 'get',
    params: { keyword }
  })
}

// 获取最新视频
export function getLatestVideos(count = 10) {
  return request({
    url: '/video/latest',
    method: 'get',
    params: { count }
  })
}

// 获取视频统计信息
export function getVideoStats(videoId) {
  return request({
    url: `/video/stats/${videoId}`,
    method: 'get'
  })
}

// ========== 视频历史记录 API ==========

// 添加观看历史
export function addWatchHistory(videoId) {
  return request({
    url: `/video/history/add/${videoId}`,
    method: 'post'
  })
}

// 获取观看历史
export function getWatchHistory(params) {
  return request({
    url: '/video/history/list',
    method: 'get',
    params
  })
}

// 删除单条历史记录
export function removeHistory(videoId) {
  return request({
    url: `/video/history/remove/${videoId}`,
    method: 'delete'
  })
}

// 清空观看历史
export function clearHistory() {
  return request({
    url: '/video/history/clear',
    method: 'delete'
  })
}

// 标记/取消不喜欢
export function toggleDislike(videoId) {
  return request({
    url: `/video/history/dislike/${videoId}`,
    method: 'post'
  })
}

// 检查是否不喜欢
export function checkDislike(videoId) {
  return request({
    url: `/video/history/dislike/check/${videoId}`,
    method: 'get'
  })
}

// 添加/移除稍后观看
export function toggleWatchLater(videoId) {
  return request({
    url: `/video/history/watchlater/${videoId}`,
    method: 'post'
  })
}

// 获取稍后观看列表
export function getWatchLaterList(params) {
  return request({
    url: '/video/history/watchlater/list',
    method: 'get',
    params
  })
}

// 保存观看进度
export function saveWatchProgress(videoId, seconds) {
  return request({
    url: `/video/history/progress/${videoId}`,
    method: 'post',
    data: { seconds }
  })
}

// 获取观看进度
export function getWatchProgress(videoId) {
  return request({
    url: `/video/history/progress/${videoId}`,
    method: 'get'
  })
}
