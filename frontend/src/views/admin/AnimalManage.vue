<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';

const animals = ref<any[]>([]);

const fetchAnimals = async () => {
  try {
    const res = await axios.get('/api/admin/animals');
    animals.value = res.data;
  } catch (e) {
    console.error(e);
  }
};

const deleteAnimal = async (id: number) => {
  if (confirm('确定要删除这个动物档案吗？相关足迹也会失效！')) {
    try {
      await axios.delete(`/api/admin/animals/${id}`);
      fetchAnimals();
    } catch (e) {
      alert('删除失败');
    }
  }
};

onMounted(() => {
  fetchAnimals();
});
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>动物档案管理</h2>
      <p class="subtitle">管理并维护所有已建档的流浪动物信息</p>
    </div>

    <div class="table-container">
      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>昵称</th>
            <th>品种</th>
            <th>唯一码</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="animal in animals" :key="animal.id">
            <td class="col-id">#{{ animal.id }}</td>
            <td class="col-name">
              <div class="animal-name">
                <img v-if="animal.avatarUrl" :src="animal.avatarUrl" class="animal-avatar" />
                <span v-else class="avatar-placeholder">🐾</span>
                {{ animal.name }}
              </div>
            </td>
            <td><span class="badge">{{ animal.breed }}</span></td>
            <td class="col-code"><code>{{ animal.qrCodeId }}</code></td>
            <td class="col-time">{{ new Date(animal.createdAt).toLocaleString() }}</td>
            <td>
              <button class="btn-danger" @click="deleteAnimal(animal.id)">下线处理</button>
            </td>
          </tr>
          <tr v-if="animals.length === 0">
            <td colspan="6" class="empty-state">
              <div class="empty-icon">📭</div>
              暂无动物档案数据
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.table-container {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
  overflow: hidden;
  border: 1px solid #f1f5f9;
}

.admin-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.admin-table th, .admin-table td {
  padding: 16px 24px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
}

.admin-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.admin-table tbody tr {
  transition: background-color 0.2s;
}

.admin-table tbody tr:hover {
  background-color: #f8fafc;
}

.col-id {
  color: #94a3b8;
  font-family: monospace;
}

.col-name {
  font-weight: 500;
  color: #0f172a;
}

.animal-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #f1f5f9;
  border-radius: 50%;
  font-size: 14px;
}

.animal-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e2e8f0;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 500;
}

.col-code code {
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 6px;
  color: #475569;
  font-size: 13px;
}

.col-time {
  color: #64748b;
  font-size: 14px;
}

.btn-danger {
  background: white;
  color: #ef4444;
  border: 1px solid #fca5a5;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-danger:hover {
  background: #fef2f2;
  border-color: #ef4444;
}

.empty-state {
  text-align: center;
  padding: 48px !important;
  color: #94a3b8;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.5;
}
</style>
