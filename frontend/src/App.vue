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
        <h1>Campus Paw-Track</h1>
        <p>让校园里的小生命都被温柔看见</p>
      </div>
    </header>
    
    <main class="app-main">
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
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#9C27B0" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M15 4V2M15 16v-2M8 9h2M20 9h-2M17.8 5.1l-1.4 1.4M17.8 12.9l-1.4-1.4M12.2 5.1l1.4 1.4M12.2 12.9l1.4-1.4"/>
                <path d="M4 20l7-7 3 3-7 7z"/>
              </svg>
            </span>
            <span class="text">AI 行为推理</span>
          </button>
          <button class="action-btn directory-btn">
            <span class="icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#FF9800" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
              </svg>
            </span>
            <span class="text">动物图鉴</span>
          </button>
          <button class="action-btn alert-btn">
            <span class="icon">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#F44336" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </span>
            <span class="text">异常预警</span>
          </button>
        </div>
      </section>
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
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1rem;
  box-sizing: border-box;
}

.section-title {
  font-size: 1.2rem;
  color: #558B2F; /* Deep olive green */
  margin-bottom: 1rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.map-section {
  margin-bottom: 2rem;
}

.map-box {
  height: 400px;
  border-radius: 24px;
  overflow: visible;
  box-shadow: 0 10px 30px rgba(0,0,0,0.08);
  border: 4px solid white;
  background: #E8F5E9;
  position: relative;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
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
  gap: 0.5rem;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.1);
}

.action-btn .icon {
  font-size: 2rem;
}

.action-btn .text {
  font-weight: 600;
  color: #4A4A4A;
}

.ai-btn:hover { border-bottom: 4px solid #9C27B0; }
.directory-btn:hover { border-bottom: 4px solid #FF9800; }
.alert-btn:hover { border-bottom: 4px solid #F44336; }
</style>
