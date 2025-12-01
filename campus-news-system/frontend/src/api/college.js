import request from '@/utils/request'

export const getCollegeList = () => {
  return request({
    url: '/college/list',
    method: 'get'
  })
}

export const createCollege = (data) => {
  return request({
    url: '/college/create',
    method: 'post',
    data
  })
}

export const updateCollege = (data) => {
  return request({
    url: '/college/update',
    method: 'put',
    data
  })
}

export const deleteCollege = (id) => {
  return request({
    url: `/college/delete/${id}`,
    method: 'delete'
  })
}
