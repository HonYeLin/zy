<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, toRef } from 'vue';
import axios from 'axios';

const props = withDefaults(defineProps<{
  activeTrackAnimalId?: number | null
}>(), {
  activeTrackAnimalId: null
});

const activeTrackAnimalId = toRef(props, 'activeTrackAnimalId');

const center = ref([116.397428, 39.90923]); 
const zoom = ref(16);
const map = ref<any>(null);
const animalLogs = ref<any[]>([]);
const userLocation = ref<[number, number] | null>(null); // 用户当前定位位置
const tempMarker = ref<{ lng: number, lat: number } | null>(null); // 用户点击地图生成的临时标记

const emit = defineEmits(['select-animal', 'update:activeTrackAnimalId']);

const handleMarkerClick = (animal: any) => {
  if (animal) {
    if (props.activeTrackAnimalId === animal.id) {
      emit('update:activeTrackAnimalId', null); // 再次点击该标识图标，恢复初始默认状态
    } else {
      emit('update:activeTrackAnimalId', animal.id); // 点击选择该图标，只保留同一标识图标且只显示该图标
    }
    emit('select-animal', animal);
  }
};

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
const formTimeOffset = ref(0); // minutes offset: 0, 10, 30, 60, 120
const formPhotoUrl = ref('');
const isSubmitting = ref(false);

// Photo upload and nickname select state
const selectedNicknameOption = ref('');
const existingAnimalNames = ref<string[]>([]);
const uploadedPhotoUrl = ref('');
const localPreviewUrl = ref('');
const isUploadingPhoto = ref(false);
const cameraInput = ref<HTMLInputElement | null>(null);

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
    const res = await axios.get('/api/locations/all');
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
  selectedNicknameOption.value = '';
  formFeatures.value = '';
  formBehaviorTag.value = '';
  formTimeOffset.value = 0;
  formPhotoUrl.value = '';
  uploadedPhotoUrl.value = '';
  localPreviewUrl.value = '';
  fetchAnimalsList();
};

const fetchAnimalsList = async () => {
  try {
    const res = await axios.get('/api/animals');
    const names: string[] = res.data.map((a: any) => a.name);
    existingAnimalNames.value = Array.from(new Set(names.filter(n => n && n.trim().length > 0)));
  } catch (error) {
    console.error('获取已有小动物列表失败:', error);
  }
};

const handleNicknameSelectChange = () => {
  if (selectedNicknameOption.value === '__NEW__') {
    formNickname.value = '';
  } else {
    formNickname.value = selectedNicknameOption.value;
  }
};

const triggerCamera = () => {
  if (cameraInput.value) {
    cameraInput.value.click();
  }
};

const handlePhotoCapture = async (event: any) => {
  const files = event.target.files;
  if (!files || files.length === 0) return;
  
  const file = files[0];
  // 立即生成本地预览 URL，给用户即时的视觉反馈
  localPreviewUrl.value = URL.createObjectURL(file);
  
  const formData = new FormData();
  formData.append('file', file);
  
  isUploadingPhoto.value = true;
  try {
    const res = await axios.post('/api/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    uploadedPhotoUrl.value = res.data.url;
    formPhotoUrl.value = res.data.url;
    
    // 自动用 AI 识别结果填充表单
    if (res.data.aiData) {
      const ai = res.data.aiData;
      if (ai.type) {
        formType.value = ai.type;
      }
      if (ai.nickname) {
        selectedNicknameOption.value = '__NEW__';
        formNickname.value = ai.nickname;
      }
      if (ai.features) {
        formFeatures.value = ai.features;
      }
      if (ai.behaviorTag) {
        formBehaviorTag.value = ai.behaviorTag;
      }
    }
  } catch (error) {
    console.error('图片上传失败:', error);
    alert('照片上传失败，请确保后端服务正常运行。');
    // 上传失败时，清除本地预览以防止显示错误的数据
    localPreviewUrl.value = '';
  } finally {
    isUploadingPhoto.value = false;
  }
};

const removeUploadedPhoto = () => {
  uploadedPhotoUrl.value = '';
  formPhotoUrl.value = '';
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value);
    localPreviewUrl.value = '';
  }
  if (cameraInput.value) {
    cameraInput.value.value = '';
  }
};

const closeModal = () => {
  showModal.value = false;
  tempMarker.value = null; // 关闭页面时清除地图上的临时标记
};

// 3. 提交表单 (对接后端 DTO 格式)
const submitMarker = async () => {
  if (!formNickname.value.trim() && !formFeatures.value.trim()) {
    alert("请至少填写一个昵称或特征！");
    return;
  }
  isSubmitting.value = true;
  
  try {
    await axios.post('/api/locations', {
      type: formType.value,
      nickname: formNickname.value.trim(),
      features: formFeatures.value.trim(),
      longitude: currentLocation.value.lng,
      latitude: currentLocation.value.lat,
      behaviorTag: formBehaviorTag.value,
      qrCodeId: '',
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

let refreshIntervalId: any = null;

onMounted(() => {
  // 持续每 5 秒自动拉取一次数据库最新的标记点，保持地图数据实时刷新
  refreshIntervalId = setInterval(() => {
    fetchNearbyLogs();
  }, 5000);
});

onUnmounted(() => {
  if (refreshIntervalId) {
    clearInterval(refreshIntervalId);
  }
});

const animalColors = [
  '#FF8A65', // coral
  '#4DB6AC', // teal
  '#7986CB', // indigo
  '#AED581', // light green
  '#FFD54F', // amber
  '#BA68C8', // purple
  '#4FC3F7', // light blue
  '#D4E157', // lime
  '#F06292', // pink
  '#A1887F', // light brown
];

const getAnimalColor = (animal: any) => {
  if (!animal || !animal.id) return '#81C784';
  
  // Use string hash to get a consistent color index
  const idStr = String(animal.id);
  let hash = 0;
  for (let i = 0; i < idStr.length; i++) {
    hash = idStr.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % animalColors.length;
  return animalColors[index];
};

// 点击地图空白处，先在地图上生成绿色临时标记，再次点击该标记才会打开表单
const handleMapClick = (e: any) => {
  const { lng, lat } = e.lnglat;
  tempMarker.value = { lng, lat };
  currentLocation.value = { lng, lat };
};

// --- 轨迹与连线相关状态和计算属性 ---

// 过滤后的足迹：若开启了轨迹追踪，则只显示该动物的足迹；否则显示全部
const filteredLogs = computed(() => {
  if (activeTrackAnimalId.value === null) {
    return animalLogs.value;
  }
  return animalLogs.value.filter(log => log.animal?.id === activeTrackAnimalId.value);
});

// 按时间正序排列的选中动物足迹列表
const trackedLogsSorted = computed(() => {
  if (activeTrackAnimalId.value === null) return [];
  return animalLogs.value
    .filter(log => log.animal?.id === activeTrackAnimalId.value)
    .sort((a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime());
});

// 获取某条记录在轨迹中的序号（从1开始）
const getLogIndex = (logId: number) => {
  const index = trackedLogsSorted.value.findIndex(log => log.id === logId);
  return index !== -1 ? index + 1 : '';
};

// 轨迹折线的坐标序列
const trajectoryPath = computed(() => {
  return trackedLogsSorted.value.map(log => [log.longitude, log.latitude]);
});

// 轨迹折线颜色（与选中动物匹配）
const trajectoryLineColor = computed(() => {
  if (activeTrackAnimalId.value === null) return '#81C784';
  const firstLog = trackedLogsSorted.value[0];
  return getAnimalColor(firstLog?.animal);
});
</script>

<template>
  <div class="map-wrapper">
    <div class="map-container">
      <el-amap :center="center" :zoom="zoom" @init="initMap" @click="handleMapClick" :show-label="false">
        <!-- 轨迹连线 (带方向箭头) -->
        <el-amap-polyline
          v-if="activeTrackAnimalId !== null && trajectoryPath.length > 1"
          :path="trajectoryPath"
          :showDir="true"
          :strokeWeight="6"
          :strokeColor="trajectoryLineColor"
          :lineJoin="'round'"
          :lineCap="'round'"
        />

        <el-amap-marker
          v-for="log in filteredLogs"
          :key="log.id"
          :position="[log.longitude, log.latitude]"
          :offset="[-20, -20]"
          :title="`${log.animal?.name || '未知小动物'} (${log.behaviorTag || '活动'}): ${log.description || '无特征描述'}`"
          @click="handleMarkerClick(log.animal)"
        >
          <div class="custom-marker" :class="[log.animal?.breed?.toLowerCase(), { 'is-tracked': activeTrackAnimalId !== null }]" :style="{ borderColor: getAnimalColor(log.animal), color: getAnimalColor(log.animal) }">
            <!-- 时间排序序号徽章 -->
            <div v-if="activeTrackAnimalId !== null" class="marker-index-badge" :style="{ backgroundColor: getAnimalColor(log.animal) }">
              {{ getLogIndex(log.id) }}
            </div>
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
          :offset="[-10, -10]"
          title="您当前的位置"
        >
          <div class="user-position-dot">
            <div class="dot-core"></div>
            <div class="dot-halo"></div>
          </div>
        </el-amap-marker>
        <!-- 临时选点标记，再次点击此标记弹出记录面板 -->
        <el-amap-marker
          v-if="tempMarker"
          :position="[tempMarker.lng, tempMarker.lat]"
          :offset="[-16, -32]"
          title="再次点击此标记记录发现"
          @click="openModal"
        >
          <div class="temp-marker-container">
            <div class="temp-marker-pulse">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#2E7D32" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                <circle cx="12" cy="10" r="3" fill="#2E7D32"></circle>
              </svg>
            </div>
            <div class="temp-marker-tooltip">在这发现了... (点我记录)</div>
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
          <svg viewBox="0 0 24 24" width="36" height="36" fill="currentColor">
            <path d="M12 13c-2.2 0-4 1.8-4 4 0 2.5 2 4.5 4 4.5s4-2 4-4.5c0-2.2-1.8-4-4-4z"/>
            <circle cx="8" cy="11" r="2"/>
            <circle cx="10.5" cy="8.5" r="2.2"/>
            <circle cx="13.5" cy="8.5" r="2.2"/>
            <circle cx="16" cy="11" r="2"/>
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

            <!-- 选择/输入昵称 -->
            <div class="input-label-tag">选择或新建观测昵称</div>
            <div class="input-group">
              <select v-model="selectedNicknameOption" @change="handleNicknameSelectChange" class="elegant-input elegant-select">
                <option value="">暂未命名 (系统根据种类自动命名)</option>
                <option v-for="name in existingAnimalNames" :key="name" :value="name">{{ name }}</option>
                <option value="__NEW__">➕ 记录新的小生命...</option>
              </select>
            </div>
            
            <div v-if="selectedNicknameOption === '__NEW__'" class="input-group fade-in-animation">
              <input type="text" v-model="formNickname" placeholder="请输入新小动物的昵称 (如：花花)" class="elegant-input" />
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
            
            <div class="input-group">
              <select v-model="formTimeOffset" class="elegant-input elegant-select">
                <option :value="0">发现时间：刚才</option>
                <option :value="10">发现时间：10分钟前</option>
                <option :value="30">发现时间：半小时前</option>
                <option :value="60">发现时间：1小时前</option>
                <option :value="120">发现时间：2小时前</option>
              </select>
            </div>

            <div class="input-label-tag">现场实拍照 (选填)</div>
            <div class="photo-capture-container">
              <input 
                type="file" 
                ref="cameraInput" 
                accept="image/*" 
                capture="environment" 
                style="display: none" 
                @change="handlePhotoCapture" 
              />
              
              <!-- 上传成功后的预览区 -->
              <div v-if="uploadedPhotoUrl" class="photo-preview-wrapper">
                <img :src="uploadedPhotoUrl" class="photo-preview-img" alt="现场实拍" />
                <button type="button" class="photo-delete-btn" @click="removeUploadedPhoto" title="删除照片">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              </div>
              
              <!-- 触发按钮 -->
              <button 
                v-else 
                type="button" 
                class="photo-capture-btn" 
                :class="{ 'is-uploading': isUploadingPhoto }" 
                @click="triggerCamera"
              >
                <div class="capture-icon">
                  <svg v-if="isUploadingPhoto" class="upload-spinner" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
                    <circle cx="12" cy="12" r="10" stroke="rgba(0,0,0,0.1)"></circle>
                    <path d="M12 2a10 10 0 0 1 10 10" stroke="#FF9800"></path>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                    <circle cx="12" cy="13" r="4"></circle>
                  </svg>
                </div>
                <span class="capture-text">{{ isUploadingPhoto ? '照片上传中...' : '点击拍照 / 上传图片' }}</span>
              </button>
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
  position: relative;
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

/* 时间排序轨迹序号徽章 */
.marker-index-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  color: white;
  font-size: 11px;
  font-weight: bold;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.25);
  border: 1.5px solid white;
  z-index: 10;
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
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
  width: 92%;
  max-width: 420px;
  max-height: 90vh;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 1.8rem;
  box-shadow: 0 20px 50px rgba(0,0,0,0.15);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding-right: 6px;
  margin-bottom: 0.8rem;
}

.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track {
  background: transparent;
}

.modal-body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

.modal-footer {
  padding-top: 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
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

/* 临时定位绿色标记点脉冲与气泡样式 */
.temp-marker-container {
  position: relative;
  width: 32px;
  height: 32px;
  cursor: pointer;
}

.temp-marker-pulse {
  width: 32px;
  height: 32px;
  animation: bounce-marker 1s infinite alternate cubic-bezier(0.25, 0.46, 0.45, 0.94);
  filter: drop-shadow(0 4px 6px rgba(46, 125, 50, 0.3));
}

.temp-marker-tooltip {
  position: absolute;
  top: 38px;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  background: rgba(46, 125, 50, 0.95);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
  border: 1.5px solid rgba(255, 255, 255, 0.25);
  animation: temp-fade-in 0.3s ease-out;
}

.temp-marker-tooltip::after {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 5px;
  border-style: solid;
  border-color: transparent transparent rgba(46, 125, 50, 0.95) transparent;
}

@keyframes bounce-marker {
  0% {
    transform: translateY(0) scale(1);
  }
  100% {
    transform: translateY(-8px) scale(1.05);
  }
}

@keyframes temp-fade-in {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}


/* Photo Capture & Upload UI */
.photo-capture-container {
  margin-bottom: 1.2rem;
  width: 100%;
}

.photo-capture-btn {
  width: 100%;
  padding: 16px;
  background: rgba(0, 0, 0, 0.03);
  border: 2px dashed rgba(129, 199, 132, 0.5);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #558B2F;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.photo-capture-btn:hover {
  background: rgba(129, 199, 132, 0.08);
  border-color: #81C784;
  color: #2E7D32;
  transform: translateY(-2px);
}

.photo-capture-btn:active {
  transform: translateY(1px);
}

.photo-capture-btn.is-uploading {
  border-color: #FFB74D;
  background: rgba(255, 183, 77, 0.05);
  color: #E65100;
  cursor: wait;
}

.capture-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-spinner {
  animation: spin 1s infinite linear;
}

.photo-preview-wrapper {
  position: relative;
  width: 100%;
  max-height: 200px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.photo-preview-img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.photo-delete-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.photo-delete-btn:hover {
  background: rgba(244, 67, 54, 0.9);
  transform: scale(1.1);
}

.fade-in-animation {
  animation: fieldFadeIn 0.35s ease-out;
}

@keyframes fieldFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .glass-card {
    padding: 1.2rem;
    border-radius: 20px;
    width: 95%;
  }
  .modal-header h3 {
    font-size: 1.3rem;
  }
  .type-btn-content {
    padding: 10px 0;
    font-size: 0.85rem;
  }
  .behavior-selector-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
