# 技术选型说明

> 版本：v1.0
> 创建人：负责人
> 创建日期：2026-03
> 维护人：负责人

---

## 一、技术栈总览

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 前端框架 | Vue3 | 3.4.x | 前端核心框架 |
| 前端构建 | Vite | 5.x | 开发服务器和构建工具 |
| 前端语言 | TypeScript | 5.x | 类型安全 |
| UI组件库 | Element Plus | 2.x | 页面组件 |
| 状态管理 | Pinia | 2.x | 全局状态管理 |
| 路由 | Vue Router | 4.x | 前端路由 |
| HTTP客户端 | Axios | 1.x | 接口请求 |
| 后端框架 | SpringBoot | 3.5.13 | 后端核心框架 |
| 后端语言 | Java | 17 | 后端开发语言 |
| 权限认证 | Spring Security + JWT | 6.x | 认证与权限控制 |
| ORM | Spring Data JPA | 3.x | 数据库操作 |
| 数据库 | PostgreSQL | 16 | 主数据库 |
| 向量扩展 | PGVector | 0.7.x | 向量存储与检索 |
| 缓存 | Redis | 7.2 | 缓存和并发控制 |
| 消息队列 | RabbitMQ | 3.12 | 异步消息通知 |
| AI框架 | LangChain4j | 0.27.x | RAG链路实现 |
| 代码简化 | Lombok | 1.18.x | 减少样板代码 |
| 接口文档 | Knife4j | 4.x | 自动生成接口文档 |
| 依赖管理 | Maven | 3.9.x | 后端依赖管理 |
| 包管理 | npm | 10.x | 前端依赖管理 |
| 容器化 | Docker + Compose | 24.x | 环境一致性部署 |
| Web服务器 | Nginx | alpine | 静态资源托管+反向代理 |
| 版本控制 | Git + GitHub | - | 代码管理和团队协作 |

---

## 二、前端技术选型

### 2.1 Vue3

```
选择理由：
  团队成员均有Vue使用经验，上手成本低
  Composition API使逻辑复用更清晰
  相比Vue2性能提升显著
  配合TypeScript支持更完善

版本选择：3.4.x（当前稳定版）
```

### 2.2 Vite

```
选择理由：
  相比Webpack冷启动速度快10倍以上
  原生支持Vue3和TypeScript
  开发体验好，热更新几乎即时

版本选择：5.x
```

### 2.3 TypeScript

```
选择理由：
  接口返回数据有类型定义，前后端对接更安全
  IDE智能提示，减少低级错误
  代码可读性更强，团队协作时理解成本低

使用范围：
  所有 .vue 文件的 script 部分
  所有 .ts 工具函数和API封装
  所有类型定义文件 .d.ts
```

### 2.4 Element Plus

```
选择理由：
  Vue3生态最成熟的组件库
  表单、表格、弹窗等后台管理常用组件完善
  文档详细，问题容易查找解决

主要使用组件：
  ElTable       → 列表展示（竞赛列表、报名列表）
  ElForm        → 表单（报名表单、发布竞赛）
  ElDialog      → 弹窗（审核操作、确认框）
  ElMenu        → 侧边导航
  ElBadge       → 通知红点
  ElUpload      → 获奖证书上传
  ElSteps       → 流程步骤展示
```

### 2.5 Pinia

```
选择理由：
  Vue3官方推荐的状态管理库
  相比Vuex更简洁，无需mutation
  完整的TypeScript支持

存储内容：
  userStore     → 当前登录用户信息、Token
  notificationStore → 未读通知数量
```

### 2.6 Axios

```
选择理由：
  最主流的HTTP客户端，生态成熟
  拦截器机制方便统一处理Token和错误

封装内容：
  请求拦截器：自动添加Authorization头
  响应拦截器：统一处理错误码，Token过期跳转登录
```

---

## 三、后端技术选型

### 3.1 SpringBoot3

```
选择理由：
  课程服务端开发要求使用Java技术栈
  SpringBoot3自动配置，减少繁琐配置
  生态最完善，问题容易查找解决

版本选择：3.5.13
  要求Java17+，我们使用Java17完全兼容
  内置对GraalVM、虚拟线程等新特性的支持
```

### 3.2 Spring Security + JWT

```
选择理由：
  SpringBoot生态原生支持，集成无缝
  JWT无状态认证，不依赖服务端Session
  RBAC权限控制满足三角色需求
  满足课程进阶评分要求（Spring Security 10分）

JWT设计：
  Payload包含：userId、role、过期时间
  有效期：24小时
  存储位置：前端localStorage
  传输方式：请求头 Authorization: Bearer {token}
```

### 3.3 Spring Data JPA

```
选择理由：
  SpringBoot生态原生支持
  简单查询无需写SQL，复杂查询支持JPQL和原生SQL
  配合PostgreSQL使用稳定

使用规范：
  简单CRUD：使用Repository接口方法
  复杂查询：使用@Query注解写JPQL或原生SQL
  禁止在Service层直接写SQL
```

### 3.4 Lombok

```
选择理由：
  减少Entity、DTO、VO的样板代码
  @Data、@Builder、@Slf4j等注解显著提升开发效率

主要注解：
  @Data         → 自动生成getter/setter/toString
  @Builder      → 建造者模式创建对象
  @Slf4j        → 自动注入log对象
  @RequiredArgsConstructor → 自动生成构造器注入
  @NoArgsConstructor       → 无参构造器
  @AllArgsConstructor      → 全参构造器
```

### 3.5 Knife4j

```
选择理由：
  基于Swagger3，SpringBoot3原生支持
  UI界面比原生Swagger更友好
  支持接口调试，联调时直接在文档页面测试

访问地址：http://localhost:8080/doc.html
使用规范：
  每个Controller类加@Tag注解说明模块
  每个接口方法加@Operation注解说明功能
  每个DTO字段加@Schema注解说明含义
```

---

## 四、数据层技术选型

### 4.1 PostgreSQL

```
选择理由：
  相比MySQL，原生支持PGVector扩展
  一个数据库同时解决关系型数据和向量存储
  无需额外部署向量数据库（减少复杂度）
  JSON字段支持更强
  满足课程要求（PostgreSQL在加分项中明确提及）

版本选择：16（当前稳定版）
镜像选择：pgvector/pgvector:pg16（内置PGVector）
```

### 4.2 PGVector

```
选择理由：
  PostgreSQL官方认可的向量扩展
  无需单独部署Milvus/Weaviate等向量数据库
  使用pgvector/pgvector:pg16镜像，开箱即用
  LangChain4j原生支持PGVector

用途：
  存储竞赛介绍文档的向量表示
  支持cosine相似度检索
  支持Top-K最近邻查询
```

### 4.3 Redis

```
选择理由：
  缓存和并发控制双重用途
  原子操作（INCR/DECR）天然支持并发计数
  满足课程进阶评分要求（Redis 10分）

使用场景：
  场景1：竞赛列表缓存（降低数据库压力）
  场景2：老师带队计数器（并发控制）
  场景3：竞赛名额计数器（并发控制）
  场景4：用户Token缓存

版本选择：7.2-alpine（轻量，功能完整）
```

### 4.4 RabbitMQ

```
选择理由：
  相比Kafka更轻量，适合中小规模消息量
  管理界面友好，方便演示和调试
  满足课程进阶评分要求（消息队列 15分）
  SpringBoot原生支持（Spring AMQP）

使用场景：
  所有消息通知的异步推送
  解耦核心业务和通知逻辑
  
版本选择：3.12-management（含管理界面）
管理界面：http://localhost:15672
```

---

## 五、AI技术选型

### 5.1 LangChain4j

```
选择理由：
  Java生态的RAG框架，与SpringBoot集成方便
  原生支持PGVector作为向量存储
  支持多种LLM API（智谱AI、通义千问等）
  满足课程进阶评分要求（Spring AI或LangChain4j 20分）

版本选择：0.27.x（当前稳定版）

主要功能使用：
  EmbeddingModel    → 文本向量化
  EmbeddingStore    → 向量存储（对接PGVector）
  ContentRetriever  → 相似度检索
  AiServices        → LLM调用封装
```

### 5.2 大模型API选择

```
主选：智谱AI GLM-4
  理由：
    国内访问稳定，无需VPN
    免费额度足够课程演示使用
    同时提供Embedding API
    LangChain4j有官方支持

备选：通义千问
  理由：
    阿里云产品，国内访问稳定
    免费额度充足
    LangChain4j支持

注意：
  API Key不要提交到代码仓库
  通过环境变量注入（application-prod.yml读取）
```

---

## 六、工程化技术选型

### 6.1 Maven

```
选择理由：
  Java生态最成熟的依赖管理工具
  课程要求（工程化基建10分）
  SpringBoot官方推荐

配置：
  镜像：阿里云maven镜像（加速国内下载）
  打包：mvn package -DskipTests
```

### 6.2 Docker + Docker Compose

```
选择理由：
  环境一致性：所有人本地环境完全一致
  一键启动：docker compose up -d 启动全部服务
  满足课程进阶评分要求（Docker 10分）

容器规划：
  nginx        → 前端静态资源 + 反向代理
  backend      → SpringBoot后端
  postgres     → 数据库
  redis        → 缓存
  rabbitmq     → 消息队列

  共5个容器，统一由docker-compose管理
```

### 6.3 Git + GitHub

```
选择理由：
  课程要求每人有提交记录
  Fork模式支持多人协作
  免费，无需自建GitLab

协作模式：
  Fork + Pull Request
  主仓库：负责人仓库
  个人仓库：各成员Fork

分支策略：
  main → 稳定版本
  dev  → 开发集成
  feature/xxx → 功能开发
```

---

## 七、技术依赖版本清单

### 7.1 前端 package.json 核心依赖

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "element-plus": "^2.6.0",
    "axios": "^1.6.0",
    "@element-plus/icons-vue": "^2.3.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "vite": "^5.0.0",
    "typescript": "^5.3.0",
    "vue-tsc": "^2.0.0",
    "@types/node": "^20.0.0",
    "eslint": "^8.57.0",
    "eslint-plugin-vue": "^9.23.0",
    "@typescript-eslint/eslint-plugin": "^7.0.0",
    "@typescript-eslint/parser": "^7.0.0",
    "prettier": "^3.2.0",
    "eslint-config-prettier": "^9.1.0"
  }
}
```

### 7.2 后端 pom.xml 核心依赖

```xml
<dependencies>
    <!-- SpringBoot核心 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
    </dependency>

    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- RabbitMQ -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Knife4j接口文档 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.4.0</version>
    </dependency>

    <!-- LangChain4j核心 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>0.27.1</version>
    </dependency>

    <!-- LangChain4j PGVector -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-pgvector</artifactId>
        <version>0.27.1</version>
    </dependency>

    <!-- LangChain4j 智谱AI -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-zhipu-ai</artifactId>
        <version>0.27.1</version>
    </dependency>
</dependencies>
```

---

## 八、各技术对应课程评分

| 技术 | 对应评分项 | 分值 |
|------|-----------|------|
| Vue3 前后端分离 | 架构纯洁 | 10分 |
| Controller/Service/Repository分层 | 分层规范 | 10分 |
| RESTful API + 统一返回体 | RESTful API与交互 | 15分 |
| PostgreSQL + 多表关联 | 数据持久化 | 15分 |
| Maven + Git | 工程化基建 | 10分 |
| Redis缓存 | 高并发与分布式 | 10分 |
| RabbitMQ异步消息 | 高并发与分布式 | 15分 |
| Redis+乐观锁并发控制 | 高并发与分布式 | 20分 |
| LangChain4j + RAG | 接入多模态大模型 | 20分 |
| Docker容器化 | 云端服务架构部署 | 10分 |
| Spring Security | 服务端工程素养 | 10分 |
| Git协作+接口文档+注释 | 服务端工程素养 | 10分 |

```
基础分：  60分（全拿）
进阶分：  95分（按上限60分计）
小团队系数：1.2
最终得分：(60 + 60) × 1.2 = 144 → 按满分120计
```