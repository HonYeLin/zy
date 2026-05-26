<script setup lang="ts">
import { ref, computed } from 'vue';
import axios from 'axios';

const center = ref([116.397428, 39.90923]); 
const zoom = ref(16);
const map = ref<any>(null);
const animalLogs = ref<any[]>([]);
const userLocation = ref<[number, number] | null>(null); // 用户当前定位位置

// Form UI State
const isLocating = ref(false);
const isLocatingOnly = ref(false);
const showModal = ref(false);
const currentLocation = ref({ lng: 0, lat: 0 });

// Form Data
const formType = ref('Cat'); // 'Cat' | 'Dog' | 'Other'
const formNickname = ref('');
const formFeatures = ref('');
const formBehaviorTag = ref(''); // '' | 'EATING' | 'SLEEPING' | 'PLAYING' | 'SUNBATHING' | 'WALKING' | 'OTHER'
const formQrCodeId = ref('');
const formTimeOffset = ref(0); // minutes offset: 0, 10, 30, 60, 120
const formPhotoUrl = ref('');
const isSubmitting = ref(false);

const isFormValid = computed(() => {
  return formNickname.value.trim().length > 0 || formFeatures.value.trim().length > 0;
});

const initMap = (mapInstance: any) => {
  map.value = mapInstance;
  fetchNearbyLogs();
  autoLocateUser(); // 打开页面自动获取当前位置并跳转
};

const fetchNearbyLogs = async () => {
  try {
    // 获取当前数据库中所有记录的实体
    const res = await axios.get('http://localhost:8080/api/locations/all');
    animalLogs.value = res.data;
  } catch (error) {
    console.error('获取所有数据失败:', error);
  }
};

// 自动定位用户并跳转居中
const autoLocateUser = () => {
  if (!map.value) return;
  if ((window as any).AMap && (window as any).AMap.Geolocation) {
    const geolocation = new (window as any).AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 8000,
      zoomToAccuracy: true,
      showMarker: false, // 我们自己在 Vue 中绘制更漂亮的脉冲水波纹定位点
      showCircle: true,
      panToLocation: true
    });

    geolocation.getCurrentPosition((status: string, result: any) => {
      if (status === 'complete') {
        userLocation.value = [result.position.lng, result.position.lat];
        center.value = [result.position.lng, result.position.lat];
        map.value.setCenter([result.position.lng, result.position.lat]);
      } else {
        console.warn('自动获取当前定位失败:', result);
      }
    });
  }
};

// 1. 点击侧边独立定位按钮：仅移动中心并更新用户定位
const handleLocateOnlyClick = () => {
  if (!map.value || isLocatingOnly.value) return;
  isLocatingOnly.value = true;
  
  if ((window as any).AMap && (window as any).AMap.Geolocation) {
    const geolocation = new (window as any).AMap.Geolocation({
      enableHighAccuracy: true, timeout: 5000, zoomToAccuracy: true,
      showMarker: false, showCircle: true
    });

    geolocation.getCurrentPosition((status: string, result: any) => {
      isLocatingOnly.value = false;
      if (status === 'complete') {
        userLocation.value = [result.position.lng, result.position.lat];
        center.value = [result.position.lng, result.position.lat];
        map.value.setCenter([result.position.lng, result.position.lat]);
      } else {
        alert('❌ 定位失败，请检查浏览器权限。');
      }
    });
  } else {
    isLocatingOnly.value = false;
    alert('地图定位插件未加载完成。');
  }
};

// 2. 点击主爪印按钮：获取当前位置并弹出表单
const handleAddClick = () => {
  if (!map.value || isLocating.value) return;
  isLocating.value = true;
  
  if ((window as any).AMap && (window as any).AMap.Geolocation) {
    const geolocation = new (window as any).AMap.Geolocation({
      enableHighAccuracy: true, timeout: 5000, zoomToAccuracy: true,
      showMarker: false, showCircle: true
    });

    geolocation.getCurrentPosition((status: string, result: any) => {
      isLocating.value = false;
      if (status === 'complete') {
        userLocation.value = [result.position.lng, result.position.lat];
        currentLocation.value = { lng: result.position.lng, lat: result.position.lat };
        center.value = [result.position.lng, result.position.lat];
        map.value.setCenter([result.position.lng, result.position.lat]);
        openModal();
      } else {
        alert('❌ 定位失败，请检查浏览器权限。为了测试，将使用地图中心点。');
        currentLocation.value = { lng: center.value[0], lat: center.value[1] };
        openModal();
      }
    });
  } else {
    isLocating.value = false;
    alert('地图定位插件未加载完成。');
  }
};

const openModal = () => {
  showModal.value = true;
  formType.value = 'Cat';
  formNickname.value = '';
  formFeatures.value = '';
  formBehaviorTag.value = '';
  formQrCodeId.value = '';
  formTimeOffset.value = 0;
  formPhotoUrl.value = '';
};

const closeModal = () => {
  showModal.value = false;
};

// 3. 提交表单 (对接后端 DTO 格式)
const submitMarker = async () => {
  if (!formNickname.value.trim() && !formFeatures.value.trim()) {
    alert("请至少填写一个昵称或特征！");
    return;
  }
  isSubmitting.value = true;
  
  try {
    await axios.post('http://localhost:8080/api/locations', {
      type: formType.value,
      nickname: formNickname.value.trim(),
      features: formFeatures.value.trim(),
      longitude: currentLocation.value.lng,
      latitude: currentLocation.value.lat,
      behaviorTag: formBehaviorTag.value,
      qrCodeId: formQrCodeId.value.trim(),
      timeOffset: formTimeOffset.value,
      photoUrl: formPhotoUrl.value.trim()
    });
    isSubmitting.value = false;
    closeModal();
    fetchNearbyLogs();
  } catch (error) {
    console.error('提交失败:', error);
    alert('提交失败，请确保后端已启动。');
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="map-wrapper">
    <div class="map-container">
      <el-amap :center="center" :zoom="zoom" @init="initMap" :show-label="false">
        <el-amap-marker
          v-for="log in animalLogs"
          :key="log.id"
          :position="[log.longitude, log.latitude]"
          :title="`${log.animal?.name || '未知小动物'} (${log.behaviorTag || '活动'}): ${log.description || '无特征描述'}`"
        >
          <div class="custom-marker" :class="log.animal?.breed?.toLowerCase()">
            <template v-if="log.animal?.breed === 'Cat'">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 5c.67 0 1.35.09 2 .26L18.5 2 17 6.5c2.3 1.9 3.5 4.7 3 7.5-.7 4-4 7-8 7s-7.3-3-8-7c-.5-2.8.7-5.6 3-7.5L5.5 2 10 5.26c.65-.17 1.33-.26 2-.26z" />
                <circle cx="9" cy="12" r="1.5" fill="currentColor" />
                <circle cx="15" cy="12" r="1.5" fill="currentColor" />
                <path d="M12 15c-.5.5-1.5.5-2 0m2 0c.5.5 1.5.5 2 0" />
              </svg>
            </template>
            <template v-else-if="log.animal?.breed === 'Dog'">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M19 12a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
                <path d="M7 6.5C5.5 7.5 4 10 4 12.5" />
                <path d="M17 6.5c1.5 1 3 3.5 3 6" />
                <path d="M12 13v1.5" />
                <circle cx="9" cy="10" r="1.5" fill="currentColor" />
                <circle cx="15" cy="10" r="1.5" fill="currentColor" />
                <path d="M11 15c.5.3 1.5.3 2 0" />
              </svg>
            </template>
            <template v-else>
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <circle cx="7.5" cy="9.5" r="2" />
                <circle cx="12" cy="6.5" r="2.5" />
                <circle cx="16.5" cy="9.5" r="2" />
                <path d="M12 11c-1.8 0-3.5 1.5-3.5 3.3 0 1.8 1.5 3.2 3.5 3.2s3.5-1.4 3.5-3.2c0-1.8-1.7-3.3-3.5-3.3z"/>
              </svg>
            </template>
          </div>
        </el-amap-marker>

        <!-- 用户当前位置脉冲水波纹标记 -->
        <el-amap-marker
          v-if="userLocation"
          :position="userLocation"
          title="您当前的位置"
        >
          <div class="user-position-dot">
            <div class="dot-core"></div>
            <div class="dot-halo"></div>
          </div>
        </el-amap-marker>
      </el-amap>
    </div>

    <!-- 侧边定位到当前位置按钮 (Radar Locate Button) -->
    <button 
      class="locate-side-btn" 
      :class="{ 'is-locating': isLocatingOnly }" 
      @click="handleLocateOnlyClick"
      title="定位到当前位置"
    >
      <div class="locate-icon">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="7" />
          <circle cx="12" cy="12" r="2" fill="currentColor" />
          <line x1="12" y1="1" x2="12" y2="4" />
          <line x1="12" y1="20" x2="12" y2="23" />
          <line x1="1" y1="12" x2="4" y2="12" />
          <line x1="20" y1="12" x2="23" y2="12" />
        </svg>
      </div>
    </button>

    <!-- 底部主干预按钮 (Paw Print FAB) -->
    <div class="fab-container">
      <button 
        class="paw-fab" 
        :class="{ 'is-loading': isLocating }" 
        @click="handleAddClick"
      >
        <div class="paw-icon">
          <svg viewBox="0 0 512 512" width="36" height="36" fill="currentColor">
            <path d="M226.5 92.9c14.3 73.1-.1 134-32.2 136.2-32.1 2.2-69.8-55.2-84.1-128.3-14.3-73.1.1-134 32.2-136.2 32.1-2.2 69.8 55.2 84.1 128.3zM285.5 92.9c-14.3 73.1.1 134 32.2 136.2 32.1 2.2 69.8-55.2 84.1-128.3-14.3-73.1-.1-134-32.2-136.2-32.1-2.2-69.8 55.2-84.1 128.3zM256 272c-53 0-96 43-96 96 0 53 43 96 96 96s96-43 96-96c0-53-43-96-96-96zm-119.5 59.5C125 352 104 382.5 89.5 403c-14.5 20.5-23 44-23 68 0 24 8.5 47.5 23 68 14.5 20.5 35.5 51 47 71.5 11.5 20.5 27 33.5 39.5 33.5 12.5 0 28-13 39.5-33.5C227 590 248 559.5 248 539s-8.5-47.5-23-68c-14.5-20.5-35.5-51-47-71.5-11.5-20.5-27-33.5-39.5-33.5h-2z" transform="translate(0, -50) scale(1.1)"/>
          </svg>
        </div>
        <span class="fab-text" v-if="isLocating">定位中...</span>
      </button>
    </div>

    <!-- 玻璃拟态添加记录模态框 -->
    <Transition name="fade-scale">
      <div class="glass-modal-overlay" v-if="showModal" @click.self="closeModal">
        <div class="glass-card">
          <button class="close-btn" @click="closeModal">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
          
          <div class="modal-header">
            <h3>📍 在这发现了...</h3>
            <p>我们在 ({{ currentLocation.lng.toFixed(3) }}, {{ currentLocation.lat.toFixed(3) }}) 发现了谁？</p>
          </div>

          <div class="modal-body">
            <!-- 动物类型选择器 (Segmented Control) -->
            <div class="input-label-tag">发现的动物类型</div>
            <div class="type-selector">
              <label class="type-btn" :class="{ active: formType === 'Cat' }">
                <input type="radio" v-model="formType" value="Cat">
                <div class="type-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-svg">
                    <path d="M12 5c.67 0 1.35.09 2 .26L18.5 2 17 6.5c2.3 1.9 3.5 4.7 3 7.5-.7 4-4 7-8 7s-7.3-3-8-7c-.5-2.8.7-5.6 3-7.5L5.5 2 10 5.26c.65-.17 1.33-.26 2-.26z" />
                    <circle cx="9" cy="12" r="1.2" fill="currentColor" />
                    <circle cx="15" cy="12" r="1.2" fill="currentColor" />
                    <path d="M12 15c-.5.5-1.5.5-2 0m2 0c.5.5 1.5.5 2 0" />
                  </svg>
                  <span>猫猫</span>
                </div>
              </label>
              <label class="type-btn" :class="{ active: formType === 'Dog' }">
                <input type="radio" v-model="formType" value="Dog">
                <div class="type-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-svg">
                    <path d="M19 12a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z" />
                    <path d="M7 6.5C5.5 7.5 4 10 4 12.5" />
                    <path d="M17 6.5c1.5 1 3 3.5 3 6" />
                    <path d="M12 13v1.5" />
                    <circle cx="9" cy="10" r="1.2" fill="currentColor" />
                    <circle cx="15" cy="10" r="1.2" fill="currentColor" />
                    <path d="M11 15c.5.3 1.5.3 2 0" />
                  </svg>
                  <span>狗子</span>
                </div>
              </label>
              <label class="type-btn" :class="{ active: formType === 'Other' }">
                <input type="radio" v-model="formType" value="Other">
                <div class="type-btn-content">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" class="btn-svg">
                    <circle cx="7.5" cy="9.5" r="1.8" />
                    <circle cx="12" cy="6.5" r="2.2" />
                    <circle cx="16.5" cy="9.5" r="1.8" />
                    <path d="M12 11c-1.8 0-3.5 1.5-3.5 3.3 0 1.8 1.5 3.2 3.5 3.2s3.5-1.4 3.5-3.2c0-1.8-1.7-3.3-3.5-3.3z"/>
                  </svg>
                  <span>其他</span>
                </div>
              </label>
            </div>

            <!-- 输入表单 -->
            <div class="input-group">
              <input type="text" v-model="formNickname" placeholder="TA的昵称 (选填，如：大橘)" class="elegant-input" />
            </div>
            
            <div class="input-group">
              <textarea v-model="formFeatures" placeholder="有什么特征？(如：左耳缺角，很亲人)" rows="2" class="elegant-input"></textarea>
            </div>

            <!-- 行为状态选择器 (behavior_tag) -->
            <div class="input-label-tag">当前状态行为 (不选则由 AI 自动推断)</div>
            <div class="behavior-selector-grid">
              <label class="behavior-btn" :class="{ active: formBehaviorTag === 'EATING' }">
                <input type="radio" v-model="formBehaviorTag" value="EATING">
                <div class="behavior-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 12a9 9 0 0 0 18 0" />
                    <path d="M21 12H3" />
                    <path d="M6 8c1.5-1 3 0 4.5-1s2.5 1.5 4.5-.5" />
                  </svg>
                  <span>吃喝</span>
                </div>
              </label>
              <label class="behavior-btn" :class="{ active: formBehaviorTag === 'SLEEPING' }">
                <input type="radio" v-model="formBehaviorTag" value="SLEEPING">
                <div class="behavior-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
                    <path d="M19 3v4M17 5h4" />
                  </svg>
                  <span>睡觉</span>
                </div>
              </label>
              <label class="behavior-btn" :class="{ active: formBehaviorTag === 'PLAYING' }">
                <input type="radio" v-model="formBehaviorTag" value="PLAYING">
                <div class="behavior-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M9 3.5c1.5 2.5 1.5 6 0 8.5m6-8.5c-1.5 2.5-1.5 6 0 8.5M3.5 9c2.5 1.5 6 1.5 8.5 0m-8.5 6c2.5-1.5 6-1.5 8.5 0" />
                  </svg>
                  <span>玩耍</span>
                </div>
              </label>
              <label class="behavior-btn" :class="{ active: formBehaviorTag === 'SUNBATHING' }">
                <input type="radio" v-model="formBehaviorTag" value="SUNBATHING">
                <div class="behavior-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="4" />
                    <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
                  </svg>
                  <span>晒太阳</span>
                </div>
              </label>
              <label class="behavior-btn" :class="{ active: formBehaviorTag === 'WALKING' }">
                <input type="radio" v-model="formBehaviorTag" value="WALKING">
                <div class="behavior-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 22c0-3 3-4 5-6s3-3 2-6M21 2c0 3-3 4-5 6s-3 3-2 6" />
                  </svg>
                  <span>行走</span>
                </div>
              </label>
              <label class="behavior-btn" :class="{ active: formBehaviorTag === 'OTHER' }">
                <input type="radio" v-model="formBehaviorTag" value="OTHER">
                <div class="behavior-btn-content">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10" />
                    <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
                    <line x1="12" y1="17" x2="12.01" y2="17" />
                  </svg>
                  <span>其他</span>
                </div>
              </label>
            </div>

            <!-- 附加选填信息 -->
            <div class="input-label-tag">选填附加信息</div>
            
            <div class="form-row-2">
              <div class="input-group">
                <select v-model="formTimeOffset" class="elegant-input elegant-select">
                  <option :value="0">发现时间：刚才</option>
                  <option :value="10">发现时间：10分钟前</option>
                  <option :value="30">发现时间：半小时前</option>
                  <option :value="60">发现时间：1小时前</option>
                  <option :value="120">发现时间：2小时前</option>
                </select>
              </div>
              <div class="input-group">
                <input type="text" v-model="formQrCodeId" placeholder="挂牌二维码ID (选填)" class="elegant-input" />
              </div>
            </div>

            <div class="input-group">
              <input type="text" v-model="formPhotoUrl" placeholder="照片链接 URL (选填)" class="elegant-input" />
            </div>

            <div class="validation-tip" v-if="!isFormValid">
              * 请至少填写“昵称”或“特征”以标记足迹
            </div>
          </div>

          <div class="modal-footer">
            <button class="submit-btn" :disabled="isSubmitting || !isFormValid" @click="submitMarker">
              {{ isSubmitting ? '记录中...' : '标记足迹' }}
            </button>
            <p class="privacy-tip">
              🐾 数据仅用于校园小动物轨迹分析，支持匿名上传
            </p>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 20px;
  overflow: hidden;
  box-sizing: border-box;
}

/* 侧边定位按钮 */
.locate-side-btn {
  position: absolute;
  right: 16px;
  bottom: 80px;
  z-index: 1000;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #2F4F4F;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.locate-side-btn:hover {
  transform: scale(1.1);
  background: white;
  color: #FF9800;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.locate-side-btn:active {
  transform: scale(0.95);
}

.locate-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.locate-side-btn.is-locating .locate-icon {
  animation: spin 1.5s infinite linear;
}

/* 自定义图标样式 */
.custom-marker {
  background-color: rgba(255, 255, 255, 0.95);
  border: 2px solid #81C784;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transform-origin: bottom center;
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), border-color 0.3s, color 0.3s;
  color: #81C784;
}

.custom-marker.cat {
  border-color: #81C784;
  color: #2E7D32;
}

.custom-marker.dog {
  border-color: #FFB74D;
  color: #E65100;
}

.custom-marker:hover {
  transform: scale(1.2) translateY(-5px);
  z-index: 1000;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

/* Floating Action Button */
.fab-container {
  position: absolute;
  bottom: -24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
}

.paw-fab {
  background: linear-gradient(135deg, #FF9800, #F57C00);
  color: white;
  border: none;
  border-radius: 50px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 10px 25px rgba(245, 124, 0, 0.4), inset 0 -3px 0 rgba(0,0,0,0.1);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  font-family: inherit;
}

.paw-fab:hover {
  transform: translateY(-5px) scale(1.05);
  box-shadow: 0 15px 35px rgba(245, 124, 0, 0.5), inset 0 -3px 0 rgba(0,0,0,0.1);
}

.paw-fab:active {
  transform: translateY(2px) scale(0.95);
  box-shadow: 0 5px 15px rgba(245, 124, 0, 0.3);
}

.paw-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.paw-fab.is-loading .paw-icon {
  animation: pulse 1s infinite alternate, spin 2s infinite linear;
}

.fab-text {
  font-weight: 700;
  font-size: 1rem;
  letter-spacing: 0.5px;
}

/* 玻璃拟态 Modal 样式 */
.glass-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(47, 79, 79, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.glass-card {
  position: relative;
  width: 90%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 20px 50px rgba(0,0,0,0.1);
  box-sizing: border-box;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  transition: all 0.2s;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}
.close-btn:hover {
  color: #333;
  background: rgba(0,0,0,0.05);
}

.modal-header h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  color: #2E7D32;
}

.modal-header p {
  margin: 0 0 1.5rem 0;
  font-size: 0.85rem;
  color: #666;
}

/* Type Selector */
.type-selector {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  background: rgba(0,0,0,0.03);
  padding: 4px;
  border-radius: 12px;
}

.type-btn {
  flex: 1;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.type-btn input {
  display: none;
}

.type-btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 0; /* Expanded touch height to comply with 44px minimum target */
  font-weight: 600;
  font-size: 0.95rem;
  color: #666;
  transition: all 0.3s;
}

.type-btn.active {
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.type-btn.active .type-btn-content {
  color: #FF9800;
}

.btn-svg {
  transition: transform 0.3s;
}

.type-btn:hover .btn-svg {
  transform: scale(1.15);
}

/* Form Inputs */
.input-group {
  margin-bottom: 1rem;
}

.elegant-input {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 12px;
  font-size: 1rem;
  color: #333;
  font-family: inherit;
  transition: all 0.3s;
  box-sizing: border-box;
}

.elegant-input:focus {
  outline: none;
  border-color: #81C784;
  background: white;
  box-shadow: 0 0 0 3px rgba(129, 199, 132, 0.2);
}

.elegant-input::placeholder {
  color: #AAA;
}

/* Submit Button */
.submit-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: #4CAF50;
  color: white;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
}

.submit-btn:hover:not(:disabled) {
  background: #43A047;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
}

.submit-btn:disabled {
  background: #A5D6A7;
  cursor: not-allowed;
  transform: none;
}

/* Animations */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  from { opacity: 0.6; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1.1); }
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
}

.fade-scale-enter-from .glass-card,
.fade-scale-leave-to .glass-card {
  transform: scale(0.9) translateY(20px);
}
.validation-tip {
  font-size: 0.8rem;
  color: #E65100;
  margin-top: -4px;
  margin-bottom: 12px;
  padding-left: 4px;
  font-weight: 500;
  text-align: left;
}

.privacy-tip {
  font-size: 0.72rem;
  color: #888;
  text-align: center;
  margin: 12px 0 0 0;
  font-weight: 500;
  letter-spacing: 0.2px;
}

.input-label-tag {
  font-size: 0.78rem;
  font-weight: 700;
  color: #558B2F;
  margin: 14px 0 8px 4px;
  text-align: left;
}

.behavior-selector-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 16px;
  background: rgba(0,0,0,0.02);
  padding: 4px;
  border-radius: 12px;
}

.behavior-btn {
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.behavior-btn input {
  display: none;
}

.behavior-btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px 0;
  font-weight: 600;
  font-size: 0.85rem;
  color: #666;
  transition: all 0.3s;
  border: 1px solid transparent;
  border-radius: 8px;
}

.behavior-btn.active .behavior-btn-content {
  background: white;
  color: #FF9800;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.behavior-btn-content svg {
  transition: transform 0.2s;
}

.behavior-btn:hover .behavior-btn-content svg {
  transform: scale(1.15);
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 8px;
  margin-bottom: 0.5rem;
}

.elegant-select {
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='none' stroke='%23666' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m3 5 3 3 3-3'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

/* 隐藏高德地图版权信息和 Logo，避免与悬浮按钮重叠，提升视觉高级感 */
:deep(.amap-copyright),
:deep(.amap-logo) {
  display: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
  pointer-events: none !important;
}

/* 用户当前定位水波纹脉冲点样式 */
.user-position-dot {
  position: relative;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dot-core {
  width: 12px;
  height: 12px;
  background-color: #2196F3;
  border: 2.5px solid #FFFFFF;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(33, 150, 243, 0.7);
  z-index: 2;
}
.dot-halo {
  position: absolute;
  width: 28px;
  height: 28px;
  background-color: rgba(33, 150, 243, 0.4);
  border-radius: 50%;
  animation: pulse-ring 1.8s infinite ease-in-out;
  z-index: 1;
}
@keyframes pulse-ring {
  0% {
    transform: scale(0.5);
    opacity: 1;
  }
  100% {
    transform: scale(1.6);
    opacity: 0;
  }
}
</style>
