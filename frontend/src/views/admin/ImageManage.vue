<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';

const images = ref<any[]>([]);

const fetchImages = async () => {
  try {
    const res = await axios.get('/api/admin/images');
    images.value = res.data;
  } catch (e) {
    console.error(e);
  }
};

const deleteImage = async (filename: string) => {
  if (confirm('确定要从服务器物理删除这张图片吗？此操作不可逆！')) {
    try {
      await axios.delete(`/api/admin/images/${filename}`);
      fetchImages();
    } catch (e) {
      alert('删除失败');
    }
  }
};

const formatSize = (bytesStr: string) => {
  const bytes = parseInt(bytesStr);
  if (isNaN(bytes)) return '未知大小';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

onMounted(() => {
  fetchImages();
});
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>相册与图片素材管理</h2>
      <p class="subtitle">管理全站上传的图片资源，清理无用素材释放空间</p>
    </div>

    <div class="image-grid">
      <div v-for="img in images" :key="img.filename" class="image-card">
        <div class="img-wrapper">
          <img :src="img.url" :alt="img.filename" loading="lazy" />
          <div class="img-overlay">
            <button class="btn-delete-img" @click="deleteImage(img.filename)" title="物理删除">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            </button>
          </div>
        </div>
        <div class="img-info">
          <div class="filename" :title="img.filename">{{ img.filename }}</div>
          <div class="size">{{ formatSize(img.size) }}</div>
        </div>
      </div>
      
      <div v-if="images.length === 0" class="empty-state">
        <div class="empty-icon">🖼️</div>
        <p>后端暂未扫描到用户上传的图片素材</p>
      </div>
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

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

.image-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
  transition: transform 0.2s, box-shadow 0.2s;
}

.image-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05);
}

.img-wrapper {
  position: relative;
  height: 180px;
  background: #f1f5f9;
  overflow: hidden;
}

.img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.image-card:hover .img-wrapper img {
  transform: scale(1.05);
}

.img-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  opacity: 0;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-card:hover .img-overlay {
  opacity: 1;
}

.btn-delete-img {
  background: #ef4444;
  color: white;
  border: none;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transform: translateY(20px);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.btn-delete-img svg {
  width: 20px;
  height: 20px;
}

.image-card:hover .btn-delete-img {
  transform: translateY(0);
}

.btn-delete-img:hover {
  background: #dc2626;
  transform: scale(1.1) !important;
}

.img-info {
  padding: 16px;
  background: white;
  position: relative;
  z-index: 1;
}

.filename {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.size {
  font-size: 13px;
  color: #64748b;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 64px 20px;
  background: white;
  border-radius: 16px;
  border: 1px dashed #cbd5e1;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}
</style>
