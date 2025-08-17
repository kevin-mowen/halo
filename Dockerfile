# ============================
# Halo 镜像（多阶段构建）
# ============================

# ============ Builder ============
FROM crpi-0nyhmsk4kaamfjub.cn-guangzhou.personal.cr.aliyuncs.com/mokevin/dragonwell:21 AS builder

WORKDIR /application

# 复制完整 Halo 项目源码
COPY . .

# 构建 Spring Boot jar（跳过测试）
RUN ./gradlew :application:bootJar -x test

# ============ Runtime ============
FROM crpi-0nyhmsk4kaamfjub.cn-guangzhou.personal.cr.aliyuncs.com/mokevin/dragonwell:21
LABEL maintainer="mokevin <kevin_mowen@163.com>"

WORKDIR /application

# 从 Builder 阶段复制构建好的 Jar
COPY --from=builder /application/application/build/libs/*.jar app.jar

# 支持 Spring Boot Layered jar
RUN java -Djarmode=layertools -jar app.jar extract

# 可选：复制 README 或其他文件
COPY --from=builder /application/application/README.md ./

# 设置环境变量
ENV JVM_OPTS="-Xmx512m -Xms256m" \
    HALO_WORK_DIR="/root/.halo2" \
    SPRING_CONFIG_LOCATION="optional:classpath:/;optional:file:/root/.halo2/" \
    TZ=Asia/Shanghai

# 设置时区
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# 暴露 Halo 默认端口
EXPOSE 8090

# 启动 Halo（支持虚拟线程）
ENTRYPOINT ["sh", "-c", "java -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true $JVM_OPTS org.springframework.boot.loader.launch.JarLauncher $0 $@"]
