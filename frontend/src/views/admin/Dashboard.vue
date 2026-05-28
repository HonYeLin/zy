<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';

const stats = ref({
  animals: 0,
  comments: 0
});

onMounted(async () => {
  try {
    const resA = await axios.get('/api/admin/animals');
    stats.value.animals = resA.data.length;
    const resC = await axios.get('/api/admin/comments');
    stats.value.comments = resC.data.length;
  } catch (e) {
    console.error('获取统计失败', e);
  }
});
</script>

<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h2>数据看板</h2>
      <p class="subtitle">总览校园流浪动物的实时数据情况</p>
    </div>
    
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon animal-icon">
          🐾
        </div>
        <div class="stat-content">
          <h3>建档动物数</h3>
          <div class="val">{{ stats.animals }}</div>
          <div class="trend positive"><span>↑</span> 稳定更新中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon comment-icon">
          💬
        </div>
        <div class="stat-content">
          <h3>全网留言数</h3>
          <div class="val">{{ stats.comments }}</div>
          <div class="trend positive"><span>↑</span> 活跃互动中</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 32px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #0f172a;
  font-weight: 700;
}

.subtitle {
  margin: 0;
  color: #64748b;
  font-size: 15px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
  display: flex;
  align-items: flex-start;
  gap: 20px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.05), 0 4px 6px -2px rgba(0,0,0,0.025);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.animal-icon {
  background: #f0fdf4;
  color: #22c55e;
}

.comment-icon {
  background: #eff6ff;
  color: #3b82f6;
}

.stat-content {
  flex: 1;
}

.stat-content h3 {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.stat-content .val {
  font-size: 36px;
  color: #0f172a;
  font-weight: 700;
  margin: 8px 0;
  line-height: 1;
}

.trend {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.trend.positive {
  color: #10b981;
}

.trend.positive span {
  font-weight: bold;
}
</style>
