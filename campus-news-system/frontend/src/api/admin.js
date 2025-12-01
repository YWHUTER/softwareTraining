import request from '@/utils/request'

/**
 * 管理后台 API
 */

/**
 * 获取系统统计数据
 * @returns {Promise<Object>} 统计数据
 */
export const getStatistics = () => {
  return request({
    url: '/admin/statistics',
    method: 'get'
  })
}

/**
 * 获取图表数据
 * @returns {Promise<Object>} 图表数据
 */
export const getChartData = () => {
  return request({
    url: '/admin/chart-data',
    method: 'get'
  })
}
