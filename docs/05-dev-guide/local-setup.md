# 本地开发环境搭建指南

> 适用系统：Windows 10/11
> 最后更新：2026-03
> 维护人：负责人

按照本文档操作，预计 1-2 小时完成所有配置。
遇到问题先看文末【常见问题】，解决不了问AI。

---

## 一、必装软件清单

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| Git | 最新版 | 代码版本管理 |
| Node.js | 18.x 或 20.x | 前端运行环境 |
| Java | 17 或 21 | 后端运行环境 |
| Docker Desktop | 最新版 | 运行中间件 |
| VS Code | 最新版 | 开发工具 |

---

## 二、安装 Git

### 下载安装

```
1. 访问：https://git-scm.com/download/win
2. 点击 "Click here to download" 下载安装包
3. 双击安装，全部默认选项，一路 Next
4. 安装完成
```

### 验证安装

```bash
# 打开 CMD 或 PowerShell，输入：
git --version
# 看到版本号即成功，例如：git version 2.43.0.windows.1
```

### 配置用户信息

```bash
# 替换为你自己的信息
git config --global user.name "你的姓名"
git config --global user.email "你的GitHub注册邮箱"

# 验证配置
git config --global --list
```

### 配置 SSH 密钥

```bash
# Step1：生成密钥
ssh-keygen -t ed25519 -C "你的GitHub注册邮箱"
# 连按3次回车，使用默认配置

# Step2：查看公钥
type C:\Users\你的用户名\.ssh\id_ed25519.pub
# 复制输出的全部内容（从ssh-ed25519开始到邮箱结束）

# Step3：添加到GitHub
# 浏览器打开：GitHub → Settings → SSH and GPG keys
# → New SSH key
# Title：我的电脑
# Key：粘贴刚才复制的内容
# → Add SSH key

# Step4：验证
ssh -T git@github.com
# 成功输出：Hi 用户名! You've successfully authenticated.
```

---

## 三、安装 Node.js

### 下载安装

```
1. 访问：https://nodejs.org/zh-cn
2. 点击 "20.x.x LTS" 下载（选LTS长期支持版）
3. 双击安装，全部默认选项
4. 安装完成
```

### 验证安装

```bash
node --version
# 输出：v20.x.x

npm --version
# 输出：10.x.x
```

### 配置 npm 镜像（加速下载）

```bash
npm config set registry https://registry.npmmirror.com

# 验证
npm config get registry
# 输出：https://registry.npmmirror.com
```

---

## 四、安装 Java 21

### 下载安装

```
1. 访问：https://adoptium.net/zh-CN/temurin/releases/
2. 筛选条件：
   Version：21
   OS：Windows
   Architecture：x64
   Package Type：JDK
3. 下载 .msi 安装包
4. 双击安装
   ✅ 勾选 "Set JAVA_HOME variable"
   ✅ 勾选 "JavaSoft registry keys"
   其余默认，一路 Next
```

### 验证安装

```bash
java --version
# 输出：openjdk 21.x.x ...

javac --version
# 输出：javac 21.x.x
```

### 如果验证失败（找不到命令）

```
手动配置环境变量：

1. 右键"此电脑" → 属性 → 高级系统设置 → 环境变量
2. 系统变量 → 新建：
   变量名：JAVA_HOME
   变量值：C:\Program Files\Eclipse Adoptium\jdk-21.x.x（你的安装路径）
3. 找到系统变量 Path → 编辑 → 新建：
   %JAVA_HOME%\bin
4. 确定保存
5. 重新打开 CMD 验证
```

---

## 五、安装 Docker Desktop

### 下载安装

```
1. 访问：https://www.docker.com/products/docker-desktop/
2. 点击 "Download for Windows" 下载
3. 双击安装
   安装时勾选：
   ✅ Use WSL 2 instead of Hyper-V（推荐）
4. 安装完成后重启电脑
5. 重启后 Docker Desktop 自动启动
   等待右下角 Docker 图标变为绿色（Running状态）
```

### 验证安装

```bash
docker --version
# 输出：Docker version 24.x.x

docker compose version
# 输出：Docker Compose version v2.x.x
```

### 配置镜像加速（重要，否则拉取镜像极慢）

```
1. 打开 Docker Desktop
2. 右上角 Settings（齿轮图标）
3. Docker Engine
4. 在 JSON 配置中添加：
```

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
```

```
5. 点击 "Apply & restart"
6. 等待 Docker 重启完成
```

---

## 六、安装 VS Code 及插件

### 下载安装

```
1. 访问：https://code.visualstudio.com/
2. 点击 "Download for Windows" 下载
3. 双击安装
   ✅ 勾选"添加到PATH"
   ✅ 勾选"添加到右键菜单"
```

### 必装插件

```
打开 VS Code
左侧点击插件图标（四个方块）
搜索并安装以下插件：
```

**前端插件**

| 插件名 | 用途 |
|--------|------|
| Vue - Official | Vue3 语法支持 |
| ESLint | 代码规范检查 |
| Prettier - Code formatter | 代码格式化 |
| Auto Rename Tag | 自动重命名标签 |
| Path Intellisense | 路径自动补全 |

**后端插件**

| 插件名 | 用途 |
|--------|------|
| Extension Pack for Java | Java开发全套 |
| Spring Boot Extension Pack | SpringBoot支持 |
| Maven for Java | Maven项目管理 |
| Lombok Annotations Support | Lombok支持 |

**通用插件**

| 插件名 | 用途 |
|--------|------|
| GitLens | Git历史查看 |
| Docker | Docker文件支持 |
| YAML | YAML文件支持 |
| REST Client | 接口测试（替代Postman）|

---

## 七、克隆项目到本地

```bash
# 在你想存放项目的目录打开 CMD
# 例如放在 D:\Projects

cd D:\
mkdir Projects
cd Projects

# 克隆你Fork的仓库（注意是你自己Fork的，不是负责人的）
git clone git@github.com:你的GitHub用户名/campus-competition-platform.git

# 进入项目目录
cd campus-competition-platform

# 添加负责人仓库为 upstream
git remote add upstream git@github.com:负责人GitHub用户名/campus-competition-platform.git

# 验证remote配置
git remote -v
# 应看到 origin 和 upstream 两个地址
```

---

## 八、启动中间件（Docker）

### 查看 docker-compose.yml

项目根目录已有 docker-compose.yml，内容如下：

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
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

  redis:
    image: redis:7.2-alpine
    container_name: competition-redis
    ports:
      - "6379:6379"
    command: redis-server --requirepass redis123
    restart: unless-stopped

  rabbitmq:
    image: rabbitmq:3.12-management
    container_name: competition-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: competition
      RABBITMQ_DEFAULT_PASS: competition123
    ports:
      - "5672:5672"    # 应用连接端口
      - "15672:15672"  # 管理界面端口
    restart: unless-stopped

volumes:
  postgres_data:
```

### 启动命令

```bash
# 在项目根目录执行

# 首次启动（拉取镜像，需要等待3-5分钟）
docker compose up -d

# 查看启动状态（三个服务都是 running 即成功）
docker compose ps

# 查看日志（如果启动失败时用）
docker compose logs
```

### 验证中间件启动成功

```bash
# 验证 PostgreSQL
docker exec -it competition-postgres psql -U competition -d campus_competition -c "\dt"
# 能看到表列表即成功

# 验证 Redis
docker exec -it competition-redis redis-cli -a redis123 ping
# 输出 PONG 即成功

# 验证 RabbitMQ
# 浏览器访问：http://localhost:15672
# 用户名：competition
# 密码：competition123
# 能看到管理界面即成功
```

### 常用 Docker 命令

```bash
# 停止所有中间件
docker compose stop

# 启动所有中间件
docker compose start

# 重启所有中间件
docker compose restart

# 停止并删除容器（数据不丢失，因为用了volume）
docker compose down

# 停止并删除容器和数据（慎用）
docker compose down -v

# 查看容器状态
docker compose ps

# 查看某个服务的日志
docker compose logs postgres
docker compose logs redis
docker compose logs rabbitmq
```

---

## 九、启动后端

### 配置 application-dev.yml

```
backend/src/main/resources/ 目录下
复制 application-dev.yml.example 为 application-dev.yml
（application-dev.yml 已加入 .gitignore，不会提交到仓库）
```

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/campus_competition
    username: competition
    password: competition123
    driver-class-name: org.postgresql.Driver

  data:
    redis:
      host: localhost
      port: 6379
      password: redis123
      database: 0

  rabbitmq:
    host: localhost
    port: 5672
    username: competition
    password: competition123
    virtual-host: /

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

jwt:
  secret: campus-competition-jwt-secret-key-2024
  expiration: 7200
```

### VS Code 启动后端

```
方式1：使用 Spring Boot Dashboard（推荐）
  1. VS Code 左侧找到 Spring Boot Dashboard 图标
  2. 找到 backend 项目
  3. 点击运行按钮（▶）

方式2：使用终端
  cd backend
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

方式3：直接运行主类
  打开 BackendApplication.java
  点击 main 方法上方的 "Run" 按钮
```

### 验证后端启动成功

```bash
# 浏览器访问接口文档
http://localhost:8080/doc.html
# 能看到 Knife4j 接口文档界面即成功

# 测试健康检查接口
http://localhost:8080/actuator/health
# 返回 {"status":"UP"} 即成功
```

---

## 十、启动前端

```bash
# 进入前端目录
cd frontend

# 安装依赖（首次需要，后续不用）
npm install

# 启动开发服务器
npm run dev

# 看到以下输出即成功：
#   VITE v5.x.x  ready in xxx ms
#   ➜  Local:   http://localhost:5173/
#   ➜  Network: use --host to expose
```

```
浏览器访问：http://localhost:5173
能看到页面即成功
```

### 配置前端环境变量

```bash
# frontend 目录下
# 复制 .env.example 为 .env.local
# .env.local 已加入 .gitignore，不会提交到仓库
```

```ini
# .env.local
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=校园学术竞赛管理平台
```

---

## 十一、验证完整链路

完成以上步骤后，验证整体环境是否正常：

```
检查清单：
□ docker compose ps 显示三个服务都是 running
□ 后端启动无报错，http://localhost:8080/doc.html 可访问
□ 前端启动无报错，http://localhost:5173 可访问
□ 前端能正常调用后端接口（登录接口测试）
□ git remote -v 能看到 origin 和 upstream
```

---

## 十二、日常开发启动顺序

```
每次开发前按以下顺序启动：

Step1：启动 Docker Desktop
  等待右下角图标变绿

Step2：启动中间件
  cd campus-competition-platform
  docker compose start

Step3：启动后端
  cd backend
  VS Code Spring Boot Dashboard 点击运行

Step4：启动前端
  cd frontend
  npm run dev

Step5：开始开发
```

---

## 十三、常见问题

### Docker Desktop 无法启动

```
原因：WSL2 未安装或版本过低

解决：
1. 以管理员身份打开 PowerShell
2. 执行：wsl --update
3. 重启电脑
4. 重新启动 Docker Desktop
```

### 端口被占用

```
报错：Bind for 0.0.0.0:5432 failed: port is already allocated

解决：
1. 查看占用端口的进程
   netstat -ano | findstr :5432

2. 结束对应进程
   taskkill /PID 进程ID /F

3. 重新启动容器
   docker compose start
```

### Maven 下载依赖慢

```
配置国内镜像：

找到或创建 Maven settings.xml：
C:\Users\你的用户名\.m2\settings.xml

添加以下内容：
```

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### npm install 失败

```
解决：
1. 确认镜像已配置
   npm config get registry
   应输出：https://registry.npmmirror.com

2. 清除缓存重试
   npm cache clean --force
   npm install

3. 如果还是失败，删除 node_modules 重试
   rmdir /s /q node_modules
   npm install
```

### 后端启动报数据库连接失败

```
报错：Connection refused: localhost/127.0.0.1:5432

原因：PostgreSQL 容器未启动

解决：
1. 确认 Docker Desktop 已启动（图标为绿色）
2. docker compose ps 查看容器状态
3. 如果容器未运行：docker compose start
4. 等待 10 秒后重新启动后端
```

### SSH 克隆报错 Permission denied

```
原因：SSH 密钥未正确配置

解决：
1. 确认密钥已生成
   type C:\Users\你的用户名\.ssh\id_ed25519.pub

2. 确认公钥已添加到 GitHub
   GitHub → Settings → SSH keys 查看

3. 重新验证
   ssh -T git@github.com
```

---

## 附：所有服务访问地址汇总

| 服务 | 地址 | 账号 | 密码 |
|------|------|------|------|
| 前端 | http://localhost:5173 | - | - |
| 后端接口 | http://localhost:8080 | - | - |
| Knife4j文档 | http://localhost:8080/doc.html | - | - |
| RabbitMQ管理台 | http://localhost:15672 | competition | competition123 |
| PostgreSQL | localhost:5432 | competition | competition123 |
| Redis | localhost:6379 | - | redis123 |