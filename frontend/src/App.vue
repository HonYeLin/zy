<script setup lang="ts">
import MapContainer from './components/MapContainer.vue'
</script>

<template>
  <div class="app-wrapper">
    <header class="app-header">
      <div class="logo">
        <svg viewBox="0 0 24 24" width="40" height="40" fill="#81C784" stroke="none">
          <circle cx="7.5" cy="9.5" r="2" />
          <circle cx="12" cy="6.5" r="2.5" />
          <circle cx="16.5" cy="9.5" r="2" />
          <path d="M12 11c-1.8 0-3.5 1.5-3.5 3.3 0 1.8 1.5 3.2 3.5 3.2s3.5-1.4 3.5-3.2c0-1.8-1.7-3.3-3.5-3.3z"/>
        </svg>
      </div>
      <div class="title-container">
        <h1>爪印</h1>
        <p>让校园里的小生命都被温柔看见</p>
      </div>
    </header>
    
    <main class="app-main">
      <div class="layout-container">
        <!-- 左侧地图区域 (佔據主要寬度) -->
        <section class="map-section">
          <h2 class="section-title">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="inline-icon">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
              <circle cx="12" cy="10" r="3"></circle>
            </svg>
            附近足迹
          </h2>
          <div class="map-box">
            <MapContainer />
          </div>
        </section>

        <!-- 右侧信息与控制侧面板 -->
        <div class="side-panel">
          <section class="actions-section">
            <h2 class="section-title">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="inline-icon">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
              更多功能
            </h2>
            <div class="button-grid">
              <button class="action-btn ai-btn">
                <span class="icon">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#9C27B0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M15 4V2M15 16v-2M8 9h2M20 9h-2M17.8 5.1l-1.4 1.4M17.8 12.9l-1.4-1.4M12.2 5.1l1.4 1.4M12.2 12.9l1.4-1.4"/>
                    <path d="M4 20l7-7 3 3-7 7z"/>
                  </svg>
                </span>
                <span class="text">AI 行为推理</span>
              </button>
              <button class="action-btn directory-btn">
                <span class="icon">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#FF9800" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                  </svg>
                </span>
                <span class="text">动物图鉴</span>
              </button>
              <button class="action-btn alert-btn">
                <span class="icon">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#F44336" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                    <line x1="12" y1="9" x2="12" y2="13"></line>
                    <line x1="12" y1="17" x2="12.01" y2="17"></line>
                  </svg>
                </span>
                <span class="text">异常预警</span>
              </button>
            </div>
          </section>

          <!-- 守护统计看板与小常识 -->
          <section class="stat-section">
            <h2 class="section-title">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="inline-icon">
                <line x1="18" y1="20" x2="18" y2="10"></line>
                <line x1="12" y1="20" x2="12" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="14"></line>
              </svg>
              校园守护统计
            </h2>
            <div class="stat-card">
              <div class="stat-item">
                <span class="stat-val">3</span>
                <span class="stat-lbl">建档小生命</span>
              </div>
              <div class="stat-item">
                <span class="stat-val">28</span>
                <span class="stat-lbl">累计足迹数</span>
              </div>
              <div class="stat-item">
                <span class="stat-val">100%</span>
                <span class="stat-lbl">AI 识别率</span>
              </div>
            </div>

            <div class="tip-card">
              <h3>🐾 地图使用引导</h3>
              <p>直接点击地图上的任意空白位置，可在该处悬浮生成一个绿色的临时标记，再次点击绿色标记便能快捷录入新的发现记录。点击主下悬浮爪印，可实现自动定位记录！</p>
            </div>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<style>
/* Pet Natural Theme Styles */
body, html {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Nunito', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  background-color: #F6F9F4; /* Very light earthy green */
  color: #2F4F4F; /* Dark slate gray */
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-header {
  background-color: #81C784; /* Soft nature green */
  color: white;
  padding: 1.5rem 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  box-shadow: 0 4px 12px rgba(129, 199, 132, 0.3);
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
}

.app-header .logo {
  font-size: 2.5rem;
  background: white;
  border-radius: 50%;
  padding: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.title-container h1 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.title-container p {
  margin: 0.2rem 0 0 0;
  font-size: 0.95rem;
  opacity: 0.9;
}

.app-main {
  flex: 1;
  max-width: 1400px; /* 扩大最大宽度以消除宽屏两侧过多空白 */
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  box-sizing: border-box;
}

.layout-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.section-title {
  font-size: 1.2rem;
  color: #558B2F; /* Deep olive green */
  margin-top: 0;
  margin-bottom: 1.2rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-bottom: 2px solid rgba(85, 139, 47, 0.15);
  padding-bottom: 8px;
}

.inline-icon {
  color: #81C784;
}

.map-section {
  flex: 1;
}

.map-box {
  height: 450px;
  border-radius: 24px;
  overflow: visible;
  box-shadow: 0 10px 35px rgba(0,0,0,0.06);
  border: 4px solid white;
  background: #E8F5E9;
  position: relative;
  transition: all 0.3s;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
}

.action-btn {
  background: white;
  border: none;
  border-radius: 16px;
  padding: 1.5rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0,0,0,0.03);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.action-btn:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.08);
}

.action-btn .icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn .text {
  font-weight: 700;
  color: #2F4F4F;
  font-size: 0.95rem;
}

.ai-btn:hover { border-bottom: 4px solid #9C27B0; }
.directory-btn:hover { border-bottom: 4px solid #FF9800; }
.alert-btn:hover { border-bottom: 4px solid #F44336; }

/* 统计看板小卡片 */
.stat-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem 1rem;
  display: flex;
  justify-content: space-around;
  box-shadow: 0 4px 15px rgba(0,0,0,0.03);
  border: 1px solid rgba(0,0,0,0.01);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-val {
  font-size: 1.6rem;
  font-weight: 800;
  color: #2E7D32;
}

.stat-lbl {
  font-size: 0.75rem;
  color: #666;
  font-weight: 700;
  margin-top: 6px;
}

/* 引导提示卡片 */
.tip-card {
  background: linear-gradient(135deg, rgba(129, 199, 132, 0.12), rgba(129, 199, 132, 0.05));
  border-left: 4px solid #81C784;
  border-radius: 16px;
  padding: 1.2rem 1.5rem;
  box-sizing: border-box;
  margin-top: 1rem;
}

.tip-card h3 {
  margin: 0 0 0.5rem 0;
  font-size: 0.95rem;
  color: #2E7D32;
  font-weight: 700;
  display: flex;
  align-items: center;
}

.tip-card p {
  margin: 0;
  font-size: 0.82rem;
  color: #4B6E4B;
  line-height: 1.6;
}

/* 大屏响应式分栏布局 */
@media (min-width: 1024px) {
  .layout-container {
    display: grid;
    grid-template-columns: 1fr 380px; /* 两栏：左侧地图，右侧控制侧栏 */
    gap: 2rem;
    align-items: start;
  }

  .map-box {
    height: 580px; /* 大屏下大幅度增加地图高度以凸显沉浸感 */
  }

  .button-grid {
    grid-template-columns: 1fr; /* 侧栏里按钮纵向排布 */
    gap: 0.8rem;
  }

  .action-btn {
    flex-direction: row; /* 按钮内部图标与文字横向展示 */
    justify-content: flex-start;
    padding: 1.2rem 1.8rem;
    gap: 1.2rem;
  }
}
</style>
