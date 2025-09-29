import axios from 'axios'
import { message } from 'ant-design-vue'

const myAxios = axios.create({
  baseURL : '',
  timeout: 6000,
  withCredentials: true
})

myAxios.interceptors.request.use(
  function(config){
    return config
  },
  function(error) {
    return Promise.reject(error)
  }
)

//全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    if (data.code === 40100){
      if(!response.request.responseURL.includes('user/get/login') && !window.location.pathname.includes('/user/login')) {
        message.warning('请先登录')
        window.location.href=`/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
function (error){
  return Promise.reject(error)
  }
)

export default myAxios
