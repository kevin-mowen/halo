# ============================
# Halo + UI 合并镜像（多阶段构建）
# ============================

# ============ Node.js Builder ============
FROM node:18-slim AS node-builder

# 安装必要的构建工具和 pnpm
RUN apt-get update && \
    apt-get install -y python3 make g++ && \
    npm install -g pnpm@10.12.4 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /ui

# 复制前端项目文件
COPY ui/package.json ui/pnpm-lock.yaml ui/pnpm-workspace.yaml ./
COPY ui/packages ./packages

# 设置环境变量以优化依赖安装
ENV NODE_ENV=production
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"

# 安装依赖（不跳过可选依赖，让rolldown正确安装平台绑定）
RUN pnpm install --frozen-lockfile

# 复制前端源码
COPY ui .

# 构建前端包和UI应用（跳过TypeScript检查）
RUN pnpm build:packages && pnpm run build:console && pnpm run build:uc

# ============ Java Builder ============
FROM crpi-0nyhmsk4kaamfjub.cn-guangzhou.personal.cr.aliyuncs.com/mokevin/dragonwell:21 AS java-builder

WORKDIR /application

# 复制完整 Halo 项目源码
COPY . .

# 从Node.js构建阶段复制UI构建产物
COPY --from=node-builder /ui/build/dist ./ui/build/dist

# 修复gradlew的Windows换行符并确保执行权限
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# 构建 Spring Boot jar（跳过测试、git属性生成和UI构建）
RUN ./gradlew :application:bootJar -x test -x generateGitProperties -x :ui:build -x :ui:pnpmSetup -x :ui:pnpmInstall

# ============ Runtime ============
FROM crpi-0nyhmsk4kaamfjub.cn-guangzhou.personal.cr.aliyuncs.com/mokevin/dragonwell:21

LABEL maintainer="mokevin <kevin_mowen@163.com>"

# 安装Node.js来运行前端服务
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g pnpm@10.12.4 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /application

# 从Java构建阶段复制构建好的 Jar
COPY --from=java-builder /application/application/build/libs/*.jar app.jar

# 从Node.js构建阶段复制前端构建产物和源码
COPY --from=node-builder /ui /ui

# 复制启动脚本
COPY start.sh /application/start.sh
RUN chmod +x /application/start.sh

# 设置环境变量
ENV JVM_OPTS="-Xmx512m -Xms256m" \
    HALO_WORK_DIR="/root/.halo2" \
    SPRING_CONFIG_LOCATION="optional:classpath:/;optional:file:/root/.halo2/" \
    TZ=Asia/Shanghai \
    NODE_ENV=production

# 设置时区
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# 暴露端口（Halo: 8090, UI: 3000）
EXPOSE 8090 3000

# 使用启动脚本同时启动前后端服务
ENTRYPOINT ["/application/start.sh"]
