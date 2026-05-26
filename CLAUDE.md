# CLAUDE.md 开发规范

本文档为 Campus Paw-Track 项目的开发规范和常用指令速查手册。

---

## 1. 常用命令 (Build & Dev Commands)

### 后端 (Java Spring Boot)
- **本地编译**: `mvn clean compile` (需在 `backend` 目录下执行)
- **打包工程**: `mvn clean package -DskipTests`
- **运行测试**: `mvn test`

### 前端 (Vue 3 + Vite)
- **进入目录**: `cd frontend`
- **安装依赖**: `npm install`
- **启动开发环境**: `npm run dev`
- **生产环境编译**: `npm run build`
- **预览打包产物**: `npm run preview`

---

## 2. 核心编码规程 (Coding Standards)

### 核心语言规范
- 对话回复、功能说明、代码注释和技术文档必须 100% 使用简体中文。
- 技术框架、API 路由、代码语法、以及数据库关键字等英文专有名词必须保留其原始英文形式，禁止强行直译（如 `Spring Boot`、`POINT`、`DTO`）。

### 后端规范 (Java & MySQL)
- **代码结构**: 严格分离 Controller、Service、Repository、Entity 层，遵循 RESTful 规范。
- **空间地理数据操作**:
  - 数据库 `POINT` 字段使用 `SRID 4326`，在原生 SQL 中使用 `ST_GeomFromText('POINT(latitude longitude)', 4326)` 录入。**注意：MySQL 8 对 SRID 4326 要求纬度在先，经度在后**。
  - JPA 读取时使用 Hibernate `@Formula` 进行读写分离，使用 `ST_Latitude(location)` 和 `ST_Longitude(location)` 获取经纬度值，避免引入不稳定的第三方空间依赖。
- **错误捕获**: 核心数据保存需要开启 `@Transactional`，并在边界层捕获异常，返回标准的 `ResponseEntity` 结构。

### 前端规范 (Vue & CSS)
- **类型安全**: 所有 Vue 组件使用 `<script setup lang="ts">`，严格使用 TypeScript 编写，杜绝 `any` 滥用（第三方库无法推断时可通过 `(window as any)` 显式类型断言）。
- **美学规范 (Organic Natural)**:
  - 严禁在页面 UI（包含标记、按钮、气泡等）中使用原生的 Emoji 字符。所有图标必须使用内联 SVG 编写。
  - 移动端点击元素（按钮、关闭框、单选项等）必须保证其物理可点击面积不低于 `44x44px`。
  - 页面主打绿色（`#81C784`）与暖橙色（`#FF9800`），配合半透明毛玻璃特效。
- **表单优化 (Form CRO & Poka-Yoke)**:
  - 严禁采用点击后弹出 `alert("请输入...")` 的阻塞式弱校验。
  - 必须使用 Computed 属性校验表单（如 `isFormValid`），并在未输入内容时将提交按钮置灰禁用 (`:disabled`)。
  - 表单错误校验与成功提示应采用平滑的内联文字或非阻塞动画提示。
