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

// 全局请求拦截器：每次发请求时自动从 localStorage 读取最新 admin token
// 这样子页面 onMounted 时不会存在 token 还未就绪的竞态问题
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

import router from './router';

const app = createApp(App)
app.use(router)
app.use(VueAMap)
app.mount('#app')
