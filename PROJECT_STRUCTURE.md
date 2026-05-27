# 爪印 (Campus Paw-Track) 项目架构与结构说明书

本项目是一个基于校园地理信息系统 (GIS) 的社区众包流浪动物足迹追踪平台。系统通过前端高德地图实现地图取点与定位，结合 Google Gemini AI 进行行为预测与第一人称成长日记生成，并对接后端 Spring Boot 与 MySQL 空间索引进行点位数据与反馈链路的全量持久化。

---

## 1. 项目目录树结构

本项目的根目录下主要分为 `backend`（后端）与 `frontend`（前端）两个独立的工程：

```text
f:\AISai
├─ backend/                        # 后端 Spring Boot Maven 工程
│  ├─ src/
│  │  └─ main/
│  │     ├─ java/com/pawtrack/
│  │     │  ├─ analysis/           # AI 行为推理与大模型适配层
│  │     │  │  ├─ GeminiProvider.java   # Gemini API 交互实现（使用 gemini-flash-latest）
│  │     │  │  ├─ IAIProvider.java       # AI 适配接口声明
│  │     │  │  └─ AnalysisService.java   # 行为推演及 Few-shot 纠偏反馈核心逻辑
│  │     │  ├─ config/             # Spring MVC 配置层
│  │     │  │  └─ WebMvcConfig.java      # 静态图片外部映射配置
│  │     │  ├─ controller/         # API 控制器路由层
│  │     │  │  ├─ AnimalController.java
│  │     │  │  ├─ LocationLogController.java
│  │     │  │  ├─ AnalysisController.java # 反馈提交、推理获取、日记获取接口
│  │     │  │  └─ UploadController.java   # 本地拍照图片上传接口
│  │     │  ├─ entity/             # 数据库实体类 (JPA Entities) 与数据传输对象 (DTOs)
│  │     │  │  ├─ Animal.java            # 小动物档案实体 (animals 表)
│  │     │  │  ├─ LocationLog.java       # 足迹空间日志实体 (animal_logs 表)
│  │     │  │  ├─ BehaviorTag.java       # 行为枚举 (EATING, SLEEPING 等)
│  │     │  │  ├─ PredictionFeedback.java# AI 预测确认/纠偏记录表 (prediction_feedbacks 表)
│  │     │  │  ├─ AnimalLifeNarrative.java# AI 撰写的小生命成长故事日记表 (animal_life_narratives 表)
│  │     │  │  ├─ FeedbackRequest.java   # 纠偏反馈数据传输对象 (DTO)
│  │     │  │  └─ LocationLogCreateRequest.java # 新增足迹请求数据传输对象 (DTO)
│  │     │  ├─ repository/         # 数据库仓储接口 (Spring Data JPA)
│  │     │  │  ├─ AnimalRepository.java
│  │     │  │  ├─ LocationLogRepository.java # 含米单位的空间拓扑查询
│  │     │  │  ├─ PredictionFeedbackRepository.java
│  │     │  │  └─ AnimalLifeNarrativeRepository.java
│  │     │  ├─ service/            # 核心业务逻辑服务层
│  │     │  │  ├─ AnimalService.java
│  │     │  │  └─ LocationLogService.java# 含 `@Async` 异步成长故事生成与唯一ID自动分配逻辑
│  │     │  └─ PawTrackApplication.java # Spring Boot 启动类 (包含种子数据初始化)
│  │     └─ resources/
│  │        ├─ static/
│  │        │  └─ images/          # 后端图片 UI 素材文件夹 (用于提供后端实体图片服务)
│  │        │     ├─ cat_avatar.png
│  │        │     ├─ dog_avatar.png
│  │        │     └─ other_avatar.png
│  │        └─ application.yml     # 后端数据库连接与 AI Key 配置文件
│  └─ pom.xml                      # Maven 依赖配置文件
│
├─ frontend/                       # 前端 Vite + Vue 3 + TypeScript 工程
│  ├─ public/                      # 静态资源公开文件夹
│  │  ├─ images/                   # 前端图片 UI 素材文件夹 (提供本地备用头像与拉伸占位)
│  │  │  ├─ cat_avatar.png
│  │  │  ├─ dog_avatar.png
│  │  │  └─ other_avatar.png
│  │  ├─ favicon.svg
│  │  └─ icons.svg
│  ├─ src/
│  │  ├─ assets/                   # 前端静态 UI 资产目录
│  │  │  ├─ hero.png
│  │  │  ├─ vite.svg
│  │  │  └─ vue.svg
│  │  ├─ components/
│  │  │  └─ MapContainer.vue       # 高德地图核心交互、响应式表单录入及优化的肉垫 FAB 定位按钮
│  │  ├─ App.vue                   # 页面主布局 (包含 AI 弹窗、日记便签、底部图鉴与轨迹时间轴)
│  │  ├─ main.ts                   # 前端系统入口，高德地图 SDK 初始化与插件注册
│  │  └─ style.css                 # 全局基础样式
│  ├─ index.html                   # 前端主入口 HTML5 文件
│  ├─ package.json                 # Node.js 项目依赖及运行指令配置文件
│  └─ vite.config.ts               # Vite 编译与打包配置文件
│
├─ README.md                       # 项目运行指南与技术栈介绍
├─ CLAUDE.md                       # 开发规范与常用操作指令说明
├─ PROJECT_STRUCTURE.md            # 项目结构与架构说明书 (当前文件)
└─ ZY.sql                          # MySQL 数据库表结构与当前数据完整导出脚本 (含全量表及初始化记录)
```

---

## 2. 核心文件夹与关键文件作用

### 2.1 后端项目 (backend)

| 路径/文件 | 类型 | 作用描述 |
| :--- | :--- | :--- |
| `PawTrackApplication.java` | 文件 | 程序的启动主类。在系统首次运行且检测到数据库空置时，会自动生成测试种子数据，实现冷启动填充。 |
| `entity/Animal.java` | 实体 | 映射 MySQL 的 `animals` 表。存储建档动物的档案信息（姓名、品种/大类、自动生成的唯一标识等）。 |
| `entity/LocationLog.java` | 实体 | 映射 MySQL 的 `animal_logs` 表。保存流浪动物的足迹日志。内含经纬度坐标（通过 `@Formula` 动态以 `ST_Latitude` / `ST_Longitude` 实时将 MySQL POINT 地理空间数据解析为 Double 数值）、发现时间、照片链接、行为枚举及特征文本。 |
| `entity/PredictionFeedback.java` | 实体 | 映射 MySQL 的 `prediction_feedbacks` 表。用于保存用户对 AI 推理结果做出的确认（`CONFIRMED`）或人工纠偏纠错（`CORRECTED`）记录。 |
| `entity/AnimalLifeNarrative.java` | 实体 | 映射 MySQL 的 `animal_life_narratives` 表。持久化保存 AI 根据此小动物全部足迹产生的成长日记文本。 |
| `entity/LocationLogCreateRequest.java` | DTO | 新增足迹记录的请求传输对象，封装前端上传的动物种类、昵称、特征描述、经纬度坐标、照片 URL、选定行为标签和时间偏移量。 |
| `entity/FeedbackRequest.java` | DTO | 纠正/确认 AI 预测行为的反馈请求传输对象，包含动物 ID、预测行为、真实行为及反馈类型。 |
| `analysis/AnalysisService.java` | 服务 | 负责对特定动物的 7 天轨迹进行分析推断。并提取历史用户纠偏反馈，通过 In-context Few-shot 自我重训对 AI 推导的权重进行修正。 |
| `config/WebMvcConfig.java` | 配置 | 注册自定义静态资源处理器，将 `/images/unclassified/**` 路径映射至外部磁盘目录，使上传的图片即时可见。 |
| `controller/AnalysisController.java` | 路由 | 提供 AI 行为预测 `/behavior/{id}`、反馈收集 `/feedback`、成长日记拉取 `/narrative/{id}` 接口。 |
| `controller/UploadController.java` | 路由 | 提供文件上传接口 `/api/upload`，处理本地拍照及文件图片接收并保存至 `static/images/unclassified`。 |
| `resources/static/images/` | 文件夹 | **后端实体图片库**，存放默认头像图片。Spring Boot 在 `8080` 端口直接向前端提供静态图片拉取服务，支持自定义特定昵称头像（`大橘_avatar.png`）的物理命名调用。 |
| `ZY.sql` | 脚本 | MySQL 数据库结构与初始化数据导出脚本，创建 `animals`、`animal_logs`、`ai_predictions`、`prediction_feedbacks` 和 `animal_life_narratives` 等全表结构及记录。 |

### 2.2 前端项目 (frontend)

| 路径/文件 | 类型 | 作用描述 |
| :--- | :--- | :--- |
| `src/App.vue` | 组件 | 页面的主架构。包括：<br>1. **AI 行为推理模态框**：展现 Gemini 模型分析的当前行为与推演逻辑。提供准确认同与纠偏按钮。<br>2. **小动物成长日记便签**：温馨的 physical paper 风格，自动渲染以第一人称撰写的成长日记。<br>3. **动物图鉴图册网格**：置于页面最下方，陈列全量建档动物。卡片左上角显示昵称，右上角标识类别，中央为拉伸适配的图片。<br>4. **行为轨迹时间轴模态框**：点击图鉴卡片调起，抓取该动物全量生存记录并用精美时间轴展示（含时间、行为徽章及现场照片）。 |
| `components/MapContainer.vue` | 组件 | **高德地图核心组件**，其主要业务功能包括：<br>1. **用户当前位置展示**：在用户 GPS 坐标处绘制一个亮蓝色的中心点，并添加外层水波纹脉冲淡出动画（`pulse-ring`）。<br>2. **手动地图取点与二次点击记录**：点击地图空白处会生成一个绿色的虚线定位针（带有 `bounce-marker` 弹性动画及 “在这发现了...” 浮动气泡），再次点击即可弹出记录表单。<br>3. **响应式玻璃表单模态框**：通过 flex-direction 与 max-height 实现小屏幕自适应，包含照片、昵称、特征、行为状态和发生时间等，去掉繁琐的扫码二维码 ID。<br>4. **点位精细高亮显示**：猫类显示猫头 SVG、狗类显示狗头 SVG，并根据中心点坐标偏移进行精准锚定。<br>5. **自动定位与静默轮询**：初始化后自动定位，并维持 5 秒周期的静默拉取更新，保持地图点位实时同步。 |
| `public/images/` | 文件夹 | 前端本地静态图片素材夹，存储 `cat_avatar.png` 等本地镜像资源以做兜底。 |
| `src/assets/` | 文件夹 | 前端静态 UI 资产目录，存放系统标志、横幅图等未打包静态资源。 |

---

## 3. 系统模块交互与数据流向

### 3.1 用户录入足迹及 AI 日记异步生成的数据流通路 (用户点击取点 -> 二次确认 -> 异步生成日记)

```mermaid
sequenceDiagram
    actor User as 用户
    participant FE_Map as 前端地图 (MapContainer.vue)
    participant BE_Ctrl as 后端路由 (LocationLogController)
    participant BE_Serv as 核心服务 (LocationLogService)
    participant Gemini as 谷歌 AI (Gemini Provider)
    database MySQL as 空间数据库 (MySQL 8.0)

    User->>FE_Map: 1. 点击地图空白处
    FE_Map->>FE_Map: 在点击处渲染临时绿色定位针 (tempMarker)
    User->>FE_Map: 2. 再次点击绿色定位针
    FE_Map->>User: 弹出 "在这发现了..." 记录面板 (移除二维码录入)
    User->>FE_Map: 3. 填写特征描述并点击“标记”提交
    FE_Map->>FE_Map: 抹除地图上的临时定位针 (tempMarker = null)
    FE_Map->>BE_Ctrl: 4. POST 提交 DTO JSON 请求数据
    BE_Ctrl->>BE_Serv: 5. 转发给服务层
    
    rect rgb(240, 248, 240)
        Note over BE_Serv: 业务内部物理保存
        BE_Serv->>MySQL: INSERT 原生 SQL 空间几何数据 (POINT)
        Note over BE_Serv: 自动为新建动物分配唯一标识: {种类}-{新生成的ID}
        BE_Serv->>MySQL: 保存/更新 动物 qr_code_id
        MySQL-->>BE_Serv: 写入足迹与小生命档案成功
    end
    
    BE_Serv-->>BE_Ctrl: 返回生成的 LocationLog 实体
    BE_Ctrl-->>FE_Map: 返回 200 OK 成功响应并刷新地图
    
    rect rgb(240, 240, 250)
        Note over BE_Serv: 后台异步独立协程 (CompletableFuture)
        BE_Serv->>MySQL: 读取该小动物历史所有的足迹序列
        BE_Serv->>Gemini: 携带足迹列表请求成长故事 (指定第一人称)
        Gemini-->>BE_Serv: 返回幽默趣味、温暖的第一人称日记
        BE_Serv->>MySQL: 保存日记至 animal_life_narratives
    end
```

### 3.2 AI 行为推理与 Few-shot 学习纠偏反馈通路

```mermaid
sequenceDiagram
    actor User as 用户
    participant FE_App as 前端 UI (App.vue)
    participant BE_Ctrl as 后端路由 (AnalysisController)
    participant BE_Serv as 核心服务 (AnalysisService)
    participant Gemini as 谷歌 AI (Gemini Provider)
    database MySQL as 空间数据库 (MySQL 8.0)

    User->>FE_App: 1. 点击“AI 行为推理”按钮
    FE_App->>BE_Ctrl: GET 请求 `/api/analysis/behavior/{animalId}`
    BE_Ctrl->>BE_Serv: 调用推演逻辑
    BE_Serv->>MySQL: 2. 获取该动物最近 7 天内轨迹数据
    BE_Serv->>MySQL: 3. 获取该动物历史上被用户“修正/纠偏”的数据记录 (CORRECTED)
    
    rect rgb(255, 248, 240)
        Note over BE_Serv: Prompt Few-shot 拼接
        Note over BE_Serv: 携带历史纠偏日志，使 AI 能够根据以往判定失误自适应修正预测权重
    end
    
    BE_Serv->>Gemini: 4. 传输上下文并向大模型索要推断与逻辑
    Gemini-->>BE_Serv: 返回当前时间最可能的行为状态与逻辑文本
    BE_Serv-->>BE_Ctrl: 封装推理字符串
    BE_Ctrl-->>FE_App: 返回推理结果并在模态框显示
    
    alt 预测准确
        User->>FE_App: 点击“预测得真准！🐾”
        FE_App->>BE_Ctrl: POST 反馈请求 (Type: CONFIRMED)
        BE_Ctrl->>MySQL: 反馈存表归档
    else 预测有偏差
        User->>FE_App: 选择真实状态并点击“确认修正”
        FE_App->>BE_Ctrl: POST 纠偏请求 (Type: CORRECTED, actualBehavior)
        BE_Ctrl->>MySQL: 反馈存表 (作为下次 AI 推理的 Few-shot 上下文)
    end
```

### 3.3 动物图鉴底部隔离栏与行为轨迹数据流

```mermaid
graph TD
    %% 图鉴加载
    OnMount[进入网页] -->|1. 自动触发| fetchAnimals[fetchAnimals 函数]
    fetchAnimals -->|2. GET /api/animals| AnimalAPI[获取全部已建档动物]
    AnimalAPI -->|3. 返回 animals 数组| RenderGrid[v-for 渲染底部 directory-grid 网格]
    
    %% 图像加载
    RenderGrid -->|4. 加载图片 src| ImgSrc[http://localhost:8080/images/昵称_avatar.png]
    ImgSrc -->|5. 加载失败触发 @error| Fallback[handleImageLoadError 函数]
    Fallback -->|6. 降级重定向加载| GenericImg[static/images/种类_avatar.png]
    
    %% 轨迹查看
    RenderGrid -->|7. 点击小动物卡片| ClickCard[openTrajectoryModal 函数]
    ClickCard -->|8. GET /api/locations/animal/{id}| LogsAPI[拉取此动物所有生存日志]
    LogsAPI -->|9. 返回 logs 数组| Timeline[在模态框渲染 Timeline 轨迹时间轴]
    Timeline -->|10. 渲染每个节点信息| ShowDetail[展示时间、行为、描述及现场实拍照]
```

---

## 4. 开发设计原则 (Kaizen & UI-UX Pro Max)

本项目的代码组织结构贯彻了以下设计哲学：

1. **前后端职责单一 (SOC)**：
   * 前端仅仅关注于高精度的 GIS 点位采集、防重复提交状态机（Poka-Yoke）、以及令人眼前一亮的微动画交互；
   * 后端仅关注于地理数据安全入库、AI 语义判定、高维度的关系映射查重，不干涉前端地图渲染参数。
2. **多分辨率自适应 (Responsive Glassmorphism)**：
   * 针对录入表单和推理大脑模态框，采用了 Flex 布局与 `max-height: 90vh`，将超高分辨率或超低分辨率（如移动设备横屏、老旧手机等）的展示适配进行了彻底重构。
   * 表单内部的 `.modal-body` 容器默认支持独立纵向滚动，确保不会在大屏/小屏中出现显示截断或无法触达底部提交按钮的适配死角。
3. **Poka-Yoke (防错) 与 44x44px 触控法则**：
   * 触控目标的最小物理响应热区严格遵守 `44px` 规格（例如小屏上的行为选择器改为双列宽触点，以拓宽有效触点响应高度），降低误触率。
   * 当用户未输入任何小动物的昵称与特征时，置灰并拦截提交，实时在输入框下方输出轻柔的校验文字，规避粗暴的原生 `alert` 弹窗对用户沉浸式心智的破坏。
4. **自适应拉伸与智能回退 (Cover & Fallback)**：
   * 所有头像及现场照片均通过 `object-fit: cover` 消除宽高拉伸形变，确保图鉴的精美度。
   * 通过多层 API 图片回退机制设计，将自定义实拍图与默认插图无缝咬合，极大降低了静态资产维护的物理成本。
5. **系统唯一标识自动维护 (Autonomous Entity Identifiers)**：
   * 彻底摒弃了繁琐的手工二维码 ID 输入，完全托付给数据库自增主键。小生命被初次目击并建立档案时，系统以其 `type` (大类) 搭配生成的实体 `id` 自动形成全局唯一识别标识符（例如 `Cat-6`、`Dog-12`），既简化了用户录入步骤，又确保了实体标识的健壮唯一性。
