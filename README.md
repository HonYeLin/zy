# Campus Paw-Track 校园流浪动物足迹追踪系统

Campus Paw-Track 是一个基于校园地理信息系统 (GIS) 的社区众包追踪平台。旨在通过学生的实时足迹记录，结合最新的 DeepSeek 与 Qwen 大模型进行行为分析与特征提取，构建校园流浪动物的行为轨迹图与健康状态模型，提升校园动物福利。

---

## 1. 项目架构与技术栈

项目采用前后端分离的经典全栈架构开发：

- **前端 (Frontend)**:
  - **核心框架**: Vue 3 + TypeScript + Vite
  - **地图服务**: 高德地图 JS API 及 `@vuemap/vue-amap` 组件库
  - **UI 与美学**: 自定义 CSS (高毛玻璃质感、手账日记本风格、有机自然设计) + 全量原生 SVG 矢量图标
- **后端 (Backend)**:
  - **核心框架**: Spring Boot 3.2.5 (Java 17)
  - **数据持久化**: Spring Data JPA + Hibernate
  - **人工智能**: 
    - 文本推理：DeepSeek 大语言模型 (进行状态特征分类、历史轨迹行为推演与拟人化日记生成)
    - 视觉模型：阿里云通义千问 Qwen-VL-Max (进行图片智能识别与非动物脏数据过滤)
- **数据库 (Database)**:
  - **存储服务**: MySQL 8.0 (利用空间数据类型 `POINT` 及其空间索引实现高性能距离搜索)

---

## 2. 目录结构

```text
f:/AISai/
├── backend/                   # Spring Boot 后端工程
│   ├── src/main/java/         # Java 源码 (包含 Controllers, Services, Entities)
│   └── src/main/resources/    # 配置文件 (application.yml)
├── frontend/                  # Vue 3 前端工程
│   ├── src/components/        # UI 组件 (MapContainer.vue, AnimalReview.vue 等)
│   ├── src/views/             # 视图页面 (HomeView.vue, AdminDashboard.vue 等)
│   ├── src/App.vue            # 主页面
│   └── src/style.css          # 全局样式
├── ZY.sql                     # 数据库初始化及测试数据脚本
└── README.md                  # 项目说明文档
```

---

## 3. 详细部署步骤 (Windows / Linux)

### 3.1 数据库与环境准备 (Win/Linux 通用)

1. **环境依赖**:
   - **Java**: JDK 17
   - **Node.js**: v18+ 及 npm
   - **数据库**: MySQL 8.0+
   - **Maven**: 3.8+ (可选，后端已自带 `mvnw` wrapper)

2. **数据库初始化**:
   在 MySQL 环境中，执行项目根目录下的 `ZY.sql` 文件：
   ```sql
   CREATE DATABASE IF NOT EXISTS campus_paw_track DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE campus_paw_track;
   SOURCE /绝对路径/ZY.sql;
   ```

3. **配置修改**:
   打开 `backend/src/main/resources/application.yml`，根据您的实际环境修改以下配置：
   - `spring.datasource.url`: 数据库连接地址
   - `spring.datasource.username` / `password`: MySQL 账号密码
   - `ai.deepseek.api-key`: DeepSeek 接口密钥
   - `ai.dashscope.api-key`: 阿里云通义千问 (Qwen-VL) 接口密钥

---

### 3.2 Windows 本地开发与测试部署

**前端运行**:
1. 打开 PowerShell 或 CMD，进入前端目录：
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
2. 浏览器访问 `http://localhost:5173`。

**后端运行**:
1. 进入后端目录：
   ```powershell
   cd backend
   .\mvnw spring-boot:run
   ```
2. 或者直接在 IntelliJ IDEA 中打开 `backend` 目录，运行 `PawTrackApplication.java`。后端默认监听 `8080` 端口。

---

### 3.3 Linux 服务器生产环境部署

**1. 后端打包与后台运行**:
```bash
cd backend
chmod +x mvnw
./mvnw clean package -DskipTests
nohup java -jar target/backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

**2. 前端打包与 Nginx 部署**:
```bash
cd frontend
npm install
npm run build
```
将 `frontend/dist` 目录下的静态文件复制到 Nginx 网页根目录。
配置反向代理，将 `/api` 请求转发至后端的 `8080` 端口：
```nginx
location /api/ {
    proxy_pass http://localhost:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 4. 核心接口说明 (API)

所有接口基础路径为 `http://localhost:8080/api`：

### 4.1 动物档案、上传与足迹接口
- `GET /animals`：获取档案列表。
- `POST /upload`：**图片智能上传验证**。在落盘前调用大模型验证是否包含小动物，过滤风景等无关照片。
- `GET /locations/all`：获取全量足迹用于地图渲染。
- `GET /locations/animal/{animalId}`：获取特定动物轨迹。
- `POST /locations`：上传新足迹。后台异步触发 AI 生成叙事与状态总结。

### 4.2 AI 分析与交互接口
- `GET /analysis/narrative/{animalId}`：**AI 生活日记**。
- `GET /analysis/behavior/{animalId}`：**AI 行为预测大脑**。结合历史足迹推演当前状态。
- `POST /analysis/feedback`：人工纠偏反馈，记录用户修正用于 AI 模型参考。

### 4.3 评论留言与超级管理员 (Admin) 系统
- `POST /reviews/ratings` & `POST /reviews/comments`：提交评分与发布留言。
- `POST /admin/auth/login`：超级管理员登录验证。
- `GET /admin/animals` & `DELETE /admin/animals/{id}`：管理员统一管控动物档案与异常数据清理。

---

## 5. 核心功能及特色亮点

1. **AI 智能校验与脏数据过滤**：在图片上传阶段引入 Qwen-VL-Max 进行视觉验证，拦截非动物类照片（如绿植、风景、自拍），拒绝污染数据库。
2. **多模型协同的 AI 推理大脑**：图像特征由 Qwen 提取，行为时空推理及日记写作由 DeepSeek 负责，实现最优的“视觉+推理”混合模型架构。
3. **沉浸式手账 UI 体验**：全站采用高毛玻璃质感、拍立得相纸卡片、手账式留言板的设计美学，取代生硬的数据报表展示。
4. **全自动防并发竞态保护**：对高并发情况下的数据库读写、异步 AI 生成引入 `TransactionSynchronizationManager` 提交后钩子，保证了数据存储与提取的绝对安全一致。
5. **超级管理中枢**：内置独立的管理员面板，方便后期集中管理所有流浪动物档案及纠正不良信息。
