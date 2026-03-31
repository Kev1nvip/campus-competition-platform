# 系统架构设计文档

> 版本：v1.0
> 创建人：负责人
> 创建日期：2026-03
> 维护人：负责人

---

## 一、架构概述

### 1.1 设计目标

```
可用性：核心功能稳定运行，本地环境一键启动
安全性：JWT认证，接口级别权限控制
可扩展：业务模块低耦合，后续功能易于添加
可维护：代码分层清晰，文档完整
```

### 1.2 整体架构风格

```
前后端分离的单体应用架构

选择理由：
  单体应用开发效率高，调试方便
  满足课程要求的同时控制复杂度
  后续毕设阶段可按需拆分微服务
```

---

## 二、整体架构图

```mermaid
graph TB
    subgraph 客户端层
        S[学生端<br/>Vue3]
        T[老师端<br/>Vue3]
        A[管理员端<br/>Vue3]
    end

    subgraph Docker容器网络
        subgraph 接入层
            N[Nginx<br/>:80<br/>静态资源托管 + 反向代理]
        end

        subgraph 应用层
            B[SpringBoot3 后端<br/>:8080<br/>业务逻辑处理]
        end

        subgraph 数据层
            DB[(PostgreSQL + PGVector<br/>:5432<br/>主数据库 + 向量存储)]
            R[(Redis<br/>:6379<br/>缓存 + 并发控制)]
            MQ[RabbitMQ<br/>:5672<br/>异步消息队列]
        end

        subgraph AI层
            AI[LangChain4j<br/>RAG引擎<br/>内嵌于后端]
            LLM[智谱AI / 通义千问<br/>外部API]
        end
    end

    S --> N
    T --> N
    A --> N
    N -->|静态资源| S
    N -->|/api/* 反向代理| B
    B --> DB
    B --> R
    B --> MQ
    MQ --> B
    B --> AI
    AI --> DB
    AI --> LLM
```

---

## 三、各层详细说明

### 3.1 客户端层

```
技术栈：Vue3 + Vite + TypeScript + Element Plus

三端共用同一套前端代码：
  根据登录用户的 role 字段区分展示内容
  student  → 学生端页面和菜单
  teacher  → 老师端页面和菜单
  admin    → 管理员端页面和菜单

路由权限控制：
  Vue Router 路由守卫
  未登录 → 跳转登录页
  角色不匹配 → 跳转403页面
```

### 3.2 接入层（Nginx）

```
职责：
  1. 托管前端静态文件（Vue3 build产物）
  2. 反向代理后端API（/api/* → 后端:8080）
  3. 处理跨域问题

Nginx配置核心逻辑：
  所有 /api/ 开头的请求 → 转发给后端
  其余请求 → 返回前端静态文件
  前端路由刷新处理 → try_files $uri /index.html
```

```nginx
# Nginx 核心配置
server {
    listen 80;

    # 托管前端静态文件
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端API
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3.3 应用层（SpringBoot3）

```
技术栈：SpringBoot3 + Spring Security + JPA + LangChain4j

分层结构：
  Controller   → 接收请求，参数校验，返回响应
  Service      → 业务逻辑处理
  Repository   → 数据库操作
  Entity       → 数据库实体映射
  DTO          → 接口入参对象
  VO           → 接口出参对象

核心组件：
  Spring Security + JWT  → 认证与权限控制
  Spring Data JPA        → ORM数据库操作
  Spring AMQP            → RabbitMQ消息收发
  Spring Data Redis      → Redis操作
  LangChain4j            → RAG链路实现
  Knife4j                → 接口文档自动生成
```

**业务模块划分**

```mermaid
graph LR
    subgraph SpringBoot3应用
        UC[用户模块<br/>user]
        CM[竞赛模块<br/>competition]
        SM[报名模块<br/>signup]
        TM[组队模块<br/>team]
        RM[招募模块<br/>recruitment]
        AM[审核模块<br/>audit]
        AW[获奖模块<br/>award]
        NM[通知模块<br/>notification]
        AI[AI推荐模块<br/>ai]
        ST[统计模块<br/>statistics]
    end
```

### 3.4 数据层

#### PostgreSQL + PGVector

```
用途：
  主数据库：存储所有业务数据
  PGVector扩展：存储文档向量，支持相似度检索

PGVector说明：
  PostgreSQL的向量扩展插件
  无需单独部署向量数据库
  直接在同一个PostgreSQL实例中使用
  支持cosine相似度检索

数据库：campus_competition
核心表：7张（详见数据库设计文档）
```

#### Redis

```
用途：
  1. 竞赛列表缓存
     key：competition:list:{status}:{page}
     TTL：5分钟

  2. 老师带队数量计数器（并发控制）
     key：teacher:quota:{teacherId}:{competitionId}
     value：当前带队数量
     TTL：永久（竞赛结束后清除）

  3. 用户Token存储
     key：token:{userId}
     value：token信息
     TTL：2小时

  4. 竞赛名额计数器
     key：competition:quota:{competitionId}
     value：已报名数量
     TTL：永久（竞赛结束后清除）
```

#### RabbitMQ

```
用途：
  异步处理所有消息通知
  解耦核心业务和通知推送

队列设计：
  notification.queue  → 处理所有类型的通知消息

消息流转：
  业务操作完成
    → 发送消息到 notification.queue
    → Consumer消费消息
    → 写入 sys_notification 表
    → 前端轮询获取未读通知
```

### 3.5 AI层（LangChain4j + RAG）

```
内嵌于SpringBoot3后端，不单独部署

组件：
  LangChain4j   → RAG框架，管理检索和生成链路
  PGVector      → 向量存储（复用PostgreSQL）
  外部LLM API   → 智谱AI GLM 或 通义千问

RAG完整链路：
```

```mermaid
graph LR
    subgraph 文档入库阶段
        D[txt竞赛文档] --> C[文本分块<br/>Chunk]
        C --> E[Embedding<br/>向量化]
        E --> V[(PGVector<br/>向量存储)]
    end

    subgraph 查询阶段
        Q[用户输入<br/>方向描述] --> QE[Query<br/>Embedding]
        QE --> S[相似度检索<br/>Top5]
        V --> S
        S --> P[构建Prompt<br/>含检索结果]
        P --> L[LLM生成<br/>推荐结果]
        L --> R[返回推荐<br/>含来源引用]
    end
```

---

## 四、请求处理流程

### 4.1 普通请求流程

```mermaid
sequenceDiagram
    participant C as 前端
    participant N as Nginx
    participant B as SpringBoot
    participant S as Spring Security
    participant SV as Service
    participant DB as PostgreSQL
    participant R as Redis

    C->>N: HTTP请求（含JWT Token）
    N->>B: 反向代理转发 /api/*
    B->>S: JWT过滤器验证Token
    S-->>B: 验证通过，注入用户信息
    B->>SV: Controller调用Service
    SV->>R: 查询缓存
    alt 缓存命中
        R-->>SV: 返回缓存数据
    else 缓存未命中
        SV->>DB: 查询数据库
        DB-->>SV: 返回数据
        SV->>R: 写入缓存
    end
    SV-->>B: 返回业务数据
    B-->>N: 统一响应体 Result<T>
    N-->>C: HTTP响应
```

### 4.2 并发控制流程（报名/带队）

```mermaid
sequenceDiagram
    participant C as 前端
    participant B as SpringBoot
    participant R as Redis
    participant DB as PostgreSQL
    participant MQ as RabbitMQ

    C->>B: 提交报名申请
    B->>R: DECR quota计数器
    alt 计数器 < 0
        R-->>B: 名额不足
        B->>R: INCR 回滚计数器
        B-->>C: 返回失败：名额已满
    else 计数器 >= 0
        B->>DB: 写入报名记录（乐观锁version）
        alt 乐观锁冲突
            DB-->>B: 更新失败
            B->>R: INCR 回滚计数器
            B-->>C: 返回失败：名额已满
        else 写入成功
            DB-->>B: 成功
            B->>MQ: 发送通知消息
            B-->>C: 返回成功
        end
    end
```

### 4.3 异步通知流程

```mermaid
sequenceDiagram
    participant B as SpringBoot业务
    participant MQ as RabbitMQ
    participant CS as 消息消费者
    participant DB as PostgreSQL
    participant C as 前端

    B->>MQ: 发布通知消息（非阻塞）
    B-->>C: 立即返回业务结果
    MQ->>CS: 推送消息
    CS->>DB: 写入sys_notification表
    Note over C,DB: 前端每30秒轮询
    C->>B: GET /notifications/unread/count
    B->>DB: 查询未读数量
    DB-->>B: 返回数量
    B-->>C: 返回未读数量
    C->>C: 更新红点显示
```

---

## 五、安全架构

### 5.1 认证流程

```mermaid
sequenceDiagram
    participant C as 前端
    participant B as SpringBoot
    participant S as Spring Security
    participant DB as PostgreSQL

    C->>B: POST /api/v1/auth/login
    B->>DB: 查询用户，验证密码（BCrypt）
    DB-->>B: 用户信息
    B->>S: 生成JWT Token
    S-->>B: Token（含userId、role、过期时间）
    B-->>C: 返回Token
    C->>C: 存储Token到localStorage

    Note over C,B: 后续每次请求
    C->>B: 请求头携带 Authorization: Bearer {token}
    B->>S: JWT过滤器验证Token
    S-->>B: 解析userId和role，注入SecurityContext
    B->>B: Controller获取当前用户信息
```

### 5.2 权限控制

```
实现方式：Spring Security + 自定义注解

接口级别控制：
  @PreAuthorize("hasRole('ADMIN')")          → 仅管理员
  @PreAuthorize("hasRole('TEACHER')")        → 仅老师
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')") → 管理员或老师
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')") → 管理员或学生

数据级别控制（在Service层手动判断）：
  老师只能审核分配给自己的报名
  队长才能提交审核和管理队伍
  只有发布人或管理员能编辑竞赛
```

### 5.3 安全规则

```
密码：BCrypt加密存储，不可逆
Token：
  有效期2小时
  过期后前端跳转登录页
  不同用户Token相互独立
接口：
  所有 /api/* 接口（除login和register）必须携带Token
  Token无效返回401
  权限不足返回403
```

---

## 六、部署架构

### 6.1 Docker容器规划

```mermaid
graph TB
    subgraph Docker Compose
        N[nginx<br/>镜像：nginx:alpine<br/>端口：80:80]
        B[backend<br/>镜像：自构建<br/>端口：8080:8080]
        DB[postgres<br/>镜像：pgvector/pgvector:pg16<br/>端口：5432:5432]
        R[redis<br/>镜像：redis:7.2-alpine<br/>端口：6379:6379]
        MQ[rabbitmq<br/>镜像：rabbitmq:3.12-management<br/>端口：5672:5672<br/>15672:15672]
    end

    subgraph 本地目录挂载
        V1[./uploads → /app/uploads<br/>获奖证书图片]
        V2[postgres_data<br/>数据库数据]
        V3[./docs/03-database/init.sql<br/>初始化脚本]
    end

    DB --- V2
    DB --- V3
    B --- V1
```

### 6.2 容器启动顺序

```
depends_on 依赖关系：

postgres ──→ backend ──→ nginx
redis    ──→ backend
rabbitmq ──→ backend
```

### 6.3 docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL + PGVector
  postgres:
    image: pgvector/pgvector:pg16
    container_name: competition-postgres
    environment:
      POSTGRES_DB: campus_competition
      POSTGRES_USER: competition
      POSTGRES_PASSWORD: competition123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docs/03-database/init.sql:/docker-entrypoint-initdb.d/init.sql
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U competition"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis
  redis:
    image: redis:7.2-alpine
    container_name: competition-redis
    ports:
      - "6379:6379"
    command: redis-server --requirepass redis123
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "redis123", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # RabbitMQ
  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: competition-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: competition
      RABBITMQ_DEFAULT_PASS: competition123
    ports:
      - "5672:5672"
      - "15672:15672"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # SpringBoot后端
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: competition-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: campus_competition
      DB_USERNAME: competition
      DB_PASSWORD: competition123
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: redis123
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: competition
      RABBITMQ_PASSWORD: competition123
    ports:
      - "8080:8080"
    volumes:
      - ./uploads:/app/uploads
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  # Nginx + 前端
  nginx:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: competition-nginx
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
```

### 6.4 Dockerfile

**后端 Dockerfile**

```dockerfile
# backend/Dockerfile

# 构建阶段
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app
COPY pom.xml .
# 先下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# 运行阶段
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**前端 Dockerfile**

```dockerfile
# frontend/Dockerfile

# 构建阶段
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json .
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine
# 复制前端构建产物
COPY --from=builder /app/dist /usr/share/nginx/html
# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
```

**frontend/nginx.conf**

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        # 支持Vue Router的history模式
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端API
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        # 超时配置
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }
}
```

---

## 七、技术选型说明

| 技术 | 选型 | 理由 |
|------|------|------|
| 前端框架 | Vue3 + Vite | 团队熟悉，生态成熟，开发效率高 |
| UI组件库 | Element Plus | Vue3生态最完善的组件库 |
| 后端框架 | SpringBoot3 | 课程要求，Java生态最成熟 |
| 数据库 | PostgreSQL | 支持PGVector扩展，一个数据库解决关系型和向量存储 |
| 向量存储 | PGVector | 复用PostgreSQL，无需单独部署向量数据库 |
| 缓存 | Redis | 缓存+并发控制双重用途，成熟稳定 |
| 消息队列 | RabbitMQ | 轻量级，适合中小规模，管理界面友好 |
| 权限 | Spring Security + JWT | SpringBoot生态原生支持，无状态认证 |
| AI框架 | LangChain4j | Java生态的RAG框架，与SpringBoot集成方便 |
| 容器化 | Docker + Compose | 一键启动所有服务，环境一致性 |
| 接口文档 | Knife4j | SpringBoot生态，注解自动生成文档 |

---

## 八、本地启动说明

```bash
# 前提：已安装 Docker Desktop 并启动

# 1. 克隆项目
git clone git@github.com:你的用户名/campus-competition-platform.git
cd campus-competition-platform

# 2. 一键启动所有服务
docker compose up -d

# 3. 等待所有服务启动（约2-3分钟）
docker compose ps
# 确认所有服务状态为 running

# 4. 访问服务
# 前端：http://localhost
# 后端接口文档：http://localhost:8080/doc.html
# RabbitMQ管理台：http://localhost:15672

# 5. 停止所有服务
docker compose stop

# 6. 查看日志
docker compose logs backend
docker compose logs postgres
```

---

## 九、开发环境 vs 生产环境

| 配置项 | 开发环境 | 生产环境（Docker）|
|--------|----------|-----------------|
| 前端访问 | http://localhost:5173 | http://localhost |
| 后端访问 | http://localhost:8080 | 通过Nginx代理 |
| 跨域处理 | 后端配置CORS | Nginx反向代理，无跨域 |
| 数据库 | Docker容器 | Docker容器 |
| 配置文件 | application-dev.yml | application-prod.yml |
| 日志级别 | DEBUG | INFO |