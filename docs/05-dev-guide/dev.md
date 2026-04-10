# docker
## 只启动中间件（开发阶段不启动backend和nginx）
```text
打开 CMD，进入项目根目录：
cd D:\Desktop\campus-competition-platform

执行：
docker compose up postgres redis rabbitmq -d
```

## 验证中间件启动成功
```text
# 查看容器状态
docker compose ps

# 正常输出应该是：
NAME                      STATUS
competition-postgres      running
competition-redis         running
competition-rabbitmq      running

# 验证 Redis
docker exec -it competition-redis redis-cli -a redis123 ping
# 输出 PONG 即成功

# 验证 RabbitMQ（浏览器访问）
# http://localhost:15672
# 用户名：competition
# 密码：competition123
# 能看到管理界面即成功
```

# 启动后端
```text
1. 打开 backend 目录
2. 等待 Maven 下载依赖（第一次需要几分钟）
3. 点击运行按钮

或者在终端执行：
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

后端启动成功的标志：
  控制台看到：Started BackendApplication in x.xxx seconds
  访问：http://localhost:8080/doc.html
  能看到 Knife4j 接口文档界面
```

# 启动前端
```text
cd frontend
npm install
npm run dev

# 成功输出：
# VITE v5.x.x  ready in xxx ms
# ➜  Local:   http://localhost:5173/
```


