<template>
  <div class="animal-review-container">
    <!-- Close button for the details panel -->
    <button class="close-review-btn" @click="$emit('close')" title="关闭面板">×</button>
    <div class="review-top-row">
      <!-- 左侧：图鉴样式 -->
      <div class="encyclopedia-side">
        <div class="encyclopedia-card">
          <img v-if="animal.avatarUrl" :src="animal.avatarUrl.replace('http://localhost:8080', '')" class="animal-avatar" alt="avatar" />
          <div class="animal-info">
            <h3 class="animal-name">{{ animal.name }}</h3>
            <!-- 去掉 breed 品种显示 -->
            <p class="animal-desc">{{ animal.aiSummary || animal.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>

      <!-- 右侧：打分选项与展示 -->
      <div class="rating-side">
        <div class="rating-box-inner">
          <h4 class="rating-title">给 {{ animal.name }} 打分</h4>
          
          <div class="rating-grid">
            <div class="rating-item" v-for="metric in metrics" :key="metric.key">
              <span class="metric-label">{{ metric.label }}</span>
              <div class="stars-input">
                <span 
                  v-for="n in 10" 
                  :key="n" 
                  class="star-half"
                  :class="{ 
                    'active': userRating[metric.key] >= n,
                    'is-right-half': n % 2 === 0,
                    'is-left-half': n % 2 !== 0
                  }"
                  @click="setRating(metric.key, n)"
                ></span>
              </div>
              <span class="metric-score">{{ userRating[metric.key] }} / 10</span>
            </div>
          </div>
          
          <div class="submit-rating-row">
             <button class="submit-rating-btn" @click="submitRating" :disabled="isSubmittingRating">提交评分</button>
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
      <h4 class="comments-title">💬 留言板</h4>
      
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
           <div class="comment-item" v-for="comment in comments" :key="comment.id">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';

const props = defineProps<{
  animal: any;
  currentUser?: any;
}>();

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
  position: relative; /* Added for close button positioning */
  background: rgba(255, 255, 255, 0.85);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
  padding: 24px;
  margin-top: 2.5rem; /* Optimized spacing from the button above */
  margin-bottom: 20px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.review-top-row {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}

/* 图鉴侧 */
.encyclopedia-side {
  flex: 0 0 35%;
  border-right: 1px dashed #ccc;
  padding-right: 20px;
}

.encyclopedia-card {
  text-align: center;
}

.animal-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid #fff;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  margin-bottom: 15px;
}

.animal-name {
  margin: 0 0 15px 0;
  font-size: 1.5em;
  color: #333;
}

.animal-desc {
  font-size: 0.95em;
  color: #555;
  line-height: 1.6;
  text-align: left;
  background: rgba(0, 0, 0, 0.02);
  padding: 12px;
  border-radius: 8px;
  border-left: 3px solid #5eaf66;
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
  color: #333;
  font-size: 1.25em;
  text-align: center;
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
  color: #444;
  font-size: 1.05em;
}

/* 评分星星样式：放大并优化半星 */
.stars-input {
  display: inline-flex;
  position: relative;
  font-size: 32px;
  line-height: 1;
  cursor: pointer;
  user-select: none;
}

.star-half {
  width: 16px;
  overflow: hidden;
  color: #ddd;
  transition: color 0.15s, transform 0.1s;
  display: inline-block;
}

.star-half:hover {
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
  background: #4caf50;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  font-size: 1em;
  transition: background 0.3s, transform 0.1s;
  width: 100%;
  max-width: 200px;
}

.submit-rating-btn:hover {
  background: #43a047;
}

.submit-rating-btn:active {
  transform: scale(0.98);
}

.submit-rating-btn:disabled {
  background: #a5d6a7;
  cursor: not-allowed;
}

.rating-stats {
  margin-top: 20px;
  background: #f9f9f9;
  padding: 12px 18px;
  border-radius: 8px;
  max-width: 360px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  box-sizing: border-box;
  text-align: center;
}

.rating-stats h5 {
  margin: 0 0 10px 0;
  color: #444;
  font-size: 0.95em;
}

.stats-grid {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.stat-item {
  font-size: 0.9em;
  color: #666;
  background: #fff;
  padding: 6px 12px;
  border-radius: 12px;
  border: 1px solid #eee;
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
  border-top: 2px dashed rgba(76, 175, 80, 0.15); /* Soft natural dashed line */
  padding-top: 25px;
  margin-top: 20px;
}

.comments-title {
  margin: 0 0 20px 0;
  color: #1B5E20; /* Deep forest green */
  font-size: 1.3rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.comment-input-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 30px;
  background: rgba(255, 255, 255, 0.5); /* Clear glass card backing */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(76, 175, 80, 0.15);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.02);
}

/* 登录状态栏样式 */
.user-login-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid rgba(76, 175, 80, 0.08);
}

.status-badge {
  color: #2F4F4F;
  font-size: 0.9em;
  font-weight: 600;
}

.login-stub-btn {
  background: #fff;
  border: 1.5px solid rgba(76, 175, 80, 0.3);
  color: #2E7D32;
  padding: 4px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85em;
  font-weight: bold;
  transition: all 0.25s ease;
}

.login-stub-btn:hover {
  background: rgba(76, 175, 80, 0.05);
  border-color: #2E7D32;
  transform: translateY(-1px);
}

.comment-textarea {
  width: 100%;
  height: 100px;
  padding: 14px;
  border: 1.5px solid rgba(76, 175, 80, 0.18);
  border-radius: 12px;
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
  font-size: 0.95rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(255, 255, 255, 0.85);
  color: #2F4F4F;
}

.comment-textarea:focus {
  outline: none;
  border-color: #2E7D32;
  box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.15);
  background: #ffffff;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
}

.submit-comment-btn {
  background: linear-gradient(135deg, #4CAF50, #2E7D32); /* Premium organic green gradient */
  color: white;
  border: none;
  padding: 10px 28px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
  font-size: 0.95rem;
  box-shadow: 0 4px 12px rgba(46, 125, 50, 0.18);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: inherit;
}

.submit-comment-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(46, 125, 50, 0.28);
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

/* 留言列表头部与精美 Pill Tabs 样式 */
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
  color: #2E7D32;
}

.sort-tabs span.active {
  background: white;
  color: #2E7D32;
  font-weight: bold;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* 气泡玻璃卡片式留言项 */
.comment-item {
  border-radius: 18px;
  padding: 18px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.015);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-sizing: border-box;
}

.comment-item:hover {
  transform: translateY(-3px);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 24px rgba(76, 175, 80, 0.08);
  border-color: rgba(76, 175, 80, 0.25);
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
  background: linear-gradient(135deg, #81C784, #388E3C); /* Forest green gradient */
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1rem;
  border: 2px solid white;
  box-shadow: 0 3px 8px rgba(76, 175, 80, 0.18);
}

.user-name {
  font-weight: 700;
  color: #2E7D32; /* Deep theme green */
}

.comment-time {
  font-size: 0.8em;
  color: #888;
}

.comment-content {
  color: #2F4F4F;
  line-height: 1.6;
  margin-bottom: 10px;
  padding-left: 50px; /* Aligns text cleanly to the right of the avatar placeholder */
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
  color: #666;
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
  color: #2E7D32; /* Deep theme green */
  font-weight: bold;
  background: rgba(76, 175, 80, 0.08);
}

/* 优雅暖红的删除按钮 */
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
  border: 1px solid rgba(76, 175, 80, 0.22);
  color: #2E7D32;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
  transition: all 0.25s ease;
  font-family: inherit;
}

.pagination button:hover:not(:disabled) {
  background: rgba(76, 175, 80, 0.06);
  border-color: #2E7D32;
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
  background: #e8f0fe;
  color: #1a73e8;
}

.role-indicator.user {
  background: #e6f4ea;
  color: #137333;
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
    padding: 16px;
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
  }

  .comment-content, .comment-footer {
    padding-left: 0;
    margin-top: 8px;
  }
}
</style>
