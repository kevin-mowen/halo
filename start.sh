#!/bin/bash

# Halo + UI 启动脚本
# 同时启动前端服务和后端服务

set -e

echo "=== Halo + UI 启动脚本 ==="

# 等待外部依赖准备就绪的函数
wait_for_deps() {
    echo "检查外部依赖..."
    # 这里可以添加对数据库等外部服务的健康检查
    # 例如等待PostgreSQL启动
}

# 启动前端服务的函数
start_ui() {
    echo "启动前端服务..."
    cd /ui
    # 确保依赖已安装
    if [ ! -d "node_modules" ]; then
        echo "安装前端依赖..."
        pnpm install --frozen-lockfile
    fi
    
    # 启动前端开发服务器（生产模式）
    echo "启动UI服务 (端口: 3000)..."
    pnpm dev &
    UI_PID=$!
    echo "前端服务PID: $UI_PID"
}

# 启动后端服务的函数
start_halo() {
    echo "启动Halo后端服务..."
    cd /application
    
    # 启动Halo应用
    echo "启动Halo应用 (端口: 8090)..."
    java -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
         $JVM_OPTS \
         -jar app.jar &
    HALO_PID=$!
    echo "Halo服务PID: $HALO_PID"
}

# 信号处理函数
cleanup() {
    echo "收到终止信号，正在关闭服务..."
    if [ ! -z "$UI_PID" ]; then
        echo "关闭前端服务 (PID: $UI_PID)"
        kill $UI_PID 2>/dev/null || true
    fi
    if [ ! -z "$HALO_PID" ]; then
        echo "关闭Halo服务 (PID: $HALO_PID)"
        kill $HALO_PID 2>/dev/null || true
    fi
    exit 0
}

# 注册信号处理
trap cleanup SIGTERM SIGINT

# 主启动流程
main() {
    wait_for_deps
    
    # 启动服务
    start_ui
    sleep 5  # 等待前端服务启动
    start_halo
    
    echo "=== 所有服务启动完成 ==="
    echo "前端UI: http://localhost:3000"
    echo "Halo后端: http://localhost:8090"
    echo "使用 Ctrl+C 或发送 SIGTERM 信号来停止服务"
    
    # 等待任一服务退出
    wait $UI_PID $HALO_PID
}

# 执行主流程
main