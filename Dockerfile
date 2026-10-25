# ============================================================
# 多阶段构建 - 小小怪卡密验证系统 Pro
# ============================================================

# ---- Stage 1: 构建前端 ----
# node:20-alpine：vite 7.x engines 要求 ^20.19.0 || >=22.12.0，
# 且使用 crypto.hash()（Node 18 无此 API），node:18 构建必然失败
FROM node:20-alpine AS frontend-builder
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

# 安装 tini 用于正确的 PID 1 信号处理。
# 同时安装 mysql-client：BackupService 通过 ProcessBuilder 调用 mysqldump 生成备份，
# 运行镜像若不含该可执行文件，/backup 接口必然失败（退出码 127 command not found）。
RUN apk add --no-cache tini mysql-client

WORKDIR /app

# 从构建阶段复制产物
COPY --from=backend-builder /app/backend/target/xxgkami-backend-*.jar app.jar
# 前端产物放 static_build：compose 将 frontend_dist 命名卷挂载在 /app/static，
# 启动时把 static_build 覆盖同步到 static，避免"命名卷只在首次创建时复制、
# 镜像升级后 nginx 仍服务旧前端"的版本错配
COPY --from=frontend-builder /app/frontend/dist ./static_build

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
# 预创建所有运行期需要写入的目录（含 docker-compose 挂载的卷挂载点）：
#   static — 前端产物（卷挂载点）；backups — 数据库备份；logs — 日志；
#   uploads — 用户上传文件；keys — 历史 ECC 目录
# 必须提前创建并授权，否则非 root 用户无法写入挂载卷，应用启动即报权限错误。
RUN mkdir -p /app/static /app/backups /app/logs /app/uploads /app/keys && chown -R appuser:appgroup /app
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

# 启动前把镜像内最新的前端产物同步到 /app/static（compose 中该路径挂载了
# frontend_dist 命名卷）。Docker 命名卷只在首次创建时从镜像复制内容，
# 镜像升级后卷里仍是旧前端——每次启动主动覆盖同步即可消除版本错配。
CMD ["sh", "-c", "cp -a /app/static_build/. /app/static/ 2>/dev/null || true; exec java $JAVA_OPTS -jar app.jar"]
