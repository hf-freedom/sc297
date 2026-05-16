import request from '../utils/request'

export function uploadTask(data) {
  return request({
    url: '/api/tasks/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getTaskList() {
  return request({
    url: '/api/tasks',
    method: 'get'
  })
}

export function getTaskDetail(taskId) {
  return request({
    url: `/api/tasks/${taskId}`,
    method: 'get'
  })
}

export function retryTask(taskId) {
  return request({
    url: `/api/tasks/${taskId}/retry`,
    method: 'post'
  })
}

export function deleteTask(taskId) {
  return request({
    url: `/api/tasks/${taskId}`,
    method: 'delete'
  })
}

export function getCacheData() {
  return request({
    url: '/api/tasks/cache/data',
    method: 'get'
  })
}

export function getCacheSize() {
  return request({
    url: '/api/tasks/cache/size',
    method: 'get'
  })
}

export function checkTaskTypeRunning(taskType) {
  return request({
    url: `/api/tasks/check-type/${taskType}`,
    method: 'get'
  })
}
