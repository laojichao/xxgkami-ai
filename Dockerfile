# ============================================================
# 多阶段构建 - 小小怪卡密验证系统 Pro
# ============================================================

# ---- Stage 1: 构建前端 ----
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --prefer-offline
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: 构建后端 ----
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 3: 运行时镜像 ----
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="laojichao"
LABEL description="小小怪卡密验证系统 Pro"

# 安装 tini 用于正确的 PID 1 信号处理
RUN apk add --no-cache tini

WORKDIR /app

# 从构建阶段复制产物
COPY --from=backend-builder /app/backend/target/xxgkami-backend-*.jar app.jar
COPY --from=frontend-builder /app/frontend/dist ./static

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/backups /app/logs && chown -R appuser:appgroup /app
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/system/health || exit 1

# JVM 调优参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 使用 tini 作为 PID 1
ENTRYPOINT ["tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
