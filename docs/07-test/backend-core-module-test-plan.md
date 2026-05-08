# 后端核心模块测试方案

> 文档版本：v1.0
> 测试对象：项目负责人（@Kev1nvip）所负责的后端核心模块、AI模块及环境搭建
> 测试范围：全接口级测试 + 业务功能级测试 + 并发控制压测

---

## 一、测试环境说明
1. **基础设施**：通过 `docker-compose up -d` 启动所需的中间件（PostgreSQL + PGVector, Redis, RabbitMQ）。
2. **后端服务**：在本地启动 SpringBoot 服务，端口 8080。
3. **测试工具**：
   - 接口测试：Postman / Apifox / Knife4j (http://localhost:8080/doc.html)
   - 并发测试：JMeter
   - 数据库验证：Navicat / DBeaver / Redis Desktop Manager

---

## 一、 认证模块 (Auth)

### 1.1 用户注册 (POST)
- **URL**: `/api/v1/auth/register`
- **最小成功示例**:
```json
{
  "username": "student_001",
  "password": "password123",
  "realName": "张学生",
  "role": "STUDENT",
  "studentNo": "20240001",
  "department": "计算机学院",
  "email": "student@school.edu.cn"
}
```
- **预期结果**: `200 OK`，`{"code": 200, "message": "操作成功"}`。

### 1.2 用户登录 (POST)
- **URL**: `/api/v1/auth/login`
- **最小成功示例**:
```json
{
  "username": "admin",
  "password": "admin123456"
}
```
- **预期结果**: 返回 `token` 和 `tokenType: "Bearer"`。

---

## 二、 竞赛模块 (Competition)

### 2.1 获取竞赛分页列表 (GET)
- **URL**: `/api/v1/competitions?page=1&size=10&type=INDIVIDUAL&status=SIGNING`
- **预期结果**: 返回 `PageVO` 结构，包含 `list` 数组。

### 2.2 获取竞赛详情 (GET)
- **URL**: `/api/v1/competitions/{id}`
- **预期结果**: 返回竞赛完整字段（标题、名额、起止时间等）。

### 2.3 发布竞赛 (POST) - [需 ADMIN 权限]
- **URL**: `/api/v1/competitions`
- **最小成功示例 (个人赛)**:
```json
{
  "title": "蓝桥杯全国软件大赛",
  "type": "INDIVIDUAL",
  "organizer": "工信部",
  "signupStart": "2026-05-01T00:00:00+08:00",
  "signupEnd": "2026-06-01T00:00:00+08:00",
  "hasQuota": true,
  "maxQuota": 100,
  "status": "SIGNING",
  "description": "算法大赛"
}
```
- **预期结果**: 数据库 `competition` 表新增记录，Redis 初始化名额。

### 2.4 修改竞赛 (PUT) - [需 ADMIN 权限]
- **URL**: `/api/v1/competitions/{id}`
- **示例**: 修改标题或调整名额。

---

## 三、 报名模块 (Signup)

### 3.1 个人赛报名草稿 (POST)
- **URL**: `/api/v1/signups/individual`
- **最小成功示例**:
```json
{
  "competitionId": 1,
  "teacherId": 2,
  "motivation": "希望能通过比赛提升算法能力",
  "introduction": "掌握 Java 和数据结构"
}
```

### 3.2 提交个人赛审核 (POST)
- **URL**: `/api/v1/signups/individual/{id}/submit`
- **Payload**: `{"attachmentUrl": "http://oss.com/cert.pdf"}`

### 3.3 我的个人赛记录 (GET)
- **URL**: `/api/v1/signups/individual/my?status=DRAFT`

### 3.4 创建团队赛草稿 (POST)
- **URL**: `/api/v1/signups/team`
- **Payload**: `{"teamId": 101}`

### 3.5 提交团队赛审核 (POST)
- **URL**: `/api/v1/signups/team/{id}/submit`

---

## 四、 AI 推荐模块 (AI)

### 4.1 智能推荐 (POST)
- **URL**: `/api/v1/ai/recommend`
- **最小成功示例**:
```json
{
  "prompt": "我是大一学生，擅长 Python 和数学建模，请推荐适合我的竞赛"
}
```
- **预期结果**: 返回 AI 生成的 Markdown 文本建议。

### 4.2 手动刷新知识库 (POST)
- **URL**: `/api/v1/ai/knowledge/refresh`
- **预期结果**: 返回 "知识库刷新任务已启动"，异步清理并重构向量索引。

---

## 五、 系统与异常验证

### 5.1 全局异常处理验证
- **测试动作**: 访问 `/api/v1/auth/login` 时传入错误的密码。
- **预期结果**: `401 Unauthorized`，返回 `{"code": 40101, "message": "用户名或密码错误"}`。

---

## 六、 并发控制压测 (JMeter)
- **压测点 1 (名额扣减)**: 并发调用 `/api/v1/signups/individual`，观察 Redis 名额是否出现负数。
- **压测点 2 (乐观锁)**: 同时并发更新同一条竞赛记录，验证是否有请求报 `ObjectOptimisticLockingFailureException`。
