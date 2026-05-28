<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';

const comments = ref<any[]>([]);

const fetchComments = async () => {
  try {
    const res = await axios.get('/api/admin/comments');
    comments.value = res.data;
  } catch (e) {
    console.error(e);
  }
};

const deleteComment = async (id: number) => {
  if (confirm('确定要删除这条留言吗？')) {
    try {
      await axios.delete(`/api/admin/comments/${id}`);
      fetchComments();
    } catch (e) {
      alert('删除失败');
    }
  }
};

onMounted(() => {
  fetchComments();
});
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>留言板管理</h2>
      <p class="subtitle">管理并审核用户的全网留言，维护良好的社区环境</p>
    </div>

    <div class="table-container">
      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>发布人</th>
            <th>留言内容</th>
            <th>获赞数</th>
            <th>发布时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="comment in comments" :key="comment.id">
            <td class="col-id">#{{ comment.id }}</td>
            <td class="col-user">
              <div class="user-info">
                <span class="user-avatar">👤</span>
                {{ comment.userNickname }}
              </div>
            </td>
            <td>
              <div class="content-cell" :title="comment.content">
                {{ comment.content }}
              </div>
            </td>
            <td>
              <span class="like-badge">
                <span class="heart">❤️</span> {{ comment.likeCount }}
              </span>
            </td>
            <td class="col-time">{{ new Date(comment.createdAt).toLocaleString() }}</td>
            <td>
              <button class="btn-danger" @click="deleteComment(comment.id)">删除违规</button>
            </td>
          </tr>
          <tr v-if="comments.length === 0">
            <td colspan="6" class="empty-state">
              <div class="empty-icon">📝</div>
              暂无留言数据
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
  table-layout: fixed;
}

.admin-table th, .admin-table td {
  padding: 16px 24px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
}

.admin-table th:nth-child(1) { width: 80px; }
.admin-table th:nth-child(2) { width: 160px; }
.admin-table th:nth-child(3) { width: auto; }
.admin-table th:nth-child(4) { width: 100px; }
.admin-table th:nth-child(5) { width: 180px; }
.admin-table th:nth-child(6) { width: 120px; }

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

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #0f172a;
}

.user-avatar {
  background: #e2e8f0;
  border-radius: 50%;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.content-cell {
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 6px;
  color: #334155;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: default;
  border: 1px solid #e2e8f0;
}

.like-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #fef2f2;
  color: #ef4444;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
}

.heart {
  font-size: 12px;
}

.col-time {
  color: #64748b;
  font-size: 13px;
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
