# 爪印 (Campus Paw-Track) 项目架构与结构说明书

本项目是一个基于校园地理信息系统 (GIS) 的社区众包流浪动物足迹追踪平台。系统通过前端高德地图实现地图取点与定位，结合 Google Gemini AI 行为识别分类，并对接后端 Spring Boot 与 MySQL 空间索引进行点位数据的全量持久化。

---

## 1. 项目目录树结构

本项目的根目录下主要分为 `backend`（后端）与 `frontend`（前端）两个独立的工程：

```text
f:\AISai
├─ backend/                        # 后端 Spring Boot Maven 工程
│  ├─ src/
│  │  └─ main/
│  │     ├─ java/com/pawtrack/
│  │     │  ├─ analysis/           # AI 行为推理与接口适配层
│  │     │  ├─ controller/         # API 控制器层
│  │     │  ├─ entity/             # 数据库实体类 (JPA Entities)
│  │     │  ├─ repository/         # 数据库仓储接口 (Spring Data JPA)
│  │     │  ├─ service/            # 核心业务逻辑服务层
│  │     │  └─ PawTrackApplication.java # Spring Boot 启动类
│  │     └─ resources/
│  │        └─ application.yml     # 后端数据库连接与 AI Key 配置文件
│  └─ pom.xml                      # Maven 依赖配置文件
│
├─ frontend/                       # 前端 Vite + Vue 3 + TypeScript 工程
│  ├─ src/
│  │  ├─ components/
│  │  │  └─ MapContainer.vue       # 高德地图核心交互、表单录入及 UI 动效组件
│  │  ├─ App.vue                   # 页面主布局 (双栏响应式系统/控制台/数据统计)
│  │  ├─ main.ts                   # 前端系统入口，高德地图 SDK 初始化与插件注册
│  │  └─ style.css                 # 全局基础样式
│  ├─ index.html                   # 前端主入口 HTML5 文件
│  ├─ package.json                 # Node.js 项目依赖及运行指令配置文件
│  └─ vite.config.ts               # Vite 编译与打包打包配置文件
│
├─ README.md                       # 项目运行指南与技术栈介绍
├─ CLAUDE.md                       # 开发规范与常用操作指令说明
├─ PROJECT_STRUCTURE.md            # 项目结构与架构说明书 (当前文件)
└─ gemini-code-1779813488083.sql   # MySQL 空间数据库表初始化 SQL 脚本
```

---

## 2. 核心文件夹与关键文件作用

### 2.1 后端项目 (backend)

| 路径/文件 | 类型 | 作用描述 |
| :--- | :--- | :--- |
| `PawTrackApplication.java` | 文件 | 程序的启动主类。在系统首次运行且检测到数据库空置时，会自动生成一条“大橘”测试种子数据，实现冷启动填充。 |
| `entity/Animal.java` | 实体 | 映射 MySQL 的 `animals` 表。存储建档动物的档案信息（姓名、品种/大类、对应线下牌子的扫码 ID）。 |
| `entity/LocationLog.java` | 实体 | 映射 MySQL 的 `animal_logs` 表。保存流浪动物的足迹日志。内含经纬度坐标（通过 `@Formula` 动态以 `ST_Latitude` / `ST_Longitude` 实时将 MySQL POINT 地理空间数据解析为 Double 数值）、发现时间、照片链接、行为枚举及特征文本。 |
| `entity/BehaviorTag.java` | 枚举 | 预设的小动物行为状态枚举，包含 `EATING` (进食/饮水)、`SLEEPING` (睡觉/休息)、`PLAYING` (玩耍)、`SUNBATHING` (晒太阳)、`WALKING` (行走) 和 `OTHER` (其他)。 |
| `analysis/IAIProvider.java` | 接口 | 声明 AI 行为语义分类与历史足迹宏观逻辑推演的方法接口。 |
| `analysis/GeminiProvider.java` | 服务 | 对接 Google Gemini API 的具体实现。负责在用户不填写小动物行为状态时，自动提取“特征描述”，通过 Gemini 语义推理匹配最吻合的行为状态进行智能入库。 |
| `repository/LocationLogRepository.java` | 接口 | 流浪动物足迹数据库访问层。利用原生的 **`ST_Distance_Sphere`** 空间索引函数，实现对于某经纬度中心点一定半径范围（单位：米）内足迹点的高性能空间拓扑圈查。 |
| `service/LocationLogService.java` | 服务 | 核心业务实现。处理数据添加时的业务流转：包括查重逻辑（二维码优先 -> 昵称加品种回退）、Gemini AI 智能状态识别兜底、发现时间的时间偏移量自动校正扣除、以及通过 EntityManager 原生 SQL 方式写入空间 POINT 数据。 |
| `controller/LocationLogController.java` | 路由 | 提供外部 RESTful 路由（支持跨域 CORS `*`）。提供 `/api/locations` 接收 DTO 提交新足迹，提供 `/api/locations/all` 接口供前端地图拉取数据库内的所有记录。 |
| `resources/application.yml` | 配置 | 存储项目环境参数，如 Spring Boot 端口（8080）、数据库用户名密码、MySQL 空间地理连接参数、JPA 自动更新选项以及 Gemini AI 的 API Key。 |

### 2.2 前端项目 (frontend)

| 路径/文件 | 类型 | 作用描述 |
| :--- | :--- | :--- |
| `src/main.ts` | 接口 | 初始化并注入 `@vuemap/vue-amap` API 加载器，配置高德 Web JS API 授权 Key，并注册 `AMap.Geolocation` (定位)、`AMap.Geocoder` (逆地理编码) 等核心地图服务插件。 |
| `src/App.vue` | 组件 | 页面的外骨架。采用治愈的自然绿配色与现代字体。实现了响应式的 **双栏网格布局**：在大显示器上将地图拉伸到 580px 高度作为左侧主面板，右侧集成更多功能按钮、校园守护数据小看板（`stat-card`）、及精美的引导气泡提示；在小屏设备上自动回落降级为单栏垂直滑动流。 |
| `components/MapContainer.vue` | 组件 | **整个项目最核心的组件**，其主要业务功能包括：<br>1. **静默自动定位**：在地图初始化后调用 `AMap.Geolocation` 获取当前 GPS 坐标，并将地图居中（`panTo`）。<br>2. **当前位置展示**：使用 `el-amap-marker` 在用户当前的 GPS 坐标处绘制一个亮蓝色的中心点，并添加外层水波纹脉冲淡出动画（`pulse-ring`）。<br>3. **手动地图取点**：监听 `@click` 事件。点击地图空白处会生成一个绿色的虚线定位针（带有 `bounce-marker` 弹性动画及 “在这发现了... (点我记录)” 浮动引导气泡）。<br>4. **二次点击记录**：点击这个绿色临时标记才会拉出“在这发现了...”玻璃拟态模态表单，并在模态关闭或提交后抹除该临时标记。<br>5. **表单数据收集**：收集 6 种带手绘矢量 SVG 图标的状态选择器、时间偏移选项（直接在后端扣减）、照片 URL、二维码 ID。<br>6. **点位精细显示**：从 `/api/locations/all` 接口中加载记录，使用 Eager-loading (立即加载) 模式拿到关联的 breed。猫类显示猫头 SVG、狗类显示狗头 SVG、其他显示小脚印，并通过 `:offset` 精准锚定各标记几何中心，杜绝视觉位置偏移。<br>7. **实时自动刷新**：引入 Vue `onMounted` 钩子开启 **`5秒周期轮询定时器`**，静默重新调用 `fetchNearbyLogs()` 保证地图点位在不刷新页面时也是最新的，并在 `onUnmounted` 中销毁以防泄漏。<br>8. **高德地图样式重置**：利用 Vue CSS `:deep` 样式穿透对底层渲染的 `.amap-copyright` 与 `.amap-logo` 进行彻底隐藏，并将我们所有的操作按钮层级 `z-index` 调整到 `1000` 级别，避免遮挡，确保触控流畅。 |
| `index.html` | 文件 | 网站物理入口。重命名网站 Title 为“爪印”，并配置了移动端友好型 `viewport` 缩放规则。 |

---

## 3. 系统模块交互与数据流向

以下是系统进行“录入新足迹”以及“自动获取更新”过程中的组件关系和数据流示意图：

### 3.1 用户录入足迹的数据流通路 (用户点击取点 -> 二次确认 -> 写入落库)

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
    FE_Map->>User: 弹出 "在这发现了..." 记录面板
    User->>FE_Map: 3. 填写特征描述并点击“标记”提交
    FE_Map->>FE_Map: 抹除地图上的临时定位针 (tempMarker = null)
    FE_Map->>BE_Ctrl: 4. POST 提交 DTO JSON 请求数据
    BE_Ctrl->>BE_Serv: 5. 转发给服务层
    
    rect rgb(240, 248, 240)
        Note over BE_Serv: 业务内部处理
        alt 提供扫码 ID (qrCodeId)
            BE_Serv->>MySQL: 优先按扫码 ID 查询关联小动物
        else 未提供扫码 ID
            BE_Serv->>MySQL: 退回为使用“昵称 + 动物大类”模糊查重
        end
        alt 用户没有显式点选行为类型
            BE_Serv->>Gemini: 特征文本发送给 Gemini API 进行语义推理
            Gemini-->>BE_Serv: 返回推导最吻合的行为状态枚举 (如 EATING)
        end
        alt 选择了发现时间偏移量 (例如: 10分钟前)
            BE_Serv->>BE_Serv: recorded_at = 当前时间 - 10 分钟
        end
    end
    
    BE_Serv->>MySQL: 6. INSERT 原生 SQL 空间几何数据 (ST_GeomFromText POINT)
    MySQL-->>BE_Serv: 写入物理落库成功 (SRID 4326)
    BE_Serv-->>BE_Ctrl: 返回新生成的 LocationLog 实体
    BE_Ctrl-->>FE_Map: 返回 200 OK 成功响应
    FE_Map->>FE_Map: 重新触发数据拉取，地图呈现小动物新 Marker (猫/狗/脚印)
```

### 3.2 地图点位自动刷新与类型区分渲染通路

```mermaid
graph TD
    %% 前端轮询机制
    Timer[5秒定时器] -->|1. 触发查询| fetch[fetchNearbyLogs 函数]
    fetch -->|2. 发送 GET 请求 /api/locations/all| API[/all 接口]
    
    %% 后端 Eager 加载数据
    API -->|3. 服务层调用| Serv[LocationLogService]
    Serv -->|4. Hibernate @Formula 转换经纬度| Repo[LocationLogRepository]
    Repo -->|5. SQL 查询关联 animals (Eager Load)| DB[(MySQL 8.0)]
    
    %% JSON 完整实体反向回传
    DB -->|6. 返回 LocationLog + Animal breed| Repo
    Repo -->|7. Eager 装配完整 JSON 结构| Serv
    Serv -->|8. 传输完整 JSON 数据| API
    API -->|9. 响应 200 OK 并解析数组| fetch
    
    %% 前端动态图标分支渲染
    fetch -->|10. 赋值给 animalLogs 数组| Markers[el-amap-marker 渲染循环]
    Markers -->|11. 判断 log.animal.breed| Switch{动物类型是什么?}
    
    Switch -->|值为 Cat| CatMarker[渲染为 猫头 SVG 标记, offset -18px]
    Switch -->|值为 Dog| DogMarker[渲染为 狗头 SVG 标记, offset -18px]
    Switch -->|值为 Other / 未知| OtherMarker[渲染为 小脚印 SVG 标记, offset -18px]
    
    CatMarker -.-> MapShow[高精度锚定定位在高德地图上展示]
    DogMarker -.-> MapShow
    OtherMarker -.-> MapShow
```

---

## 4. 开发设计原则 (Kaizen & UI-UX Pro Max)

本项目的代码组织结构贯彻了以下设计哲学：

1. **前后端职责单一 (SOC)**：
   * 前端仅仅关注于高精度的 GIS 点位采集、防重复提交状态机（Poka-Yoke）、以及令人眼前一亮的微动画交互；
   * 后端仅关注于地理数据安全入库、AI 语义判定、高维度的关系映射查重，不干涉前端地图渲染参数。
2. **空间拓扑精准性 (GIS Integrity)**：
   * 将高德底层的左上角锚定修正为物理中心或物理针尖对齐，确保坐标在不同缩放级别下不会漂移。
   * 采用 EPSG:4326 空间坐标系进行读写，保证轨迹的真实可靠。
3. **Poka-Yoke (防错) 与 44x44px 触控法则**：
   * 触控目标的最小物理响应热区严格遵守 `44px` 规格，降低误触率。
   * 当用户未输入 any 小动物的昵称与特征时，置灰并拦截提交，实时在输入框下方输出轻柔的校验文字，规避粗暴的原生 `alert` 弹窗对用户沉浸式心智的破坏。
