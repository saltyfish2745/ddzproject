import axios, { AxiosError, AxiosResponse } from '@ohos/axios'

export const request= axios.create({
  baseURL: 'http://47.104.6.118:8080',
  timeout:5000,
  headers:{
    'Content-Type':'application/json'
  }
})

// 添加响应拦截器
request.interceptors.response.use((response:AxiosResponse)=> {
  // 对响应数据做点什么
  // 判断响应状态码
  if (response.status>=200&&response.status<300) {
    return response.data//直接返回data数据后续靠请求泛型指定类型
  }
  return Promise.reject(response);
}, (error:AxiosError)=> {
  // 对响应错误做点什么
  return Promise.reject(error);
});