# Campus Paw-Track 校园流浪动物足迹追踪系统

Campus Paw-Track 是一个基于校园地理信息系统 (GIS) 的社区众包追踪平台。旨在通过学生的实时足迹记录，结合 Google Gemini AI 行为分析与分类，构建校园流浪动物的行为轨迹图与健康状态模型，提升校园动物福利。

---

## 1. 项目架构与技术栈

项目采用前后端分离的经典全栈架构开发：

- **前端 (Frontend)**:
  - **核心框架**: Vue 3 + TypeScript + Vite
  - **地图服务**: 高德地图 JS API 及 `@vuemap/vue-amap` 组件库
  - **UI 与美学**: 自定义 CSS (有机自然设计风格) + 全量原生 SVG 矢量图标 (No Emojis)
- **后端 (Backend)**:
  - **核心框架**: Spring Boot 3.2.5 (Java 17)
  - **数据持久化**: Spring Data JPA + Hibernate
  - **人工智能**: Google Gemini AI (进行状态特征分类与历史轨迹行为推演)
- **数据库 (Database)**:
  - **存储服务**: MySQL 8.0 (利用空间数据类型 `POINT` 及其空间索引实现高性能距离搜索)

---

## 2. 目录结构

```text
f:/AISai/
├── backend/                   # Spring Boot 后端工程
│   ├── src/main/java/         # Java 源码
│   └── src/main/resources/    # 配置文件 (application.yml)
├── frontend/                  # Vue 3 前端工程
│   ├── src/components/        # UI 组件 (MapContainer.vue)
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
   在 MySQL 环境中（无论是 Windows 的 Navicat 还是 Linux 的命令行），执行项目根目录下的 `ZY.sql` 文件：
   ```sql
   CREATE DATABASE IF NOT EXISTS campus_paw_track DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE campus_paw_track;
   SOURCE /绝对路径/ZY.sql;
   ```

3. **配置修改**:
   打开 `backend/src/main/resources/application.yml`，根据您的实际环境修改以下配置：
   - `spring.datasource.url`: 数据库连接地址
   - `spring.datasource.username` / `password`: MySQL 账号密码
   - `ai.gemini.api-key`: Gemini AI 接口密钥
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
在 Linux 终端中执行以下命令进行打包并后台运行：
```bash
cd backend

# 赋予 Maven Wrapper 执行权限
chmod +x mvnw

# 编译并打包 (跳过测试)
./mvnw clean package -DskipTests

# 运行打包好的 Jar 包 (内置 Tomcat)，并挂载后台
nohup java -jar target/backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

**2. 前端打包与 Nginx 部署**:
```bash
cd frontend
# 安装依赖并构建静态文件
npm install
npm run build
```
构建完成后，将 `frontend/dist` 目录下的所有静态文件复制到 Nginx 的网页根目录（如 `/usr/share/nginx/html`）。
在 Nginx 配置中设置反向代理，将 `/api` 请求转发至后端的 `8080` 端口：
```nginx
location /api/ {
    proxy_pass http://localhost:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 4. 核心接口说明 (API)

项目包含以下核心业务接口，所有接口基础路径为 `http://localhost:8080/api`：

### 4.1 动物档案与足迹接口
- `GET /animals`
  **获取档案列表**。返回所有小动物的基本信息、昵称、特征、AI总结及代表性头像。
- `GET /locations/all`
  **获取全量足迹**。用于主页地图渲染。
- `GET /locations/animal/{animalId}`
  **获取特定动物轨迹**。按时间排序，用于在地图上绘制连线与生存轨迹。
- `POST /locations`
  **上传新足迹**。接收包含经纬度、特征描述的 JSON 数据。系统会在后台触发 AI 自动特征融合及行为推演。

### 4.2 AI 分析与交互接口
- `GET /analysis/narrative/{animalId}`
  **获取 AI 生活日记**。基于动物近期活动轨迹，由大模型生成的拟人化短篇日记。
- `GET /analysis/behavior/{animalId}`
  **AI 行为预测大脑**。结合历史足迹序列与环境信息，推演动物当前最可能的状态。
- `POST /analysis/feedback`
  **人工纠偏反馈**。若 AI 预测偏差，用户可提交实际行为状态，系统将其记录至 `prediction_feedbacks` 库供后续模型权重调优。

### 4.3 评论留言与评分系统 (UGC)
- `POST /reviews/ratings`
  **提交评分**。对小动物的颜值、脾气、可见度等指标进行 1-10 分打分。
- `GET /reviews/ratings/{animalId}/stats`
  **获取评分统计**。计算并返回各项指标的平均分及总评分人数。
- `POST /reviews/comments`
  **发布留言**。支持游客身份及登录会员身份提交。
- `POST /reviews/comments/{commentId}/like`
  **点赞留言**。附带防刷机制的点赞/取消点赞操作。

---

## 5. 核心功能及特点

1. **地图足迹渲染**：对接高德地图，自动展示范围内所有被记录小动物的点位标记。
2. **侧边快速定位**：地图右下角配备雷达准星按钮，点击后可以使地图瞬间居中定位至当前物理位置。
3. **悬浮爪印 FAB**：点击底部浮动的爪印大按钮，即可获取当前地理位置并弹出玻璃卡片式记录表单。
4. **AI 行为自动分类与图片评选**：Gemini 智能识别文字特征，Qwen-VL-Max 根据上传照片自动甄选最佳头像。
5. **完全矢量化设计**：页面整体遵循“有机自然”美学设计，去除所有系统默认 Emoji 字符，采用统一风格的矢量 SVG，动效平滑优雅。
