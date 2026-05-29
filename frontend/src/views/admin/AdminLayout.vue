<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const appVersion = import.meta.env.VITE_APP_VERSION || 'v2.1.2';

const router = useRouter();
const isLoggedIn = ref(false);

onMounted(() => {
  const token = sessionStorage.getItem('admin_token');
  if (token) {
    isLoggedIn.value = true;
  } else {
    router.push('/');
  }
});

const handleLogout = async () => {
  try {
    await axios.post('/api/admin/auth/logout');
  } catch (e) {}
  sessionStorage.removeItem('admin_token');
  isLoggedIn.value = false;
  router.push('/');
};
</script>

<template>
  <div class="admin-app" v-if="isLoggedIn">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <h2><span class="logo-icon">🐾</span> 爪印管理</h2>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/admin/dashboard" class="nav-item">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
          数据看板
        </router-link>
        <router-link to="/admin/animals" class="nav-item">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
          档案管理
        </router-link>
        <router-link to="/admin/comments" class="nav-item">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path></svg>
          留言管理
        </router-link>
        <router-link to="/admin/images" class="nav-item">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
          图片管理
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <button @click="handleLogout" class="logout-btn">退出登录</button>
        <div class="admin-version">
          <span class="pulse-dot"></span>
          <span>系统版本: {{ appVersion }}</span>
        </div>
      </div>
    </aside>
    <main class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-left">
          <div class="welcome-text">欢迎回来，超级管理员</div>
        </div>
        <div class="topbar-right">
          <router-link to="/" class="back-home-btn">返回前台地图</router-link>
        </div>
      </header>
      <div class="admin-content-wrapper">
        <div class="admin-content">
          <router-view></router-view>
        </div>
      </div>
    </main>
  </div>
  <div class="admin-unauthorized" v-else>
    <p>未授权，正在返回首页...</p>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

.admin-app {
  display: flex;
  height: 100vh;
  background-color: #f1f5f9;
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  color: #334155;
}

/* Sidebar */
.admin-sidebar {
  width: 260px;
  background: #0f172a;
  color: #f8fafc;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 10px rgba(0,0,0,0.05);
  z-index: 10;
}

.sidebar-header {
  padding: 24px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: 0.5px;
}

.logo-icon {
  font-size: 24px;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: #cbd5e1;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.nav-icon {
  width: 20px;
  height: 20px;
  opacity: 0.85;
  transition: opacity 0.3s ease;
}

.nav-item:hover {
  background: rgba(255,255,255,0.05);
  color: #f8fafc;
}

.nav-item:hover .nav-icon {
  opacity: 1;
}

.router-link-active {
  background: #3b82f6 !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.router-link-active .nav-icon {
  opacity: 1;
}

.sidebar-footer {
  padding: 24px 16px;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.logout-btn {
  width: 100%;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
  padding: 12px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: #ef4444;
  color: white;
}

.admin-version {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
  font-family: 'Courier New', Courier, monospace;
}

.admin-version .pulse-dot {
  width: 6px;
  height: 6px;
  background-color: #10b981;
  border-radius: 50%;
  display: inline-block;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: admin-pulse 2s infinite;
}

@keyframes admin-pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 4px rgba(16, 185, 129, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

/* Main Area */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-topbar {
  height: 64px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  z-index: 5;
}

.welcome-text {
  font-weight: 500;
  color: #334155;
}

.back-home-btn {
  padding: 8px 16px;
  background: #f1f5f9;
  color: #334155;
  text-decoration: none;
  border-radius: 6px;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s;
}

.back-home-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.admin-content-wrapper {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
}

.admin-content {
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* Login box styles */
.admin-login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f8fafc;
  background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
  background-size: 20px 20px;
  font-family: 'Inter', system-ui, sans-serif;
}

.admin-login-box {
  background: white;
  padding: 48px 40px;
  border-radius: 16px;
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.login-logo {
  font-size: 48px;
  margin-bottom: 16px;
}

.admin-login-box h2 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
}

.login-subtitle {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 32px;
}

.login-error {
  background: #fef2f2;
  color: #ef4444;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
  font-weight: 500;
}

.form-group {
  margin-bottom: 20px;
}

.form-group input {
  width: 100%;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-sizing: border-box;
  font-size: 15px;
  color: #334155;
  transition: all 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  background: white;
}

.admin-login-btn {
  width: 100%;
  padding: 14px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  margin-top: 8px;
  transition: all 0.2s;
}

.admin-login-btn:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.back-link {
  display: inline-block;
  margin-top: 24px;
  color: #94a3b8;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.2s;
}

.back-link:hover {
  color: #475569;
}
</style>
