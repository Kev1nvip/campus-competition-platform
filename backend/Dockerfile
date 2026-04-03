# ================================
# 构建阶段
# ================================
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app

# 先复制pom.xml下载依赖（利用Docker缓存层，依赖不变时不重新下载）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码编译
COPY src ./src
RUN mvn package -DskipTests

# ================================
# 运行阶段
# ================================
FROM amazoncorretto:21-alpine

WORKDIR /app

# 从构建阶段复制jar包
COPY --from=builder /app/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dfile.encoding=UTF-8", "app.jar"]