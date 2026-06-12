# campus-competition-platform
# 校园学术竞赛管理平台

## 项目结构

```
campus-competition-platform/
├── frontend/                     # Vue3 + Vite 前端项目
│   ├── src/
│   │   ├── api/                  # API 请求层
│   │   ├── assets/               # 静态资源
│   │   ├── components/           # 公共组件
│   │   ├── router/               # 路由配置
│   │   ├── store/                # Pinia 状态管理
│   │   ├── types/                # TypeScript 类型定义
│   │   ├── utils/                # 工具函数
│   │   └── views/                # 页面视图
│   ├── Dockerfile
│   └── nginx.conf                # Nginx 部署配置
├── backend/                      # SpringBoot3 后端项目
│   ├── src/main/java/com/competition/backend/
│   │   ├── common/               # 公共模块（异常、结果、安全、常量）
│   │   ├── config/               # 配置类（安全、Redis、AI、Knife4j等）
│   │   ├── controller/           # 控制器层
│   │   ├── dto/                  # 数据传输对象
│   │   ├── entity/               # 实体类
│   │   ├── repository/           # 数据访问层
│   │   ├── service/              # 业务逻辑层
│   │   ├── task/                 # 定时任务
│   │   ├── util/                 # 工具类
│   │   └── vo/                   # 视图对象
│   └── Dockerfile
├── docker/                       # Docker 编排辅助配置
│   ├── nginx/                    # Nginx 配置
│   └── postgres/                 # PostgreSQL 初始化脚本
├── docs/                         # 项目文档
│   ├── 01-requirements/          # 需求文档
│   ├── 02-architecture/          # 架构设计
│   ├── 03-database/              # 数据库设计
│   ├── 04-api/                   # 接口文档
│   ├── 05-dev-guide/             # 开发指南
│   ├── 06-team/                  # 团队协作
│   └── 07-test/                  # 测试相关
├── .github/                      # GitHub Actions 工作流
├── docker-compose.yml            # 一键部署编排
└── .env.example                  # 环境变量模板
```

## 技术栈

| 层级     | 技术                                                  |
| -------- | ----------------------------------------------------- |
| 前端     | Vue 3 + Vite 8 + TypeScript 5.9 + Pinia 3 + Vue Router 5 + Axios |
| 后端     | Spring Boot 3.3 + Java 17 + Maven                     |
| 数据库   | PostgreSQL 16（PGVector 向量插件）                     |
| 缓存     | Redis 7.2                                              |
| 消息队列 | RabbitMQ 3.12                                          |
| 权限     | Spring Security + JWT（jjwt 0.12.5）                   |
| AI 能力  | LangChain4j 0.27.1 + PGVector + 硅基流动 API          |
| 接口文档 | Knife4j 4.5.0（OpenAPI 3）                             |
| 容器化   | Docker + Docker Compose                                |
| 反向代理 | Nginx                                                  |

## 本地启动

### 前置条件
- Node.js >= 18
- JDK 17
- Docker & Docker Compose（可选，用于启动基础设施）

### 1. 启动基础设施（PostgreSQL + Redis + RabbitMQ）
```bash
docker compose up -d postgres redis rabbitmq
```

### 2. 启动后端
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
启动后 API 文档访问：http://localhost:8080/doc.html

### 3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

### 一键启动所有服务
```bash
docker compose up -d
```

## 分支规范
- `main`：主分支，受保护
- `dev`：开发分支
- `feature/xxx`：功能分支

## Commit 规范
- `feat`：新功能
- `fix`：修复 Bug
- `docs`：文档
- `chore`：工程配置
- `refactor`：重构
- `test`：测试
