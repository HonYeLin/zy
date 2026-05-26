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
└── gemini-code-1779813488083.sql # 数据库初始化脚本
```

---

## 3. 环境搭建与运行指南

### 3.1 数据库初始化
1. 确保本地安装并运行 MySQL 8.0 数据库。
2. 在 MySQL 控制台或 Navicat 等客户端中，运行项目根目录下的 SQL 脚本：
   ```sql
   source f:/AISai/gemini-code-1779813488083.sql;
   ```
   该脚本会自动创建数据库 `campus_paw_track` 并创建 `animals`、`animal_logs` 和 `ai_predictions` 三张数据表。

### 3.2 后端启动 (Spring Boot)
1. 用 IntelliJ IDEA 打开 `backend` 文件夹。
2. 检查 `src/main/resources/application.yml` 中的 MySQL 连接配置，确保用户名和密码正确 (默认为 `root` / `123456`)。
3. 检查 `ai.gemini.api-key` 是否正确配置。
4. 运行 `PawTrackApplication.java` 启动后端服务。服务默认运行在 `http://localhost:8080`。

### 3.3 前端启动 (Vue 3)
1. 打开终端进入 `frontend` 文件夹：
   ```bash
   cd frontend
   ```
2. 安装项目依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 启动后，通过浏览器访问控制台输出的地址 (默认为 `http://localhost:5173` 或 `http://localhost:5174`)。

---

## 4. 核心功能及特点

1. **地图足迹渲染**：对接高德地图，自动展示范围内所有被记录小动物的点位标记。
2. **侧边快速定位**：地图右下角配备雷达准星按钮，点击后可以使地图瞬间居中定位至当前物理位置。
3. **悬浮爪印 FAB**：点击底部浮动的爪印大按钮，即可获取当前地理位置并弹出玻璃卡片式记录表单。
4. **AI 行为自动分类**：在表单中填写描述后，Gemini AI 将根据语义智能识别小动物状态，并自动转换为 `EATING` (吃)、`SLEEPING` (睡)、`PLAYING` (玩) 等格式化标签入库。
5. **完全矢量化设计**：页面整体遵循“有机自然”美学设计，去除所有系统默认 Emoji 字符，采用统一风格的矢量 SVG，动效平滑优雅。
