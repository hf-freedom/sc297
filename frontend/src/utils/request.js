import axios from 'axios'
import { Message } from 'element-ui'

const service = axios.create({
  baseURL: '',
  timeout: 300000
})

service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res.data
    }
  },
  error => {
    console.log('err' + error)
    Message.error(error.message)
    return Promise.reject(error)
  }
)

export default service
