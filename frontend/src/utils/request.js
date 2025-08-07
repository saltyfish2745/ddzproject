import axios from 'axios';
import { ElLoading } from 'element-plus';
import { BASE_URL } from '../config/url'; // Adjust the import path as necessary

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 5000
});

let loadingInstance;

// 添加请求拦截器
request.interceptors.request.use(function (config) {
  // 在发送请求之前做些什么
  // 显示加载动画
  loadingInstance = ElLoading.service({
    lock: true,
    text: '加载中...',
    background: 'rgba(0, 0, 0, 0.7)'
  });
  
  return config;
}, function (error) {
  // 对请求错误做些什么
  loadingInstance?.close();
  return Promise.reject(error);
});

// 添加响应拦截器
request.interceptors.response.use(function (response) {
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么
    loadingInstance?.close();
    return response;
  }, function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    loadingInstance?.close();
    return Promise.reject(error);
});
  
export default request;