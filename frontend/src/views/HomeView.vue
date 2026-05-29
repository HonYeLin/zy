<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';
import MapContainer from '../components/MapContainer.vue';
import AnimalReview from '../components/AnimalReview.vue';

// Define Interface
interface Animal {
  id: number;
  name: string;
  breed: string;
  qrCodeId?: string;
  avatarUrl?: string;
  createdAt?: string;
  aiSummary?: string;
}

// --- User Account System ---
const currentUser = ref<any>(null);
const showAuthModal = ref(false);
const authTab = ref('login'); // 'login' or 'register'
const authForm = ref({
  username: '',
  password: '',
  nickname: ''
});
const authError = ref('');
const isSubmittingAuth = ref(false);

const initializeUser = async () => {
  // 1. Check logged-in user
  const loggedInUserStr = localStorage.getItem('logged_in_user');
  if (loggedInUserStr) {
    try {
      currentUser.value = JSON.parse(loggedInUserStr);
      return;
    } catch (e) {
      localStorage.removeItem('logged_in_user');
    }
  }

  // 2. Fall back to guest device id
  let deviceId = localStorage.getItem('guest_device_id');
  if (!deviceId) {
    deviceId = 'device_' + Math.random().toString(36).substring(2, 15) + '_' + Date.now();
    localStorage.setItem('guest_device_id', deviceId);
  }

  // 3. Authenticate guest
  try {
    const res = await fetch(`/api/users/guest`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ guestDeviceId: deviceId })
    });
    if (res.ok) {
      currentUser.value = await res.json();
    }
  } catch (error) {
    console.error('Failed to initialize guest user on backend', error);
    currentUser.value = {
      id: null,
      nickname: '游客',
      role: 'GUEST'
    };
  }
};

const openAuthModal = (tab = 'login') => {
  authTab.value = tab;
  authForm.value = { username: '', password: '', nickname: '' };
  authError.value = '';
  showAuthModal.value = true;
};

const closeAuthModal = () => {
  showAuthModal.value = false;
};

const handleAuthSubmit = async () => {
  authError.value = '';
  if (!authForm.value.username.trim() || !authForm.value.password.trim()) {
    authError.value = '用户名和密码不能为空';
    return;
  }
  if (authTab.value === 'register' && !authForm.value.nickname.trim()) {
    authForm.value.nickname = authForm.value.username.trim();
  }

  isSubmittingAuth.value = true;
  try {
    // 拦截管理员登录
    if (authTab.value === 'login' && authForm.value.username.trim() === 'admin') {
      const res = await fetch('/api/admin/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: authForm.value.username.trim(),
          password: authForm.value.password.trim()
        })
      });
      const data = await res.json();
      if (res.ok) {
        sessionStorage.setItem('admin_token', data.token);
        closeAuthModal();
        alert('管理员登录成功，正在打开新标签页进入管理系统...');
        window.open('/admin/dashboard', '_blank');
        return;
      } else {
        authError.value = data.error || '管理员登录失败';
        return;
      }
    }

    const endpoint = authTab.value === 'register' ? 'register' : 'login';
    const body = authTab.value === 'register' 
      ? { username: authForm.value.username.trim(), password: authForm.value.password.trim(), nickname: authForm.value.nickname.trim() }
      : { username: authForm.value.username.trim(), password: authForm.value.password.trim() };

    const res = await fetch(`/api/users/${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await res.json();
    if (res.ok) {
      localStorage.setItem('logged_in_user', JSON.stringify(data));
      currentUser.value = data;
      closeAuthModal();
      alert(authTab.value === 'register' ? '注册并登录成功！' : '登录成功！');
    } else {
      authError.value = data.error || '操作失败';
    }
  } catch (error) {
    console.error('Auth error', error);
    authError.value = '无法连接到服务器';
  } finally {
    isSubmittingAuth.value = false;
  }
};

const handleLogout = async () => {
  if (confirm('确认要退出登录吗？')) {
    localStorage.removeItem('logged_in_user');
    currentUser.value = null;
    await initializeUser(); // reload guest
  }
};


// Global Stats & Animals list
const animals = ref<Animal[]>([]);
const selectedAnimalId = ref<number | null>(null);
const lifeRecords = ref<any[]>([]);
const isNarrativeLoading = ref(false);
const totalAnimalsCount = ref(0);
const totalFootprintsCount = ref(0);

// Selected Animal computed property
const selectedAnimal = computed(() => {
  return animals.value.find(a => a.id === selectedAnimalId.value) || null;
});

// AI Modal UI State
const showAiModal = ref(false);
const isAiLoading = ref(false);
const aiReasoningResult = ref<string>('');
const showCorrectionDropdown = ref(false);
const selectedCorrectBehavior = ref<string>('');

// Toast Notification
const toastMessage = ref('');
const showToast = ref(false);

const triggerToast = (msg: string) => {
  toastMessage.value = msg;
  showToast.value = true;
  setTimeout(() => {
    showToast.value = false;
  }, 3000);
};

// Behaviors mapping
const behaviors = [
  { value: 'EATING', label: '进食/饮水' },
  { value: 'SLEEPING', label: '睡觉/休息' },
  { value: 'PLAYING', label: '玩耍/嬉戏' },
  { value: 'SUNBATHING', label: '晒太阳' },
  { value: 'WALKING', label: '行走/奔跑' },
  { value: 'OTHER', label: '其他' }
];

// Helper to get breed Chinese name
const getBreedLabel = (breed: string) => {
  if (breed === 'Cat') return '猫咪';
  if (breed === 'Dog') return '狗子';
  return '小生命';
};

// Fetch all animals
const fetchAnimals = async () => {
  try {
    const res = await axios.get('/api/animals');
    animals.value = res.data;
    totalAnimalsCount.value = res.data.length;
    
    // 默认不自动选中任何动物，保持生活日记默认隐藏
    // if (res.data.length > 0 && !selectedAnimalId.value) {
    //   selectedAnimalId.value = res.data[0].id;
    // }
  } catch (error) {
    console.error('获取动物列表失败:', error);
  }
};

// Fetch total location logs count for stats
const fetchLogsCount = async () => {
  try {
    const res = await axios.get('/api/locations/all');
    totalFootprintsCount.value = res.data.length;
  } catch (error) {
    console.error('获取足迹统计失败:', error);
  }
};

// Fetch animal diary
const fetchNarrative = async (animalId: number) => {
  isNarrativeLoading.value = true;
  lifeRecords.value = [];
  try {
    const res = await axios.get(`/api/analysis/narrative/${animalId}`);
    lifeRecords.value = res.data.narratives || [];
  } catch (error) {
    console.error('获取日记失败:', error);
  } finally {
    isNarrativeLoading.value = false;
  }
};

// Handle event when clicking an animal marker on the map
const handleSelectAnimal = (animal: any) => {
  if (animal && animal.id) {
    selectedAnimalId.value = animal.id;
    triggerToast(`已选择小动物: ${animal.name}`);
  }
};

// Open AI behavior reasoning modal
const openAiModal = async () => {
  if (animals.value.length === 0) {
    alert('当前校园还没有录入任何小动物喔！请先在地图上记录一个吧🐾');
    return;
  }
  
  if (!selectedAnimalId.value) {
    selectedAnimalId.value = animals.value[0].id;
  }
  
  showAiModal.value = true;
  await fetchBehaviorReasoning(selectedAnimalId.value!);
};

// Smooth scroll to animal directory at the bottom
const scrollToDirectory = () => {
  const el = document.querySelector('.directory-section-bottom');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' });
  }
};

// Fetch behavior prediction for the selected animal
const fetchBehaviorReasoning = async (animalId: number) => {
  isAiLoading.value = true;
  aiReasoningResult.value = '';
  showCorrectionDropdown.value = false;
  selectedCorrectBehavior.value = '';
  try {
    const now = new Date();
    const formatter = new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
      hour12: false
    });
    const currentTime = formatter.format(now).replace(/\//g, '-');
    const res = await axios.get(`/api/analysis/behavior/${animalId}?currentTime=${encodeURIComponent(currentTime)}`);
    aiReasoningResult.value = res.data.result;
  } catch (error) {
    console.error('AI行为推理失败:', error);
    aiReasoningResult.value = 'AI行为推理失败，可能由于没有历史轨迹数据或AI接口调用超额，请添加更多足迹重试！🐾';
  } finally {
    isAiLoading.value = false;
  }
};

// Handle dropdown select change inside the modal
const handleModalAnimalChange = async () => {
  if (selectedAnimalId.value) {
    await fetchBehaviorReasoning(selectedAnimalId.value);
  }
};

// Extract short label for predicted behavior to pass to feedback
const getPredictedBehaviorShort = (reasoning: string) => {
  if (!reasoning) return '未知行为';
  if (reasoning.includes('睡觉') || reasoning.includes('休息') || reasoning.includes('睡眠')) return '睡觉/休息';
  if (reasoning.includes('吃') || reasoning.includes('喝') || reasoning.includes('进食')) return '进食/饮水';
  if (reasoning.includes('玩') || reasoning.includes('嬉戏')) return '玩耍/嬉戏';
  if (reasoning.includes('太阳') || reasoning.includes('晒')) return '晒太阳';
  if (reasoning.includes('走') || reasoning.includes('跑') || reasoning.includes('行走')) return '行走/奔跑';
  return reasoning.substring(0, 20).trim();
};

// Submit CONFIRMED feedback
const submitConfirmedFeedback = async () => {
  if (!selectedAnimalId.value) return;
  const predicted = getPredictedBehaviorShort(aiReasoningResult.value);
  try {
    await axios.post('/api/analysis/feedback', {
      animalId: selectedAnimalId.value,
      predictedBehavior: predicted,
      actualBehavior: predicted,
      feedbackType: 'CONFIRMED'
    });
    triggerToast('🐾 谢谢反馈！AI 已经记下了它的这个习惯，下次会更准！');
    showAiModal.value = false;
  } catch (error) {
    console.error('提交反馈失败:', error);
    alert('提交反馈失败。');
  }
};

// Open correction panel
const enableCorrection = () => {
  showCorrectionDropdown.value = true;
};

// Submit CORRECTED feedback
const submitCorrectedFeedback = async () => {
  if (!selectedAnimalId.value) return;
  if (!selectedCorrectBehavior.value) {
    alert('请选择它当时实际上在做什么。');
    return;
  }
  
  const predicted = getPredictedBehaviorShort(aiReasoningResult.value);
  const actualLabel = behaviors.find(b => b.value === selectedCorrectBehavior.value)?.label || '其他';
  
  try {
    await axios.post('/api/analysis/feedback', {
      animalId: selectedAnimalId.value,
      predictedBehavior: predicted,
      actualBehavior: actualLabel,
      feedbackType: 'CORRECTED'
    });
    triggerToast('🐾 纠偏成功！错误数据已存入大脑，下次 AI 会修正推算权重！');
    showAiModal.value = false;
  } catch (error) {
    console.error('提交纠偏反馈失败:', error);
    alert('提交纠偏反馈失败。');
  }
};

// Close modal
const closeAiModal = () => {
  showAiModal.value = false;
};

// Watch selectedAnimalId to fetch narrative automatically
watch(selectedAnimalId, (newId) => {
  if (newId) {
    fetchNarrative(newId);
  } else {
    lifeRecords.value = [];
  }
});

// Trajectory Modal State
const showTrajectoryModal = ref(false);
const trajectoryAnimal = ref<Animal | null>(null);
const trajectoryLogs = ref<any[]>([]);
const isTrajectoryLoading = ref(false);
const activeTrackAnimalId = ref<number | null>(null);

const handleDirectoryCardClick = (animal: Animal) => {
  if (activeTrackAnimalId.value !== animal.id) {
    // 第一次点击：进入和选中地图中图标一样的选中状态，并高亮显示选中该图鉴框
    activeTrackAnimalId.value = animal.id;
    selectedAnimalId.value = animal.id; // 让生活日记等也同步切换
    triggerToast(`已定位并追踪: ${animal.name}`);
  } else {
    // 再次点击：才打开生存足迹与轨迹页面
    openTrajectoryModal(animal);
  }
};

const closeDiary = () => {
  selectedAnimalId.value = null;
  activeTrackAnimalId.value = null;
};

const openTrajectoryModal = async (animal: Animal) => {
  trajectoryAnimal.value = animal;
  showTrajectoryModal.value = true;
  isTrajectoryLoading.value = true;
  trajectoryLogs.value = [];
  try {
    const res = await axios.get(`/api/locations/animal/${animal.id}`);
    trajectoryLogs.value = res.data;
    
    // 如果该小动物还没有 AI 简介（或者之前生成失败），我们在后台异步请求生成一个并更新
    if (!trajectoryAnimal.value.aiSummary) {
      axios.post(`/api/analysis/summary/${animal.id}`)
        .then(summaryRes => {
          if (summaryRes.data && summaryRes.data.summary) {
            if (trajectoryAnimal.value && trajectoryAnimal.value.id === animal.id) {
              trajectoryAnimal.value.aiSummary = summaryRes.data.summary;
            }
            // 同时更新 animals 列表中的对应数据
            const idx = animals.value.findIndex(a => a.id === animal.id);
            if (idx !== -1) {
              animals.value[idx].aiSummary = summaryRes.data.summary;
            }
          }
        })
        .catch(err => {
          console.error('自动生成/更新 AI 简介失败:', err);
        });
    }
  } catch (error) {
    console.error('获取小动物轨迹失败:', error);
  } finally {
    isTrajectoryLoading.value = false;
  }
};

const closeTrajectoryModal = () => {
  showTrajectoryModal.value = false;
  trajectoryAnimal.value = null;
  trajectoryLogs.value = [];
};

const formatLogTime = (timeStr: string) => {
  if (!timeStr) return '';
  try {
    const d = new Date(timeStr);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  } catch (e) {
    return timeStr;
  }
};

const handlePhotoError = (e: any) => {
  e.target.style.display = 'none';
};

const handleImageLoadError = (event: any, animal: Animal) => {
  const target = event.target;
  const genericUrl = `/images/${animal.breed.toLowerCase()}_avatar.png`;
  if (target.src !== genericUrl) {
    target.src = genericUrl;
    target.alt = `文件名：static/images/${animal.breed.toLowerCase()}_avatar.png`;
  } else {
    target.src = '';
    target.style.display = 'none';
  }
};

let statsIntervalId: any = null;

onMounted(() => {
  initializeUser(); // Initialize user account system
  fetchAnimals();
  fetchLogsCount();
  
  // Poll stats and list of animals every 10 seconds to keep fresh
  statsIntervalId = setInterval(() => {
    fetchAnimals();
    fetchLogsCount();
  }, 10000);
});

onUnmounted(() => {
  if (statsIntervalId) {
    clearInterval(statsIntervalId);
  }
});
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
        <div class="title-wrapper">
          <h1>爪印</h1>
        </div>
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
            <MapContainer 
              :active-track-animal-id="activeTrackAnimalId" 
              @update:active-track-animal-id="activeTrackAnimalId = $event" 
              @select-animal="handleSelectAnimal" 
            />
          </div>
          <!-- 算算TA在干什么 按钮 -->
          <div class="map-actions-bottom">
            <button class="ai-calc-btn" @click="openAiModal">
              <span class="icon">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M15 4V2M15 16v-2M8 9h2M20 9h-2M17.8 5.1l-1.4 1.4M17.8 12.9l-1.4-1.4M12.2 5.1l1.4 1.4M12.2 12.9l1.4-1.4"/>
                  <path d="M4 20l7-7 3 3-7 7z"/>
                </svg>
              </span>
              <span class="text">算算TA在干什么</span>
            </button>
          </div>

        </section>

        <!-- 右侧信息与控制侧面板 -->
        <div class="side-panel">
          <!-- 1. 个人状态/登录卡片置顶 -->
          <div class="sidebar-card sidebar-auth-card">
            <div class="user-status-wrapper">
              <span class="user-status-avatar">👤</span>
              <div class="user-status-info">
                <div class="user-status-role" :class="currentUser?.role?.toLowerCase() || 'guest'">
                  {{ currentUser?.role === 'GUEST' ? '游客模式' : '已登录会员' }}
                </div>
                <div class="user-status-nickname">{{ currentUser?.nickname || '游客' }}</div>
              </div>
            </div>
            <div class="sidebar-auth-actions">
              <button v-if="currentUser?.role === 'GUEST'" class="sidebar-login-btn" @click="openAuthModal('login')">
                账户登录 / 注册
              </button>
              <button v-else class="sidebar-logout-btn" @click="handleLogout">
                退出登录
              </button>
            </div>
          </div>

          <!-- 2. 校园守护统计 -->
          <section class="sidebar-section">
            <h2 class="section-title">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="inline-icon">
                <line x1="18" y1="20" x2="18" y2="10"></line>
                <line x1="12" y1="20" x2="12" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="14"></line>
              </svg>
              校园守护统计
            </h2>
            <div class="sidebar-card stat-card">
              <div class="stat-item">
                <span class="stat-val">{{ totalAnimalsCount }}</span>
                <span class="stat-lbl">建档小生命</span>
              </div>
              <div class="stat-item">
                <span class="stat-val">{{ totalFootprintsCount }}</span>
                <span class="stat-lbl">累计足迹数</span>
              </div>
              <div class="stat-item">
                <span class="stat-val">100%</span>
                <span class="stat-lbl">AI 识别率</span>
              </div>
            </div>
          </section>

          <!-- 3. 快捷功能 -->
          <section class="sidebar-section">
            <h2 class="section-title">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="inline-icon">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
              快捷功能
            </h2>
            <div class="button-grid">
              <button class="action-btn directory-btn" @click="scrollToDirectory">
                <span class="icon">
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#FF9800" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                  </svg>
                </span>
                <span class="text">动物图鉴</span>
              </button>
            </div>
          </section>

          <!-- 4. 地图使用引导 -->
          <section class="sidebar-section">
            <div class="sidebar-card tip-card">
              <h3>🐾 地图使用引导</h3>
              <p>直接点击地图上的任意空白位置，可在该处悬浮生成一个绿色的临时标记，再次点击绿色标记便能快捷录入新的发现记录。点击右下悬浮爪印，可实现自动定位记录！</p>
            </div>
          </section>
        </div>
      </div>

      <!-- 评论留言打分界面 -->
      <Transition name="fade-slide">
        <AnimalReview 
          v-if="selectedAnimal" 
          :animal="selectedAnimal" 
          :current-user="currentUser" 
          @open-auth="openAuthModal" 
          @logout="handleLogout" 
          @close="closeDiary" 
        />
      </Transition>

      <!-- 生活日记 (Life Diary) - 放置在分栏下方，真正全屏独占一横栏 -->
      <Transition name="fade-slide">
        <div v-if="selectedAnimal" class="diary-container full-width-diary">
          <div class="diary-paper" :class="selectedAnimal.breed === 'Cat' ? 'cat-paper' : 'dog-paper'">
            <div class="diary-pin"></div>
            
            <div class="diary-card-title">
              <div class="diary-title-left">
                <span class="paw-icon">🐾</span>
                {{ selectedAnimal.name }} 的生活日记
              </div>
              <button class="close-diary-btn" @click="closeDiary" title="收起日记并清除选择">×</button>
            </div>
            
            <div class="diary-content-area">
              <div v-if="isNarrativeLoading" class="diary-loading">
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
                <span class="loading-dot"></span>
                <p>正在翻阅日记本...</p>
              </div>
              <div v-else>
                <div class="diary-records-list">
                  <div v-for="(record, idx) in lifeRecords" :key="idx" class="diary-record-item">
                    <div class="diary-record-header">
                      <span class="record-time">📅 {{ record.time }}</span>
                      <span class="record-bullet">🐾</span>
                    </div>
                    <span class="record-text">{{ record.content }}</span>
                  </div>
                  <div v-if="lifeRecords.length === 0" class="diary-text-empty">
                    这只小家伙还没有生活记录喔。在地图上添加或更新它的足迹，AI 行为分析师就会为它自动撰写有趣的生活日记啦！🐾
                  </div>
                </div>
                <div class="diary-footer">
                  <span>✍️ 校园小生命分析师 Gemini</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 下方动物图鉴分隔栏 -->
      <section class="directory-section-bottom">
        <h2 class="section-title">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="inline-icon">
            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
          </svg>
          动物图鉴
        </h2>
        
        <div v-if="animals.length === 0" class="directory-empty-msg">
          🐾 暂无保存的动物实体，在地图上录入足迹后自动建档图鉴！
        </div>
        <div v-else class="directory-grid">
          <div 
            v-for="animal in animals" 
            :key="animal.id" 
            class="directory-card" 
            :class="{ 'is-active-tracking': activeTrackAnimalId === animal.id }"
            @click="handleDirectoryCardClick(animal)"
          >
            <!-- Active Tooltip -->
            <div class="active-tooltip-notebook" v-if="activeTrackAnimalId === animal.id">
              再次点击查看全部记录
            </div>
            
            <!-- Left-top corner nickname -->
            <div class="card-nickname">{{ animal.name }}</div>
            
            <!-- Right-top corner breed icon -->
            <div class="card-breed-badge" :class="animal.breed.toLowerCase()">
              <svg v-if="animal.breed === 'Cat'" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <circle cx="7.5" cy="9.5" r="2" />
                <circle cx="12" cy="6.5" r="2.5" />
                <circle cx="16.5" cy="9.5" r="2" />
                <path d="M12 11c-1.8 0-3.5 1.5-3.5 3.3 0 1.8 1.5 3.2 3.5 3.2s3.5-1.4 3.5-3.2c0-1.8-1.7-3.3-3.5-3.3z"/>
              </svg>
              <svg v-else-if="animal.breed === 'Dog'" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm-2 15c-.8 0-1.5-.7-1.5-1.5S9.2 14 10 14s1.5.7 1.5 1.5-.7 1.5-1.5 1.5zm4 0c-.8 0-1.5-.7-1.5-1.5s.7-1.5 1.5-1.5 1.5.7 1.5 1.5-.7 1.5-1.5 1.5z"/>
              </svg>
              <span v-else>🐾</span>
            </div>
            
            <div class="card-media">
              <img 
                :src="animal.avatarUrl ? animal.avatarUrl.replace('http://localhost:8080', '') : `/images/${animal.name}_avatar.png`" 
                @error="handleImageLoadError($event, animal)"
                class="card-img" 
                :alt="`头像：${animal.name}`"
                :title="animal.avatarUrl ? `${animal.name}的实物代表性头像 🐾` : `在后端 static/images 文件夹下放入口碑昵称图片 ${animal.name}_avatar.png 即可自动呈现！🐾`"
              />
            </div>
            
            <!-- Bottom breed text -->
            <div class="card-footer">
              <span class="card-breed-name">{{ getBreedLabel(animal.breed) }}</span>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- AI 行为推理 Modal (毛玻璃毛刷视觉) -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="closeAiModal">
      <div class="modal-card">
        <header class="modal-header">
          <div class="modal-title">
            <span>🔮</span> AI 行为推演大脑
          </div>
          <button class="close-modal-btn" @click="closeAiModal">×</button>
        </header>
        
        <div class="modal-body">
          <!-- 切换动物选择器 -->
          <div class="animal-selector-row">
            <label for="modal-animal-select">切换观测对象：</label>
            <select id="modal-animal-select" v-model="selectedAnimalId" @change="handleModalAnimalChange" class="animal-select-dropdown">
              <option v-for="animal in animals" :key="animal.id" :value="animal.id">
                {{ animal.name }} ({{ getBreedLabel(animal.breed) }})
              </option>
            </select>
          </div>

          <!-- 推理结论盒子 -->
          <div class="reasoning-box">
            <div v-if="isAiLoading" class="reasoning-loading">
              <span class="spinner"></span>
              <p>AI 正在分析过去 7 天的轨迹序列与纠偏记忆...</p>
            </div>
            <div v-else>
              <p class="reasoning-text">{{ aiReasoningResult }}</p>
            </div>
          </div>
        </div>

        <footer class="modal-footer" v-if="!isAiLoading && aiReasoningResult && !aiReasoningResult.startsWith('推理失败')">
          <!-- 正常操作：确认或提示修正 -->
          <div class="feedback-actions" v-if="!showCorrectionDropdown">
            <button class="btn-confirm" @click="submitConfirmedFeedback">
              <span>🐾</span> 预测得真准！
            </button>
            <button class="btn-correct" @click="enableCorrection">
              <span>✍️</span> 不对，它刚才在...
            </button>
          </div>

          <!-- 修正反馈面板 -->
          <div class="correction-panel" v-else>
            <div class="correction-label">指出它的真实行为状态：</div>
            <select v-model="selectedCorrectBehavior" class="correction-dropdown">
              <option value="" disabled selected>选择它刚才的实际行为...</option>
              <option v-for="b in behaviors" :key="b.value" :value="b.value">
                {{ b.label }}
              </option>
            </select>
            <div class="correction-buttons">
              <button class="btn-submit-correction" @click="submitCorrectedFeedback">确认修正</button>
              <button class="btn-cancel-correction" @click="showCorrectionDropdown = false">取消</button>
            </div>
          </div>
        </footer>
      </div>
    </div>

    <!-- 行为轨迹 Modal -->
    <div v-if="showTrajectoryModal" class="modal-overlay" @click.self="closeTrajectoryModal">
      <div class="modal-card trajectory-modal">
        <header class="modal-header">
          <div class="modal-title">
            <span>🐾</span> {{ trajectoryAnimal?.name }} 的生存足迹与轨迹
          </div>
          <button class="close-modal-btn" @click="closeTrajectoryModal">×</button>
        </header>
        
        <div class="modal-body trajectory-body">
          <div v-if="isTrajectoryLoading" class="trajectory-loading">
            <span class="spinner"></span>
            <p>正在读取足迹轨迹数据...</p>
          </div>
          <div v-else-if="trajectoryLogs.length === 0" class="trajectory-empty">
            <p>暂无足迹轨迹记录。在地图上为它新增一条记录吧！🐾</p>
          </div>
          <div v-else class="timeline-container">
            <div class="timeline-summary">
              累计观测到 <strong>{{ trajectoryLogs.length }}</strong> 次活动足迹
            </div>
            <div class="animal-ai-summary" v-if="trajectoryAnimal?.aiSummary">
              ✨ <strong>AI 观察日志：</strong>{{ trajectoryAnimal.aiSummary }}
            </div>
            
            <div class="timeline">
              <div v-for="log in trajectoryLogs" :key="log.id" class="timeline-item">
                <div class="timeline-badge" :class="log.behaviorTag.toLowerCase()">
                  <span class="badge-icon">
                    <svg v-if="log.behaviorTag === 'EATING'" viewBox="0 0 24 24" width="12" height="12" fill="currentColor">
                      <path d="M8.1 14.1l-2.5-2.5-1.4 1.4 3.9 3.9 8.1-8.1-1.4-1.4z"/>
                    </svg>
                    <svg v-else-if="log.behaviorTag === 'SLEEPING'" viewBox="0 0 24 24" width="12" height="12" fill="currentColor">
                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
                    </svg>
                    <span v-else>●</span>
                  </span>
                </div>
                
                <div class="timeline-content">
                  <div class="timeline-time">{{ formatLogTime(log.recordedAt) }}</div>
                  <div class="timeline-behavior">
                    <span class="behavior-label-tag" :class="log.behaviorTag.toLowerCase()">{{ log.behaviorLabel }}</span>
                  </div>
                  <div class="timeline-desc">{{ log.sceneDescription || log.description || '未标记画面描述' }}</div>
                  
                  <!-- If a photo is uploaded, show it -->
                  <div v-if="log.photoUrl" class="timeline-photo">
                    <img :src="log.photoUrl.replace('http://localhost:8080', '')" alt="现场照片" @error="handlePhotoError" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 悬浮微型 Toast 提示 -->
    <div v-if="showToast" class="toast-notification">
      {{ toastMessage }}
    </div>

    <!-- 登录注册模态弹窗 -->
    <Transition name="auth-fade">
      <div v-if="showAuthModal" class="auth-modal-overlay" @click.self="closeAuthModal">
        <div class="auth-modal-card">
          <button class="close-auth-btn" @click="closeAuthModal">×</button>
          
          <div class="auth-tabs">
            <div 
              class="auth-tab-item" 
              :class="{ active: authTab === 'login' }" 
              @click="authTab = 'login'"
            >
              账户登录
            </div>
            <div 
              class="auth-tab-item" 
              :class="{ active: authTab === 'register' }" 
              @click="authTab = 'register'"
            >
              新用户注册
            </div>
          </div>

          <div class="auth-form-body">
            <div v-if="authError" class="auth-error-msg">
              ⚠️ {{ authError }}
            </div>

            <div class="auth-form-group">
              <label>用户名</label>
              <input 
                type="text" 
                v-model="authForm.username" 
                placeholder="请输入用户名" 
                class="auth-input"
                @keyup.enter="handleAuthSubmit"
              />
            </div>

            <div class="auth-form-group">
              <label>密码</label>
              <input 
                type="password" 
                v-model="authForm.password" 
                placeholder="请输入密码" 
                class="auth-input"
                @keyup.enter="handleAuthSubmit"
              />
            </div>

            <div class="auth-form-group" v-if="authTab === 'register'">
              <label>昵称</label>
              <input 
                type="text" 
                v-model="authForm.nickname" 
                placeholder="起个好听的名字 (选填)" 
                class="auth-input"
                @keyup.enter="handleAuthSubmit"
              />
            </div>

            <button 
              class="auth-submit-btn" 
              @click="handleAuthSubmit" 
              :disabled="isSubmittingAuth"
            >
              {{ isSubmittingAuth ? '提交中...' : (authTab === 'login' ? '登 录' : '注 册 并 登 录') }}
            </button>
            
            <div class="auth-modal-footer">
              <span class="continue-guest-link" @click="closeAuthModal">继续以游客身份浏览</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style>
/* Pet Natural Theme Styles */
body, html {
  margin: 0;
  padding: 0;
  min-height: 100%;
  font-family: 'Nunito', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  background-color: #F6F9F4;
  /* Premium backdrop-blend background style with custom image support - opacity lowered to 0.40 for clearer background image visibility */
  background-image: linear-gradient(135deg, rgba(246, 249, 244, 0.40) 0%, rgba(230, 240, 225, 0.40) 100%), url('/images/bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  color: #2F4F4F;
  transition: background 0.3s ease;
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
  background: linear-gradient(135deg, rgba(94, 175, 102, 0.95) 0%, rgba(46, 125, 50, 0.95) 100%);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.25);
  border-left: 1px solid rgba(255, 255, 255, 0.12);
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  color: white;
  padding: 2.5rem 2.5rem 1.8rem; /* Increased top padding from 1.8rem to 2.5rem */
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  box-shadow: 0 8px 32px rgba(46, 125, 50, 0.18), inset 0 1px 1px rgba(255, 255, 255, 0.2);
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* Subtle glowing organic background pattern inside header */
.app-header::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12) 0%, transparent 60%);
  pointer-events: none;
  animation: header-glow-pulse 12s infinite alternate ease-in-out;
}

@keyframes header-glow-pulse {
  0% { transform: translate(-5%, -5%) scale(1); }
  100% { transform: translate(5%, 5%) scale(1.08); }
}

.app-header .logo {
  font-size: 2.5rem;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 50%;
  padding: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08), inset 0 2px 4px rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 2px solid rgba(255, 255, 255, 0.8);
}

.app-header .logo:hover {
  transform: rotate(15deg) scale(1.1);
  box-shadow: 0 8px 25px rgba(76, 175, 80, 0.3), inset 0 2px 4px rgba(255, 255, 255, 1);
  background: #ffffff;
}

.app-header .logo svg {
  transition: transform 0.4s ease;
}

.app-header .logo:hover svg {
  transform: scale(1.08);
}

.title-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.2rem;
}

.title-wrapper {
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.18));
  display: inline-block;
}

.title-container h1 {
  margin: 0;
  font-size: 2.6rem; /* Enlarge title for prominent hierarchy */
  font-weight: 900; /* Extra heavy weight */
  letter-spacing: 5px; /* Distinctive spacing */
  background: linear-gradient(to right, #ffffff 0%, #E8F5E9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1.3; /* Fix clipping vertically */
  padding-bottom: 4px; /* Avoid cutoff on bottom edge */
  padding-right: 8px; /* Compensation for letter-spacing clipping */
  display: block;
}

.title-container p {
  margin: 0.1rem 0 0 0;
  font-size: 0.92rem; /* Reduce subtitle size to create clear hierarchy contrast */
  font-weight: 400; /* Normal weight instead of bold */
  letter-spacing: 1px; /* Tighter letter spacing */
  color: rgba(255, 255, 255, 0.85); /* Highly readable semi-transparent white */
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.app-main {
  flex: 1;
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
  color: #1B5E20; /* Darker green for high readability */
  margin-top: 0;
  margin-bottom: 1.2rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-bottom: 2px solid rgba(27, 94, 32, 0.22);
  padding-bottom: 8px;
  text-shadow: 0 1px 3px rgba(255, 255, 255, 0.95), 0 0 1px rgba(255, 255, 255, 0.65); /* Glow shadow to stand out against background */
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
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 1.5rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(0,0,0,0.02);
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

.directory-btn:hover { border-bottom: 4px solid #FF9800; }

/* 算算TA在干什么 按钮样式 */
.map-actions-bottom {
  margin-top: 3.2rem; /* Increased margin to prevent overlap with the paw button */
  display: flex;
  justify-content: center;
  width: 100%;
}

.ai-calc-btn {
  background: linear-gradient(135deg, #7E57C2, #5E35B1);
  color: white !important;
  border: none;
  border-radius: 18px;
  padding: 1.1rem 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.8rem;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(94, 53, 177, 0.25);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  font-family: inherit;
}

.ai-calc-btn:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 10px 25px rgba(94, 53, 177, 0.35);
}

.ai-calc-btn:active {
  transform: translateY(1px) scale(0.98);
}

.ai-calc-btn .icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-calc-btn .text {
  font-weight: 800;
  color: white !important;
  font-size: 1.1rem;
  letter-spacing: 0.5px;
}

@media (max-width: 480px) {
  .ai-calc-btn {
    width: 100%;
    padding: 1rem 1.5rem;
  }
  .ai-calc-btn .text {
    font-size: 1rem;
  }
}

/* 统一的侧边栏卡片基础样式 */
.sidebar-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  padding: 1.25rem 1.5rem;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.02), inset 0 1px 1px rgba(255, 255, 255, 0.3);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.sidebar-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(76, 175, 80, 0.08);
  border-color: rgba(76, 175, 80, 0.25);
}

/* 统计看板小卡片 */
.stat-card {
  padding: 1.5rem 1rem;
  display: flex;
  justify-content: space-around;
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
  border-left: 5px solid #388E3C;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
}

.sidebar-section .section-title {
  margin-bottom: 0.8rem;
}


.tip-card h3 {
  margin: 0 0 0.5rem 0;
  font-size: 0.95rem;
  color: #1B5E20; /* Darker green */
  font-weight: 800;
  display: flex;
  align-items: center;
}

.tip-card p {
  margin: 0;
  font-size: 0.84rem;
  color: #2E4C2E; /* Much darker green for strong contrast */
  line-height: 1.6;
  font-weight: 500;
}

/* 生活日记 (Life Diary) 便签风格 UI Styles */
.diary-container {
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
  perspective: 1000px;
}

/* 生活日记平滑过渡动画 (渐显+滑入+3D折叠翻页) */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.65s cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(40px) scale(0.97) rotateX(3deg);
}

.full-width-diary {
  width: 100%;
}

.diary-records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diary-record-item {
  font-family: inherit;
  font-size: 0.92rem;
  line-height: 1.6;
  color: #3E2723; /* Pencil dark brown */
  margin: 0;
  padding: 16px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  margin-bottom: 24px;
}

.animal-ai-summary {
  background: rgba(255, 248, 225, 0.7);
  border-left: 4px solid #FFCA28;
  padding: 12px 16px;
  margin: 12px 0 20px 0;
  border-radius: 8px;
  font-size: 0.95rem;
  color: #5D4037;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.timeline {
  border-left: 3px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.01);
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: all 0.2s;
  text-align: left;
}

.diary-record-item:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateX(2px);
}

.diary-record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.08);
  padding-bottom: 4px;
  margin-bottom: 2px;
}

.diary-record-item .record-time {
  font-size: 0.78rem;
  color: #795548; /* Warm handwriting pencil tone */
  font-weight: 600;
  letter-spacing: 0.5px;
}

.diary-record-item .record-bullet {
  font-size: 0.85rem;
  color: #81C784;
}

.diary-record-item .record-text {
  flex: 1;
  letter-spacing: 0.3px;
}

.diary-text-empty {
  font-family: inherit;
  font-size: 0.88rem;
  line-height: 1.7;
  color: #795548;
  text-align: center;
  padding: 20px 0;
}

.diary-card-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #2F4F4F;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.08);
  padding-bottom: 10px;
}

.diary-title-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.close-diary-btn {
  background: none;
  border: none;
  color: #7f9f7f;
  font-size: 1.6rem;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  width: 26px;
  height: 26px;
}

.close-diary-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #d32f2f;
  transform: rotate(90deg);
}

.diary-paper {
  background: #FFFDE7;
  background: linear-gradient(135deg, #FFFDE7 0%, #FFFDE7 90%, #FFF9C4 100%);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05), 0 3px 10px rgba(0,0,0,0.03);
  border-radius: 8px;
  padding: 24px 20px 20px 20px;
  position: relative;
  transform: rotate(-0.5deg);
  border-left: 5px solid #FFEB3B;
  transition: all 0.3s ease;
}

.diary-paper.cat-paper {
  background: linear-gradient(135deg, #F5FBEF 0%, #F5FBEF 90%, #ECF6E6 100%);
  border-left: 5px solid #81C784;
}

.diary-paper.dog-paper {
  background: linear-gradient(135deg, #FFF8E1 0%, #FFF8E1 90%, #FFE0B2 100%);
  border-left: 5px solid #FFB74D;
}

/* Washi Tape effect */
.diary-pin {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  width: 90px;
  height: 24px;
  background-color: rgba(255, 235, 59, 0.35);
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  border: 1px dashed rgba(0,0,0,0.06);
  backdrop-filter: blur(1px);
}

.cat-paper .diary-pin {
  background-color: rgba(129, 199, 132, 0.35);
}

.dog-paper .diary-pin {
  background-color: rgba(255, 183, 77, 0.35);
}

.diary-paper:hover {
  transform: translateY(-2px) rotate(0deg);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
}

.diary-content-area {
  min-height: 100px;
}

.diary-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  gap: 8px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  background-color: #81C784;
  border-radius: 50%;
  display: inline-block;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dot:nth-child(1) { animation-delay: -0.32s; }
.loading-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1.0); }
}

.diary-text {
  font-family: inherit;
  font-size: 0.88rem;
  line-height: 1.7;
  color: #3E2723; /* Pencil dark brown */
  margin: 0 0 15px 0;
  white-space: pre-wrap;
  letter-spacing: 0.4px;
}

.diary-footer {
  text-align: right;
  font-size: 0.75rem;
  font-weight: 700;
  color: #795548;
  opacity: 0.8;
  border-top: 1px dashed rgba(0, 0, 0, 0.05);
  padding-top: 8px;
}

/* AI Modal overlay style */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000; /* Higher than MapContainer's z-index */
  padding: 20px;
}

.modal-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(25px);
  -webkit-backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 24px;
  max-width: 550px;
  width: 100%;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: scaleUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes scaleUp {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.4);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #2E7D32;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-modal-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #90A4AE;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-modal-btn:hover {
  background: rgba(0,0,0,0.05);
  color: #37474F;
}

.modal-body {
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.animal-selector-row {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.animal-selector-row label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #455A64;
}

.animal-select-dropdown {
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.1);
  background-color: white;
  font-weight: 700;
  color: #2F4F4F;
  outline: none;
  cursor: pointer;
  font-size: 0.95rem;
  width: 100%;
  transition: all 0.2s;
  box-shadow: 0 2px 5px rgba(0,0,0,0.02);
}

.animal-select-dropdown:focus {
  border-color: #81C784;
  box-shadow: 0 0 0 3px rgba(129, 199, 132, 0.2);
}

.reasoning-box {
  background: rgba(255, 253, 240, 0.95); /* Warm cream yellow */
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(255, 235, 59, 0.15);
  min-height: 90px;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.01), 0 4px 12px rgba(255, 235, 59, 0.04);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reasoning-text {
  line-height: 1.8;
  font-size: 1.05rem;
  font-weight: 600;
  color: #3E2723; /* Pencil dark brown */
  margin: 0;
  white-space: pre-wrap;
  text-align: center;
}

.reasoning-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  gap: 12px;
  color: #78909C;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(129, 199, 132, 0.2);
  border-top-color: #81C784;
  border-radius: 50%;
  animation: spin 1s infinite linear;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(255,255,255,0.4);
}

.feedback-actions {
  display: flex;
  gap: 12px;
}

.btn-confirm {
  flex: 1;
  padding: 14px 20px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #66BB6A, #43A047);
  color: white;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.25);
  transition: all 0.2s;
  font-size: 0.95rem;
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.35);
}

.btn-correct {
  flex: 1;
  padding: 14px 20px;
  border-radius: 14px;
  border: 1px solid #FFB74D;
  background: white;
  color: #F57C00;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  font-size: 0.95rem;
}

.btn-correct:hover {
  background: #FFF8E1;
  transform: translateY(-2px);
}

.correction-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #FFF8E1;
  border: 1px solid #FFE082;
  padding: 16px;
  border-radius: 16px;
  margin-top: 4px;
  animation: slideDown 0.25s ease-out;
}

@keyframes slideDown {
  from { transform: translateY(-10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.correction-label {
  font-weight: 700;
  color: #E65100;
  font-size: 0.9rem;
}

.correction-dropdown {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #FFD54F;
  outline: none;
  background: white;
  font-weight: 700;
  color: #E65100;
  cursor: pointer;
  font-size: 0.9rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.correction-buttons {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.btn-submit-correction {
  flex: 1;
  padding: 12px;
  border-radius: 10px;
  border: none;
  background: #FF9800;
  color: white;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.btn-submit-correction:hover {
  background: #F57C00;
}

.btn-cancel-correction {
  padding: 12px 20px;
  border-radius: 10px;
  border: 1px solid #CFD8DC;
  background: #CFD8DC;
  color: #37474F;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.btn-cancel-correction:hover {
  background: #B0BEC5;
}

/* Toast notification styling */
.toast-notification {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(46, 125, 50, 0.95);
  color: white;
  padding: 12px 24px;
  border-radius: 50px;
  box-shadow: 0 8px 25px rgba(0,0,0,0.15);
  z-index: 10000;
  font-weight: 700;
  font-size: 0.9rem;
  pointer-events: none;
  animation: fadeInUp 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes fadeInUp {
  from { transform: translate(-50%, 20px); opacity: 0; }
  to { transform: translate(-50%, 0); opacity: 1; }
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

/* 动物图鉴分隔栏 Bottom Section Styles */
.directory-section-bottom {
  margin-top: 3rem;
  background-color: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0,0,0,0.03);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.directory-empty-msg {
  text-align: center;
  padding: 30px 0;
  color: #78909C;
  font-weight: 700;
  font-size: 0.95rem;
}

.directory-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

.directory-card {
  background: #F9FBF9;
  border: 1px solid rgba(0,0,0,0.04);
  border-radius: 18px;
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 4px 12px rgba(0,0,0,0.01);
}

.directory-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 25px rgba(0,0,0,0.06);
  border-color: rgba(129, 199, 132, 0.4);
  background: #ffffff;
}

.directory-card.is-active-tracking {
  border-color: #81C784;
  border-width: 2px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(129, 199, 132, 0.15);
  transform: translateY(-3px);
}

.active-tooltip-notebook {
  position: absolute;
  top: -10px;
  right: -25px;
  transform: rotate(15deg);
  background-color: #fff9e6;
  border: 1px solid #ffd54f;
  padding: 4px 10px;
  border-radius: 4px;
  color: #795548;
  font-size: 0.8rem;
  font-weight: bold;
  box-shadow: 1px 2px 5px rgba(0,0,0,0.1);
  z-index: 10;
  pointer-events: none;
  animation: float-tooltip-tilt 2s ease-in-out infinite;
  white-space: nowrap;
}

.active-tooltip-notebook::before {
  content: '';
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  width: 24px;
  height: 10px;
  background-color: rgba(255, 255, 255, 0.7);
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

@keyframes float-tooltip-tilt {
  0% { transform: rotate(15deg) translateY(0); }
  50% { transform: rotate(15deg) translateY(-3px); }
  100% { transform: rotate(15deg) translateY(0); }
}

.card-nickname {
  position: absolute;
  top: 12px;
  left: 14px;
  font-weight: 800;
  font-size: 0.95rem;
  color: #2E7D32;
}

.card-breed-badge {
  position: absolute;
  top: 12px;
  right: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.card-breed-badge.cat {
  background-color: #E8F5E9;
  color: #4CAF50;
}

.card-breed-badge.dog {
  background-color: #FFF3E0;
  color: #FF9800;
}

.card-breed-badge.other {
  background-color: #ECEFF1;
  color: #607D8B;
}

.card-media {
  margin-top: 2.5rem;
  margin-bottom: 0.8rem;
  width: 100%;
  height: 110px;
  background: #E8F5E9;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed rgba(0,0,0,0.05);
}

.directory-card:hover .card-media {
  background: #F1F8E9;
  border-color: rgba(129, 199, 132, 0.2);
}

.card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.media-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.media-placeholder-txt {
  font-size: 0.72rem;
  color: #78909C;
  font-weight: 700;
}

.card-footer {
  width: 100%;
  text-align: center;
  border-top: 1px solid rgba(0,0,0,0.04);
  padding-top: 10px;
  margin-top: auto;
}

.card-breed-name {
  font-size: 0.8rem;
  font-weight: 800;
  color: #90A4AE;
}

/* 行为轨迹 Modal Styles */
.trajectory-modal {
  max-width: 650px !important;
}

.trajectory-body {
  max-height: 70vh !important;
}

.trajectory-loading, .trajectory-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
  color: #78909C;
  font-weight: 700;
}

.timeline-container {
  margin-top: 10px;
}

.timeline-summary {
  font-size: 0.92rem;
  color: #455A64;
  font-weight: 700;
  margin-bottom: 24px;
  border-bottom: 1px dashed rgba(0,0,0,0.06);
  padding-bottom: 12px;
}

.timeline-summary strong {
  color: #2E7D32;
  font-size: 1.1rem;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative;
  padding-left: 24px;
  border-left: 2px solid #E8F5E9;
  margin-left: 12px;
}

.timeline-item {
  position: relative;
}

.timeline-badge {
  position: absolute;
  left: -33px;
  top: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background-color: #90A4AE;
  border: 3px solid #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}

.timeline-badge.eating { background-color: #FF9800; }
.timeline-badge.sleeping { background-color: #2196F3; }
.timeline-badge.playing { background-color: #9C27B0; }
.timeline-badge.sunbathing { background-color: #FFC107; }
.timeline-badge.walking { background-color: #009688; }
.timeline-badge.other { background-color: #607D8B; }

.badge-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
}

.timeline-content {
  background-color: #F8FBF7;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid rgba(0,0,0,0.02);
  box-shadow: 0 4px 10px rgba(0,0,0,0.01);
  transition: all 0.2s;
}

.timeline-content:hover {
  transform: translateX(3px);
  background-color: #ffffff;
  box-shadow: 0 6px 15px rgba(0,0,0,0.03);
  border-color: rgba(129, 199, 132, 0.2);
}

.timeline-time {
  font-size: 0.78rem;
  color: #90A4AE;
  font-weight: 800;
  margin-bottom: 4px;
}

.timeline-behavior {
  margin: 6px 0;
}

.behavior-label-tag {
  font-size: 0.72rem;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 50px;
  color: white;
  display: inline-block;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.behavior-label-tag.eating { background: #FF9800; }
.behavior-label-tag.sleeping { background: #2196F3; }
.behavior-label-tag.playing { background: #9C27B0; }
.behavior-label-tag.sunbathing { background: #FFC107; }
.behavior-label-tag.walking { background: #009688; }
.behavior-label-tag.other { background: #607D8B; }

.timeline-desc {
  font-size: 0.9rem;
  color: #37474F;
  margin-top: 6px;
  line-height: 1.6;
  font-weight: 500;
}

.timeline-photo {
  margin-top: 12px;
  max-width: 260px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid white;
  box-shadow: 0 4px 15px rgba(0,0,0,0.08);
}

.timeline-photo img {
  width: 100%;
  display: block;
}

/* --- Sidebar Auth Card Styles --- */
.sidebar-auth-card {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.user-status-wrapper {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.user-status-avatar {
  width: 40px;
  height: 40px;
  background: rgba(85, 139, 47, 0.1);
  color: #558B2F;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}

.user-status-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.user-status-role {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  width: fit-content;
}

.user-status-role.guest {
  background: rgba(102, 102, 102, 0.1);
  color: #666;
}

.user-status-role.user {
  background: rgba(25, 118, 210, 0.1);
  color: #1976D2;
}

.user-status-nickname {
  font-size: 0.95rem;
  font-weight: 700;
  color: #333;
}

.sidebar-auth-actions {
  display: flex;
  width: 100%;
}

.sidebar-login-btn {
  width: 100%;
  background: linear-gradient(135deg, #4CAF50, #2E7D32);
  color: white !important;
  border: none;
  border-radius: 10px;
  padding: 0.7rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(46, 125, 50, 0.2);
  transition: all 0.3s ease;
  font-family: inherit;
  font-size: 0.88rem;
}

.sidebar-login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(46, 125, 50, 0.3);
}

.sidebar-logout-btn {
  width: 100%;
  background: #fff;
  color: #d93025 !important;
  border: 1px solid #fad2cf;
  border-radius: 10px;
  padding: 0.7rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: inherit;
  font-size: 0.88rem;
}

.sidebar-logout-btn:hover {
  background: #fce8e6;
  border-color: #d93025;
}

/* --- Auth Modal Overlay & Card Styles --- */
.auth-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.auth-modal-card {
  background: rgba(255, 255, 255, 0.95);
  width: 100%;
  max-width: 380px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.15);
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
  animation: modal-pop 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modal-pop {
  from {
    transform: scale(0.9) translateY(10px);
    opacity: 0;
  }
  to {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}

.close-auth-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;
  line-height: 1;
  z-index: 10;
}

.close-auth-btn:hover {
  color: #333;
}

.auth-tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.auth-tab-item {
  flex: 1;
  text-align: center;
  padding: 15px 0;
  cursor: pointer;
  font-weight: bold;
  color: #666;
  transition: all 0.2s;
  border-bottom: 3px solid transparent;
}

.auth-tab-item:hover {
  color: #1976d2;
}

.auth-tab-item.active {
  color: #1976d2;
  border-bottom-color: #1976d2;
  background: #fff;
}

.auth-form-body {
  padding: 25px;
}

.auth-error-msg {
  background: #fce8e6;
  color: #c5221f;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 15px;
  font-size: 0.9em;
  border: 1px solid #fad2cf;
  text-align: left;
}

.auth-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 15px;
  text-align: left;
}

.auth-form-group label {
  font-size: 0.85em;
  font-weight: bold;
  color: #666;
}

.auth-input {
  padding: 10px 12px;
  border: 1px solid #dadce0;
  border-radius: 6px;
  font-size: 0.95em;
  transition: border-color 0.2s;
}

.auth-input:focus {
  outline: none;
  border-color: #1976d2;
}

.auth-submit-btn {
  width: 100%;
  padding: 12px;
  background: #1976d2;
  color: white !important;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  font-size: 1em;
  margin-top: 10px;
  transition: background 0.2s;
  font-family: inherit;
}

.auth-submit-btn:hover {
  background: #1565c0;
}

.auth-submit-btn:disabled {
  background: #90caf9;
  cursor: not-allowed;
}

.auth-modal-footer {
  text-align: center;
  margin-top: 15px;
  font-size: 0.85em;
}

.continue-guest-link {
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.continue-guest-link:hover {
  color: #1976d2;
  text-decoration: underline;
}

/* 动效 */
.auth-fade-enter-active, .auth-fade-leave-active {
  transition: opacity 0.25s ease;
}
.auth-fade-enter-from, .auth-fade-leave-to {
  opacity: 0;
}
</style>
