#!/bin/bash

# ==========================================
# 项目一键部署与启动脚本
# ==========================================

# --- 配置区 ---
PROJECT_ROOT="$HOME/zy"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_DIR="$PROJECT_ROOT/backend"
WEB_ROOT="/var/www/html"          # Nginx 静态文件根目录
BACKEND_PORT=8080                 # 后端运行端口

# --- 颜色输出函数 ---
info() { echo -e "\033[32m[INFO] $1\033[0m"; }
warn() { echo -e "\033[33m[WARN] $1\033[0m"; }
error() { echo -e "\033[31m[ERROR] $1\033[0m"; exit 1; }

echo "=========================================="
info "开始自动化构建与部署: $(date)"
echo "=========================================="

# 1. 拉取最新代码
info "步骤 1: 拉取最新 Git 代码..."
cd "$PROJECT_ROOT" || error "找不到项目根目录: $PROJECT_ROOT"
git pull origin main --tags || warn "Git 拉取失败，将使用本地当前代码进行构建..."

# 2. 构建并部署前端
info "步骤 2: 开始构建前端..."
cd "$FRONTEND_DIR" || error "找不到前端目录: $FRONTEND_DIR"

# 如果服务器使用了 nvm，尝试加载环境
if [ -s "$HOME/.nvm/nvm.sh" ]; then
    . "$HOME/.nvm/nvm.sh"
    nvm use --delete-prefix default || warn "未设置默认 Node 版本，继续使用当前版本"
fi

info "正在安装前端依赖..."
npm install || error "前端依赖安装失败！"

info "正在构建前端静态资源..."
npm run build || error "前端打包构建失败！"

info "部署前端静态文件到 Nginx 目录: $WEB_ROOT"
# 清理旧静态文件
if [ -d "$WEB_ROOT" ]; then
    sudo rm -rf "${WEB_ROOT:?}"/*
else
    sudo mkdir -p "$WEB_ROOT"
fi
# 复制新构建的文件
sudo cp -r dist/* "$WEB_ROOT"/ || error "拷贝静态文件到 Nginx 目录失败！"
info "前端部署成功！"

# 3. 停止已运行的后端服务
info "步骤 3: 检查并停止旧的后端进程..."
PID=$(lsof -t -i:$BACKEND_PORT 2>/dev/null)
if [ -n "$PID" ]; then
    info "检测到端口 $BACKEND_PORT 正在运行，PID: $PID，正在停止服务..."
    kill "$PID"
    sleep 3
    # 再次检查是否停止
    PID_CHECK=$(lsof -t -i:$BACKEND_PORT 2>/dev/null)
    if [ -n "$PID_CHECK" ]; then
        warn "服务未响应，强制结束进程 (kill -9)..."
        kill -9 "$PID_CHECK"
        sleep 1
    fi
    info "旧服务已停止。"
else
    info "端口 $BACKEND_PORT 未被占用，无需停止。"
fi

# 4. 构建后端 JAR 包
info "步骤 4: 开始构建后端..."
cd "$BACKEND_DIR" || error "找不到后端目录: $BACKEND_DIR"

# 检查 Maven 是否可用
if ! command -v mvn &> /dev/null; then
    # 如果全局没有 mvn，尝试寻找项目里的 Maven
    if [ -f "./maven-3.9.6/bin/mvn" ]; then
        MVN_CMD="./maven-3.9.6/bin/mvn"
    else
        error "系统未安装 Maven 且未在后端目录中找到预置 Maven，打包失败！"
    fi
else
    MVN_CMD="mvn"
fi

info "正在执行 Maven 打包..."
$MVN_CMD clean package -DskipTests || error "后端打包失败！"

# 5. 启动后端服务
info "步骤 5: 启动后端服务..."
# 寻找 target 目录下的 jar 包（排除 sources 包）
JAR_FILE=$(find target -maxdepth 1 -name "*.jar" ! -name "*sources*" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    error "未在 target 目录中找到构建好的 JAR 包！"
fi

info "找到 JAR 包: $JAR_FILE"
info "正在后台启动 Java 服务..."

# 【关键】确保在 backend 目录启动，使相对路径 ../.local-configs/application-local.yml 能够正确被 Spring Boot 寻找到
nohup java -jar "$JAR_FILE" > backend.log 2>&1 &

# 6. 验证启动状态
info "步骤 6: 验证服务启动状态（最长等待 15 秒）..."
sleep 15
PID=$(lsof -t -i:$BACKEND_PORT 2>/dev/null)
if [ -n "$PID" ]; then
    info "后端服务启动成功！运行 PID: $PID，正在监听端口 $BACKEND_PORT"
    info "你可以通过查看日志确认启动进度: tail -f $BACKEND_DIR/backend.log"
else
    error "后端服务未在端口 $BACKEND_PORT 启动，可能启动失败，请检查日志: cat $BACKEND_DIR/backend.log"
fi

echo "=========================================="
info "全套部署流程执行完毕！"
echo "=========================================="
