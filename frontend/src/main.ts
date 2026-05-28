import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import axios from 'axios'

import VueAMap, { initAMapApiLoader } from '@vuemap/vue-amap';
import '@vuemap/vue-amap/dist/style.css';

// TODO: Replace with your actual AMap Web JS API Key and Security Code
initAMapApiLoader({
  key: import.meta.env.VITE_AMAP_KEY,
  securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE,
  plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Geocoder', 'AMap.MarkerCluster', 'AMap.Geolocation'],
});

// 全局请求拦截器：每次发请求时自动从 sessionStorage 读取最新 admin token
axios.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('admin_token');
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  
  // 禁用 GET 请求的浏览器缓存，解决后台数据无法实时更新的问题
  if (config.method === 'get') {
    config.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate';
    config.headers['Pragma'] = 'no-cache';
    config.headers['Expires'] = '0';
    // 强制附加时间戳避免部分极端浏览器的强缓存
    config.params = { ...config.params, _t: Date.now() };
  }
  
  return config;
});

// 全局响应拦截器：处理 token 过期或失效的情况
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // 只有在后台页面遇到 401/403 才执行退出
      if (window.location.pathname.startsWith('/admin')) {
        sessionStorage.removeItem('admin_token');
        window.location.reload();
      }
    }
    return Promise.reject(error);
  }
);

import router from './router';

const app = createApp(App)
app.use(router)
app.use(VueAMap)
app.mount('#app')
