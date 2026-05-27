<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';
import MapContainer from './components/MapContainer.vue';

// Define Interface
interface Animal {
  id: number;
  name: string;
  breed: string;
  qrCodeId?: string;
  createdAt?: string;
}

// Global Stats & Animals list
const animals = ref<Animal[]>([]);
const selectedAnimalId = ref<number | null>(null);
const latestNarrative = ref<string>('');
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
    const res = await axios.get('http://localhost:8080/api/animals');
    animals.value = res.data;
    totalAnimalsCount.value = res.data.length;
    
    // Default select first animal if not selected
    if (res.data.length > 0 && !selectedAnimalId.value) {
      selectedAnimalId.value = res.data[0].id;
    }
  } catch (error) {
    console.error('获取动物列表失败:', error);
  }
};

// Fetch total location logs count for stats
const fetchLogsCount = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/locations/all');
    totalFootprintsCount.value = res.data.length;
  } catch (error) {
    console.error('获取足迹统计失败:', error);
  }
};

// Fetch animal diary
const fetchNarrative = async (animalId: number) => {
  isNarrativeLoading.value = true;
  latestNarrative.value = '';
  try {
    const res = await axios.get(`http://localhost:8080/api/analysis/narrative/${animalId}`);
    latestNarrative.value = res.data.narrative;
  } catch (error) {
    console.error('获取日记失败:', error);
    latestNarrative.value = '翻阅日记失败，请稍后重试🐾';
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

// Fetch behavior prediction for the selected animal
const fetchBehaviorReasoning = async (animalId: number) => {
  isAiLoading.value = true;
  aiReasoningResult.value = '';
  showCorrectionDropdown.value = false;
  selectedCorrectBehavior.value = '';
  try {
    const res = await axios.get(`http://localhost:8080/api/analysis/behavior/${animalId}`);
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
    await axios.post('http://localhost:8080/api/analysis/feedback', {
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
    await axios.post('http://localhost:8080/api/analysis/feedback', {
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
    latestNarrative.value = '';
  }
});

let statsIntervalId: any = null;

onMounted(() => {
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
            <MapContainer @select-animal="handleSelectAnimal" />
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
              <button class="action-btn ai-btn" @click="openAiModal">
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

            <!-- 成长日记 (Life Narrative) 便签卡片 -->
            <div v-if="selectedAnimal" class="diary-container">
              <div class="diary-card-title">
                <span class="paw-icon">🐾</span>
                {{ selectedAnimal.name }} 的成长日记
              </div>
              
              <div class="diary-paper" :class="selectedAnimal.breed === 'Cat' ? 'cat-paper' : 'dog-paper'">
                <div class="diary-pin"></div>
                <div class="diary-content-area">
                  <div v-if="isNarrativeLoading" class="diary-loading">
                    <span class="loading-dot"></span>
                    <span class="loading-dot"></span>
                    <span class="loading-dot"></span>
                    <p>正在翻阅日记本...</p>
                  </div>
                  <div v-else>
                    <p class="diary-text">{{ latestNarrative || '这只小家伙还没有成长日记喔。在地图上添加或更新它的足迹，AI 行为分析师就会为它自动撰写生动幽默的生活故事啦！🐾' }}</p>
                    <div class="diary-footer">
                      <span>✍️ 校园小生命分析师 Gemini</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="tip-card">
              <h3>🐾 地图使用引导</h3>
              <p>直接点击地图上的任意空白位置，可在该处悬浮生成一个绿色的临时标记，再次点击绿色标记便能快捷录入新的发现记录。点击右下悬浮爪印，可实现自动定位记录！</p>
            </div>
          </section>
        </div>
      </div>
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

    <!-- 悬浮微型 Toast 提示 -->
    <div v-if="showToast" class="toast-notification">
      {{ toastMessage }}
    </div>
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

/* 成长日记 (Growth Diary) 便签风格 UI Styles */
.diary-container {
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
  perspective: 1000px;
}

.diary-card-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #558B2F;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
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
  padding: 24px;
  overflow-y: auto;
  max-height: 60vh;
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
  background: rgba(255,255,255,0.95);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(0,0,0,0.04);
  min-height: 120px;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
  margin-bottom: 8px;
}

.reasoning-text {
  line-height: 1.7;
  font-size: 0.95rem;
  color: #37474F;
  margin: 0;
  white-space: pre-wrap;
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
</style>
