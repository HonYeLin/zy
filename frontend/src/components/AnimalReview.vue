<template>
  <div class="animal-review-container">
    <!-- Close button for the details panel -->
    <button class="close-review-btn" @click="$emit('close')" title="关闭面板">×</button>
    <div class="review-top-row">
      <!-- 左侧：图鉴拍立得相框样式 -->
      <div class="encyclopedia-side">
        <div class="polaroid-wrapper">
          <div class="polaroid-card">
            <div class="polaroid-tape"></div>
            <div class="polaroid-image-container" @click="isImageZoomed = true" title="点击放大图片">
              <img v-if="animal.avatarUrl" :src="animal.avatarUrl.replace('http://localhost:8080', '')" class="animal-avatar" alt="avatar" />
            </div>
            <div class="polaroid-caption">
              <h3 class="handwritten-name">{{ animal.name }}</h3>
            </div>
          </div>
        </div>
        <div class="animal-info-notebook">
          <div class="notebook-tag">✍️ {{ getAnimalTypeTitle(animal.breed) }}</div>
          <p class="animal-desc">{{ animal.aiSummary || animal.description || '暂无描述' }}</p>
        </div>
      </div>

      <!-- 右侧：打分选项与展示 -->
      <div class="rating-side">
        <div class="rating-box-inner">
          <h4 class="rating-title">给 {{ animal.name }} 打分</h4>
          
          <div class="rating-grid">
            <div class="rating-item" v-for="metric in metrics" :key="metric.key">
              <span class="metric-label">{{ metric.label }}</span>
              <div class="stars-input" :class="{ 'disabled-stars': isGuest }">
                <span 
                  v-for="n in 10" 
                  :key="n" 
                  class="star-half"
                  :class="{ 
                    'active': userRating[metric.key] >= n,
                    'is-right-half': n % 2 === 0,
                    'is-left-half': n % 2 !== 0
                  }"
                  @click="isGuest ? showLoginAlert() : setRating(metric.key, n)"
                ></span>
              </div>
              <span class="metric-score">{{ userRating[metric.key] }} / 10</span>
            </div>
          </div>
          
          <div class="submit-rating-row">
             <button class="submit-rating-btn" @click="submitRating" :disabled="isSubmittingRating || isGuest">
               {{ isGuest ? '请先登录后再打分' : '提交评分' }}
             </button>
          </div>
        </div>

        <div class="rating-stats" v-if="stats && stats.totalRatings > 0">
           <h5>平均评分 (共 {{ stats.totalRatings }} 次打分)</h5>
           <div class="stats-grid">
             <div class="stat-item">颜值: <span class="stat-score">{{ stats.appearanceAvg.toFixed(1) }}</span></div>
             <div class="stat-item">脾气: <span class="stat-score">{{ stats.temperAvg.toFixed(1) }}</span></div>
             <div class="stat-item">可见度: <span class="stat-score">{{ stats.visibilityAvg.toFixed(1) }}</span></div>
             <div class="stat-item">粘人度: <span class="stat-score">{{ stats.clinginessAvg.toFixed(1) }}</span></div>
           </div>
        </div>
        <div class="rating-stats" v-else>
           <p class="no-ratings">暂无评分，快来做第一个打分的人吧！</p>
        </div>
      </div>
    </div>

    <!-- 下方：留言区 -->
    <div class="comments-section">
      <h4 class="comments-title">💬 他言我语</h4>
      
      <!-- 留言输入框 -->
      <div class="comment-input-box">
        <div class="user-login-status">
          <span class="status-badge" v-if="currentUser">
            👤 当前身份: <strong>{{ currentUser.nickname }}</strong>
            <span class="role-indicator" :class="currentUser.role.toLowerCase()">
              {{ currentUser.role === 'GUEST' ? '游客' : '会员' }}
            </span>
          </span>
          <div class="auth-actions">
            <button v-if="currentUser && currentUser.role === 'GUEST'" class="login-stub-btn" @click="emit('open-auth', 'login')">
              登录 / 注册
            </button>
            <button v-else class="logout-btn" @click="emit('logout')">
              退出登录
            </button>
          </div>
        </div>
        <textarea v-model="newComment.content" placeholder="写下你对它的印象或有趣的故事..." class="comment-textarea"></textarea>
        <div class="comment-actions">
          <button class="submit-comment-btn" @click="submitComment" :disabled="isSubmittingComment || !newComment.content.trim()">发布留言</button>
        </div>
      </div>

      <!-- 留言列表区 -->
      <div class="comments-list-container">
        <div class="comments-header">
           <span class="comments-count">共 {{ totalComments }} 条留言</span>
           <div class="sort-tabs">
             <span :class="{ active: currentSort === 'createdAt' }" @click="changeSort('createdAt')">最新</span>
             <span :class="{ active: currentSort === 'likeCount' }" @click="changeSort('likeCount')">最热</span>
           </div>
        </div>
        
        <div v-if="isLoadingComments" class="comments-loading">加载中...</div>
        <div v-else-if="comments.length === 0" class="comments-empty">还没有人留言，快来说两句吧！</div>
        
        <div v-else class="comments-list">
           <div 
             class="comment-item" 
             v-for="(comment, index) in comments" 
             :key="comment.id"
             :class="['sticky-note-' + (index % 4)]"
           >
              <div class="comment-tape"></div>
              <div class="comment-user">
                <span class="user-avatar-placeholder">{{ comment.userNickname.charAt(0) }}</span>
                <span class="user-name">{{ comment.userNickname }}</span>
                <span class="comment-time">{{ new Date(comment.createdAt).toLocaleString() }}</span>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-footer">
                <button 
                  class="like-btn" 
                  :class="{ 'has-liked': likedCommentIds.includes(comment.id) }"
                  @click="likeComment(comment.id)"
                >
                  👍 {{ comment.likeCount }}
                </button>
                <!-- Delete button: visible only to comment owner -->
                <button 
                  v-if="currentUser && comment.userId === currentUser.id"
                  class="delete-comment-btn"
                  @click="deleteComment(comment.id)"
                  title="删除留言"
                >
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    <line x1="10" y1="11" x2="10" y2="17"></line>
                    <line x1="14" y1="11" x2="14" y2="17"></line>
                  </svg>
                  删除
                </button>
              </div>
           </div>
        </div>

        <!-- 分页控件 -->
        <div class="pagination" v-if="totalPages > 1">
           <button :disabled="currentPage === 0" @click="changePage(currentPage - 1)">上一页</button>
           <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
           <button :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">下一页</button>
        </div>
      </div>
    </div>

    <!-- 图片放大弹窗 (手账风格) -->
    <Transition name="zoom-fade">
      <div v-if="isImageZoomed && animal.avatarUrl" class="image-zoom-overlay" @click.self="isImageZoomed = false">
        <div class="image-zoom-modal">
          <button class="close-zoom-btn" @click="isImageZoomed = false" title="关闭">×</button>
          <div class="zoom-polaroid">
            <div class="zoom-polaroid-tape"></div>
            <div class="zoom-image-container">
              <img :src="animal.avatarUrl.replace('http://localhost:8080', '')" alt="enlarged avatar" />
            </div>
            <div class="zoom-caption">
              <h3 class="handwritten-name">{{ animal.name }}</h3>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';

const isImageZoomed = ref(false);

const props = defineProps<{
  animal: any;
  currentUser?: any;
}>();

const isGuest = computed(() => {
  return !props.currentUser || props.currentUser.role === 'GUEST';
});

const showLoginAlert = () => {
  alert('未登录游客仅能查看评分，请先登录/注册后再打分！');
};

const getAnimalTypeTitle = (type: string) => {
  if (!type) return 'TA的简介';
  const t = type.toLowerCase();
  if (t.includes('猫') || t.includes('cat')) {
    return '咪的简介';
  } else if (t.includes('狗') || t.includes('dog')) {
    return '汪的简介';
  }
  return 'TA的简介';
};

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'open-auth', tab: string): void;
  (e: 'logout'): void;
}>();

// --- 评分系统 ---
const metrics = [
  { key: 'appearanceScore', label: '颜值' },
  { key: 'temperScore', label: '脾气' },
  { key: 'visibilityScore', label: '可见度' },
  { key: 'clinginessScore', label: '粘人度' }
];

const userRating = ref<Record<string, number>>({
  appearanceScore: 0,
  temperScore: 0,
  visibilityScore: 0,
  clinginessScore: 0
});

const stats = ref<any>(null);
const isSubmittingRating = ref(false);

const setRating = (metric: string, score: number) => {
  userRating.value[metric] = score;
};

const fetchStats = async () => {
  try {
    const res = await fetch(`/api/reviews/ratings/${props.animal.id}/stats`);
    if (res.ok) {
      stats.value = await res.json();
    }
  } catch (error) {
    console.error('Failed to fetch rating stats', error);
  }
};

const submitRating = async () => {
  if (userRating.value.appearanceScore === 0 && userRating.value.temperScore === 0 && 
      userRating.value.visibilityScore === 0 && userRating.value.clinginessScore === 0) {
    alert("请至少为一个选项打分");
    return;
  }
  isSubmittingRating.value = true;
  try {
    const res = await fetch(`/api/reviews/ratings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        animalId: props.animal.id,
        userId: props.currentUser ? props.currentUser.id : null,
        ...userRating.value
      })
    });
    if (res.ok) {
      alert("评分成功！");
      fetchStats();
      // Reset
      userRating.value = { appearanceScore: 0, temperScore: 0, visibilityScore: 0, clinginessScore: 0 };
    }
  } catch (error) {
    console.error("Failed to submit rating", error);
  } finally {
    isSubmittingRating.value = false;
  }
};


// --- 留言系统 ---
const comments = ref<any[]>([]);
const totalComments = ref(0);
const totalPages = ref(0);
const currentPage = ref(0);
const currentSort = ref('createdAt');
const isLoadingComments = ref(false);

const newComment = ref({
  content: ''
});
const isSubmittingComment = ref(false);
const likedCommentIds = ref<number[]>([]);
try {
  likedCommentIds.value = JSON.parse(localStorage.getItem('liked_comment_ids') || '[]');
} catch (e) {
  likedCommentIds.value = [];
}

// --- 留言与评分系统 ---

const fetchComments = async () => {
  isLoadingComments.value = true;
  try {
    const res = await fetch(`/api/reviews/comments/${props.animal.id}?page=${currentPage.value}&size=5&sort=${currentSort.value}`);
    if (res.ok) {
      const data = await res.json();
      comments.value = data.content;
      totalComments.value = data.totalElements;
      totalPages.value = data.totalPages;
    }
  } catch (error) {
    console.error('Failed to fetch comments', error);
  } finally {
    isLoadingComments.value = false;
  }
};

const submitComment = async () => {
  if (!newComment.value.content.trim()) return;
  isSubmittingComment.value = true;
  try {
    const res = await fetch(`/api/reviews/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        animalId: props.animal.id,
        userId: props.currentUser ? props.currentUser.id : null,
        userNickname: props.currentUser ? props.currentUser.nickname : '游客',
        content: newComment.value.content.trim()
      })
    });
    if (res.ok) {
      newComment.value.content = '';
      currentPage.value = 0; // go to first page
      fetchComments();
    }
  } catch (error) {
    console.error("Failed to submit comment", error);
  } finally {
    isSubmittingComment.value = false;
  }
};

const likeComment = async (commentId: number) => {
  const isLiked = likedCommentIds.value.includes(commentId);
  try {
    const res = await fetch(`/api/reviews/comments/${commentId}/like?cancel=${isLiked}`, { method: 'POST' });
    if (res.ok) {
      // update local
      const c = comments.value.find(x => x.id === commentId);
      if (c) {
        if (isLiked) {
          c.likeCount = Math.max(0, c.likeCount - 1);
        } else {
          c.likeCount++;
        }
      }
      // update likedCommentIds and persist
      if (isLiked) {
        likedCommentIds.value = likedCommentIds.value.filter(id => id !== commentId);
      } else {
        likedCommentIds.value.push(commentId);
      }
      localStorage.setItem('liked_comment_ids', JSON.stringify(likedCommentIds.value));
    }
  } catch (error) {
    console.error("Failed to toggle like on comment", error);
  }
};

const deleteComment = async (commentId: number) => {
  if (!props.currentUser || !props.currentUser.id) {
    alert('删除失败，未能获取用户信息。');
    return;
  }
  if (!confirm('确认要删除这条留言吗？')) return;
  try {
    const res = await fetch(`/api/reviews/comments/${commentId}?userId=${props.currentUser.id}`, {
      method: 'DELETE'
    });
    if (res.ok) {
      alert('留言已成功删除！');
      fetchComments();
    } else {
      alert('删除失败，可能没有权限。');
    }
  } catch (error) {
    console.error('Failed to delete comment', error);
    alert('删除失败，网络错误。');
  }
};

const changeSort = (sort: string) => {
  if (currentSort.value === sort) return;
  currentSort.value = sort;
  currentPage.value = 0;
  fetchComments();
};

const changePage = (page: number) => {
  if (page < 0 || page >= totalPages.value) return;
  currentPage.value = page;
  fetchComments();
};

// Lifecycle
onMounted(() => {
  fetchStats();
  fetchComments();
});

watch(() => props.animal.id, () => {
  // Reset states when animal changes
  currentPage.value = 0;
  currentSort.value = 'createdAt';
  userRating.value = { appearanceScore: 0, temperScore: 0, visibilityScore: 0, clinginessScore: 0 };
  fetchStats();
  fetchComments();
});
</script>

<style scoped>
.animal-review-container {
  position: relative;
  background: linear-gradient(180deg, #FDFDF7 0%, #FAF7EE 100%);
  border-radius: 24px;
  /* 左侧书脊：深绿色皮质/麻布书脊 */
  border-left: 20px solid #558B2F;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  border-bottom: 3px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 12px 35px rgba(0,0,0,0.08), inset 5px 0 10px rgba(0,0,0,0.03);
  padding: 24px 24px 24px 36px;
  margin-top: 2.5rem;
  margin-bottom: 20px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

/* 缝合线视觉效果 */
.animal-review-container::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: -12px;
  width: 0;
  border-left: 2px dashed rgba(255, 255, 255, 0.45);
  pointer-events: none;
}

.review-top-row {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}

/* 图鉴侧 - 拍立得手账风 */
.encyclopedia-side {
  flex: 0 0 35%;
  border-right: 2px dashed rgba(196, 185, 163, 0.5);
  padding-right: 20px;
}

.polaroid-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  padding-top: 10px;
}

.polaroid-card {
  background: #ffffff;
  padding: 10px 10px 18px 10px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 2px;
  transform: rotate(-3deg);
  transition: all 0.3s ease;
  position: relative;
  max-width: 160px;
  width: 100%;
}

.polaroid-card:hover {
  transform: rotate(0deg) scale(1.05);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.1);
}

.polaroid-tape {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%) rotate(4deg);
  width: 55px;
  height: 16px;
  background: rgba(255, 213, 79, 0.4);
  border: 1px dashed rgba(0, 0, 0, 0.04);
}

.polaroid-image-container {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 1px solid rgba(0,0,0,0.05);
  background: #fdfdfd;
  cursor: zoom-in;
}

.polaroid-image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.polaroid-image-container:hover img {
  transform: scale(1.08);
}

.polaroid-caption {
  margin-top: 8px;
  text-align: center;
}

.handwritten-name {
  margin: 0;
  font-weight: 800;
  font-size: 1.2rem;
  color: #3E2723;
  letter-spacing: 0.5px;
}

.animal-info-notebook {
  background: rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  padding: 12px;
  border: 1px dashed rgba(85, 139, 47, 0.25);
  box-sizing: border-box;
}

.notebook-tag {
  font-size: 0.75rem;
  font-weight: bold;
  color: #558B2F;
  margin-bottom: 6px;
  text-align: left;
  letter-spacing: 0.5px;
}

.animal-desc {
  font-size: 0.9em;
  color: #5D4037;
  line-height: 1.6;
  text-align: left;
  margin: 0;
}

/* 打分侧 */
.rating-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 20px;
}

.rating-box-inner {
  max-width: 360px;
  margin: 0 auto;
  width: 100%;
}

.rating-title {
  margin: 0 0 20px 0;
  color: #3E2723;
  font-size: 1.25em;
  text-align: center;
  font-weight: 800;
}

.rating-grid {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 20px;
}

.rating-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.metric-label {
  width: 75px;
  font-weight: bold;
  color: #5D4037;
  font-size: 1.05em;
}

/* 评分星星样式 */
.stars-input {
  display: inline-flex;
  position: relative;
  font-size: 32px;
  line-height: 1;
  cursor: pointer;
  user-select: none;
}

.stars-input.disabled-stars {
  cursor: not-allowed;
  opacity: 0.65;
}

.star-half {
  width: 16px;
  overflow: hidden;
  color: #e0dcd3;
  transition: color 0.15s, transform 0.1s;
  display: inline-block;
}

.stars-input:not(.disabled-stars) .star-half:hover {
  transform: scale(1.15);
}

.star-half.active {
  color: #ffb300;
}

.star-half.is-left-half::before {
  content: '★';
  display: block;
}

.star-half.is-right-half::before {
  content: '★';
  display: block;
  margin-left: -16px;
}

.metric-score {
  font-size: 1.1em;
  font-weight: bold;
  color: #ff9800;
  min-width: 60px;
  margin-left: 10px;
}

.submit-rating-row {
  margin-top: 10px;
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.submit-rating-btn {
  background: linear-gradient(135deg, #689F38, #558B2F);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: bold;
  font-size: 1em;
  transition: background 0.3s, transform 0.1s;
  width: 100%;
  max-width: 200px;
  box-shadow: 0 4px 10px rgba(85, 139, 47, 0.15);
}

.submit-rating-btn:hover {
  background: #33691E;
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(85, 139, 47, 0.25);
}

.submit-rating-btn:active {
  transform: scale(0.98);
}

.submit-rating-btn:disabled {
  background: #C8E6C9;
  color: #81C784;
  cursor: not-allowed;
  box-shadow: none;
}

.rating-stats {
  margin-top: 20px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px dashed #C4B9A3;
  padding: 12px 18px;
  border-radius: 12px;
  max-width: 360px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  box-sizing: border-box;
  text-align: center;
}

.rating-stats h5 {
  margin: 0 0 10px 0;
  color: #5D4037;
  font-size: 0.95em;
}

.stats-grid {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.stat-item {
  font-size: 0.9em;
  color: #5D4037;
  background: #fff;
  padding: 6px 12px;
  border-radius: 12px;
  border: 1px solid rgba(196, 185, 163, 0.3);
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.stat-score {
  font-weight: bold;
  color: #ff9800;
}

.no-ratings {
  color: #999;
  font-size: 0.9em;
}

/* 留言区 */
.comments-section {
  border-top: 2px dashed rgba(85, 139, 47, 0.15);
  padding-top: 25px;
  margin-top: 20px;
}

.comments-title {
  margin: 0 0 20px 0;
  color: #3E2723;
  font-size: 1.3rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.comment-input-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 30px;
  background: rgba(255, 255, 255, 0.55);
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(85, 139, 47, 0.18);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.02);
}

/* 登录状态栏样式 */
.user-login-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.85);
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid rgba(85, 139, 47, 0.08);
}

.status-badge {
  color: #3E2723;
  font-size: 0.9em;
  font-weight: 600;
}

.login-stub-btn {
  background: #fff;
  border: 1.5px solid rgba(85, 139, 47, 0.3);
  color: #558B2F;
  padding: 4px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85em;
  font-weight: bold;
  transition: all 0.25s ease;
}

.login-stub-btn:hover {
  background: rgba(85, 139, 47, 0.05);
  border-color: #558B2F;
  transform: translateY(-1px);
}

/* 信纸横线风格输入框 */
.comment-textarea {
  width: 100%;
  height: 110px;
  padding: 10px 14px;
  line-height: 24px;
  font-size: 0.95rem;
  border: 1px solid #C4B9A3;
  border-radius: 12px;
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
  color: #3E2723;
  background-color: #FFFDF9;
  background-image: linear-gradient(#e5dec9 1px, transparent 1px);
  background-size: 100% 24px;
  background-attachment: local;
  transition: all 0.3s ease;
}

.comment-textarea:focus {
  outline: none;
  border-color: #8D6E63;
  box-shadow: 0 0 0 4px rgba(141, 110, 99, 0.12);
  background-color: #ffffff;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

.submit-comment-btn {
  background: linear-gradient(135deg, #689F38, #558B2F);
  color: white;
  border: none;
  padding: 10px 28px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
  font-size: 0.95rem;
  box-shadow: 0 4px 12px rgba(85, 139, 47, 0.15);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.submit-comment-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(85, 139, 47, 0.25);
}

.submit-comment-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-comment-btn:disabled {
  background: #C8E6C9;
  color: #81C784;
  cursor: not-allowed;
  box-shadow: none;
}

/* 留言列表头部 */
.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 0.92em;
  color: #555;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  padding-bottom: 10px;
}

.comments-count {
  font-weight: 600;
}

.sort-tabs {
  display: inline-flex;
  background: rgba(0, 0, 0, 0.04);
  padding: 3px;
  border-radius: 20px;
}

.sort-tabs span {
  cursor: pointer;
  padding: 4px 14px;
  border-radius: 18px;
  font-size: 0.88em;
  color: #555;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.sort-tabs span:hover:not(.active) {
  color: #558B2F;
}

.sort-tabs span.active {
  background: white;
  color: #558B2F;
  font-weight: bold;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* 便签拼贴风格留言项 */
.comment-item {
  position: relative;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-sizing: border-box;
}

.comment-item:hover {
  transform: translateY(-5px) scale(1.01) !important;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
  z-index: 5;
}

/* 莫兰迪糖果色便利贴 */
.sticky-note-0 {
  background: #FFFDF0;
  transform: rotate(-0.5deg);
  border-radius: 8px 12px 10px 14px / 12px 10px 14px 8px;
}
.sticky-note-1 {
  background: #F1F9F0;
  transform: rotate(0.6deg);
  border-radius: 12px 8px 14px 10px / 8px 12px 10px 14px;
}
.sticky-note-2 {
  background: #FFF8EE;
  transform: rotate(-0.8deg);
  border-radius: 10px 14px 8px 12px / 14px 8px 12px 10px;
}
.sticky-note-3 {
  background: #F5F7F8;
  transform: rotate(0.4deg);
  border-radius: 14px 10px 12px 8px / 10px 14px 8px 12px;
}

/* 和纸胶带效果 */
.comment-tape {
  position: absolute;
  top: -11px;
  left: 45%;
  width: 50px;
  height: 16px;
  transform: rotate(-2deg);
  border: 1px dashed rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.01);
  pointer-events: none;
}

.sticky-note-0 .comment-tape {
  background-color: rgba(255, 235, 59, 0.22);
}
.sticky-note-1 .comment-tape {
  background-color: rgba(129, 199, 132, 0.22);
}
.sticky-note-2 .comment-tape {
  background-color: rgba(255, 183, 77, 0.22);
}
.sticky-note-3 .comment-tape {
  background-color: rgba(129, 212, 250, 0.22);
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.user-avatar-placeholder {
  width: 38px;
  height: 38px;
  background: linear-gradient(135deg, #81C784, #388E3C);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1rem;
  border: 2px solid white;
  box-shadow: 0 2px 6px rgba(76, 175, 80, 0.15);
}

.user-name {
  font-weight: 700;
  color: #3E2723;
}

.comment-time {
  font-size: 0.8em;
  color: #8D6E63;
}

.comment-content {
  color: #3E2723;
  line-height: 1.6;
  margin-bottom: 10px;
  padding-left: 50px;
  font-size: 0.96rem;
  text-align: left;
}

.comment-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-left: 50px;
}

.like-btn {
  background: none;
  border: none;
  color: #8D6E63;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88em;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.like-btn:hover:not(.has-liked) {
  background: rgba(0, 0, 0, 0.04);
}

.like-btn.has-liked {
  color: #558B2F;
  font-weight: bold;
  background: rgba(129, 199, 132, 0.15);
}

/* 删除按钮 */
.delete-comment-btn {
  background: rgba(229, 57, 53, 0.04);
  border: none;
  color: #E53935;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.82rem;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  margin-left: 12px;
}

.delete-comment-btn:hover {
  background: rgba(229, 57, 53, 0.12);
  color: #D32F2F;
  transform: scale(1.03);
}

.delete-comment-btn:active {
  transform: scale(0.97);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 25px;
}

.pagination button {
  padding: 6px 18px;
  background: #fff;
  border: 1px solid rgba(85, 139, 47, 0.22);
  color: #558B2F;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
  transition: all 0.25s ease;
  font-family: inherit;
}

.pagination button:hover:not(:disabled) {
  background: rgba(85, 139, 47, 0.06);
  border-color: #558B2F;
  transform: translateY(-1px);
}

.pagination button:disabled {
  color: #bbb;
  border-color: #eee;
  background: rgba(0, 0, 0, 0.01);
  cursor: not-allowed;
}

.comments-empty, .comments-loading {
  text-align: center;
  color: #888;
  padding: 40px 0;
}

/* 角色标签与登录注册弹窗样式 */
.role-indicator {
  font-size: 0.75em;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 6px;
  font-weight: bold;
}

.role-indicator.guest {
  background: #efebe9;
  color: #5d4037;
}

.role-indicator.user {
  background: #e8f5e9;
  color: #2e7d32;
}

.logout-btn {
  background: none;
  border: 1px solid #dadce0;
  color: #d93025;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85em;
  font-weight: bold;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #fce8e6;
  border-color: #d93025;
}

/* 关闭面板按钮样式 */
.close-review-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  color: #666;
  font-size: 20px;
  line-height: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.close-review-btn:hover {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
  transform: rotate(90deg);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .animal-review-container {
    padding: 16px 16px 16px 28px;
    border-radius: 16px;
  }
  
  .review-top-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .encyclopedia-side {
    flex: none;
    border-right: none;
    padding-right: 0;
    border-bottom: 1px dashed rgba(0, 0, 0, 0.1);
    padding-bottom: 16px;
  }
  
  .rating-side {
    flex: none;
    padding-left: 0;
  }
  
  .rating-box-inner {
    max-width: 100%;
  }

  .rating-item {
    justify-content: space-between;
  }
  
  .rating-stars {
    margin-left: auto;
  }

  .comment-item {
    padding: 12px;
    margin-bottom: 18px;
  }

  .comment-content, .comment-footer {
    padding-left: 0;
    margin-top: 8px;
  }
  
  .image-zoom-modal {
    padding: 20px 20px 20px 30px;
    border-left-width: 15px;
    max-width: 95vw;
  }
  .zoom-image-container {
    max-height: 50vh;
  }
  .zoom-image-container img {
    max-height: 50vh;
  }
}

/* 图片放大遮罩层 */
.image-zoom-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

/* 图片放大弹窗主体 (复用手账本风格) */
.image-zoom-modal {
  position: relative;
  background: linear-gradient(180deg, #FDFDF7 0%, #FAF7EE 100%);
  border-radius: 24px;
  /* 左侧书脊：深绿色皮质/麻布书脊 */
  border-left: 20px solid #558B2F;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  border-right: 1px solid rgba(0, 0, 0, 0.05);
  border-bottom: 3px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2), inset 5px 0 10px rgba(0, 0, 0, 0.03);
  padding: 30px 30px 30px 45px;
  max-width: 90vw;
  max-height: 90vh;
  box-sizing: border-box;
  animation: modal-enter 0.3s ease-out;
}

/* 缝合线效果 */
.image-zoom-modal::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: -12px;
  width: 0;
  border-left: 2px dashed rgba(255, 255, 255, 0.45);
  pointer-events: none;
}

/* 放大弹窗内的拍立得卡片 */
.zoom-polaroid {
  background: #ffffff;
  padding: 16px 16px 28px 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  transform: rotate(1.5deg);
  transition: transform 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100%;
  box-sizing: border-box;
  position: relative;
}

.zoom-polaroid:hover {
  transform: rotate(0deg);
}

.zoom-polaroid-tape {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  width: 80px;
  height: 24px;
  background: rgba(255, 213, 79, 0.5);
  border: 1px dashed rgba(0, 0, 0, 0.06);
  z-index: 5;
}

.zoom-image-container {
  max-width: 100%;
  max-height: 60vh;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.05);
  background: #fdfdfd;
  display: flex;
  justify-content: center;
  align-items: center;
}

.zoom-image-container img {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
  display: block;
}

.zoom-caption {
  margin-top: 15px;
  text-align: center;
}

.zoom-caption h3 {
  font-family: inherit;
  font-size: 1.5rem;
  color: #3E2723;
  margin: 0;
  letter-spacing: 1px;
}

/* 关闭按钮 */
.close-zoom-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  color: #666;
  font-size: 24px;
  line-height: 34px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.close-zoom-btn:hover {
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
  transform: rotate(90deg);
}

/* 动效 */
@keyframes modal-enter {
  from {
    opacity: 0;
    transform: scale(0.9) rotate(-2deg);
  }
  to {
    opacity: 1;
    transform: scale(1) rotate(0deg);
  }
}

/* Zoom Transition */
.zoom-fade-enter-active,
.zoom-fade-leave-active {
  transition: opacity 0.3s ease;
}
.zoom-fade-enter-from,
.zoom-fade-leave-to {
  opacity: 0;
}

.zoom-fade-enter-active .image-zoom-modal,
.zoom-fade-leave-active .image-zoom-modal {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.zoom-fade-enter-from .image-zoom-modal {
  transform: scale(0.9);
}
.zoom-fade-leave-to .image-zoom-modal {
  transform: scale(0.95);
}
</style>
