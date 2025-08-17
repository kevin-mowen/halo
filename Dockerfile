# ============================
# ✅ 推荐重构版 Dockerfile
# ============================
# ✔ 兼容国内构建（用 dragonwell 基础镜像）
# ✔ 支持 Spring Boot layer jar 结构
# ✔ 设置合理的默认参数和时区
# ✔ 保持与原 Dockerfile 功能一致

# ============ Builder ============
FROM registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:21 AS builder

WORKDIR /application
# 复制项目完整代码（推荐完整构建）
COPY . .

# 使用 gradle wrapper 构建 halo 的 jar 包（跳过测试）
RUN ./gradlew :application:bootJar -x test

# ============ Runtime ============
FROM registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:21
LABEL maintainer="mokevin <kevin_mowen@163.com>"

WORKDIR /application

# 解压 spring boot 的 Layered jar 结构
COPY --from=builder /application/application/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

COPY --from=builder /application/application/build/libs/*.jar app.jar
COPY --from=builder /application/README.md ./

# 设置环境变量
ENV JVM_OPTS="-Xmx512m -Xms256m" \
    HALO_WORK_DIR="/root/.halo2" \
    SPRING_CONFIG_LOCATION="optional:classpath:/;optional:file:/root/.halo2/" \
    TZ=Asia/Shanghai

# 设置时区
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

EXPOSE 8090

# 启动 Halo（支持虚拟线程）
ENTRYPOINT ["sh", "-c", "java -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true $JVM_OPTS org.springframework.boot.loader.launch.JarLauncher $0 $@"]
