import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

import VueAMap, { initAMapApiLoader } from '@vuemap/vue-amap';
import '@vuemap/vue-amap/dist/style.css';

// TODO: Replace with your actual AMap Web JS API Key and Security Code
initAMapApiLoader({
  key: '4fece203a8eccd55ab59ac6456459ff7',
  securityJsCode: '82d1af96c1bbd634dcb0a1dc452db519',
  plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Geocoder', 'AMap.MarkerCluster', 'AMap.Geolocation'],
});

const app = createApp(App)
app.use(VueAMap)
app.mount('#app')
