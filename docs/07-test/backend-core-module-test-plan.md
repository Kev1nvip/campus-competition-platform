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

> **测试策略说明**
>
> 认证模块分两层独立测试，覆盖正常流程与异常边界：
>
> | 层次 | 方式 | 说明 |
> |------|------|------|
> | **Service 单元测试** | JUnit 5 + Mockito，纯 Mock | 隔离 Repository/PasswordEncoder，验证业务逻辑分支 |
> | **Controller 集成测试** | `@WebMvcTest` + MockMvc + `@MockBean` | 验证 HTTP 路由、参数校验、JSON 序列化、异常响应格式 |
>
> **注意**：`Result.success()` 实际返回 `{"code": 0, "message": "success", "data": null}`；失败时返回对应业务错误码。

---

### 1.1 用户注册 (POST `/api/v1/auth/register`)

#### 接口约束（来自 `RegisterDTO`）

| 字段 | 约束 |
|------|------|
| `username` | 非空；4~64 位；仅字母、数字、下划线 |
| `password` | 非空；8~32 位；必须同时含字母和数字 |
| `realName` | 非空 |
| `role` | 非空；只允许 `STUDENT` 或 `TEACHER` |
| `studentNo` | 学生角色必填，且全局唯一 |
| `title` | 教师角色必填（如：讲师/副教授） |

#### 测试用例

| # | 场景 | 请求体要点 | 预期 HTTP 状态 | 预期响应 |
|---|------|-----------|---------------|---------|
| 1.1.1 | **学生正常注册** | 合法学生字段，studentNo 不重复 | 200 | `{"code": 0, "message": "success"}` |
| 1.1.2 | **教师正常注册** | 合法教师字段，包含 title | 200 | `{"code": 0, "message": "success"}` |
| 1.1.3 | 用户名已存在 | username 与库中重复 | 200* | `{"code": 40103, "message": "用户名已存在"}` |
| 1.1.4 | 学号已存在 | studentNo 与库中重复 | 200* | `{"code": 40104, "message": "学号已被注册"}` |
| 1.1.5 | 学生缺少 studentNo | role=STUDENT 但 studentNo 为空 | 200* | `{"code": 40001, "message": "学号不能为空"}` |
| 1.1.6 | 教师缺少 title | role=TEACHER 但 title 为空 | 200* | `{"code": 40001, "message": "职称不能为空"}` |
| 1.1.7 | 密码不含数字 | password="onlyletters" | 400 | `{"code": 40000, "message": "密码必须包含字母和数字"}` |
| 1.1.8 | 密码过短 | password="ab1" | 400 | `{"code": 40000, "message": "密码长度需在8-32位之间"}` |
| 1.1.9 | 用户名含特殊字符 | username="test@!" | 400 | `{"code": 40000, "message": "用户名只能包含字母、数字和下划线"}` |
| 1.1.10 | role 非法值 | role="GUEST" | 400 | `{"code": 40000, "message": "角色不合法"}` |
| 1.1.11 | 请求体为空 JSON | `{}` | 400 | `{"code": 40000, ...}` 包含多个校验错误 |

> *业务异常由 `GlobalExceptionHandler.handleBusinessException` 处理，HTTP 状态码为 200，靠 code 字段区分。

**最小成功示例（学生）**：
```json
{
  "username": "student_test01",
  "password": "password123",
  "realName": "张学生",
  "role": "STUDENT",
  "studentNo": "20240001",
  "department": "计算机学院",
  "email": "student@school.edu.cn"
}
```

**最小成功示例（教师）**：
```json
{
  "username": "teacher_test01",
  "password": "teacher123",
  "realName": "李老师",
  "role": "TEACHER",
  "title": "副教授",
  "department": "计算机学院"
}
```

---

### 1.2 用户登录 (POST `/api/v1/auth/login`)

#### 接口约束（来自 `LoginDTO`）

| 字段 | 约束 |
|------|------|
| `username` | 非空 |
| `password` | 非空 |

#### 测试用例

| # | 场景 | 请求体要点 | 预期 HTTP 状态 | 预期响应 |
|---|------|-----------|---------------|---------|
| 1.2.1 | **正常登录** | 存在的用户名 + 正确密码 | 200 | `{"code": 0, "data": {"token": "...", "tokenType": "Bearer", "expiresIn": 86400, "userInfo": {...}}}` |
| 1.2.2 | 用户名不存在 | username="no_such_user" | 200* | `{"code": 40101, "message": "用户名或密码错误"}` |
| 1.2.3 | 密码错误 | 正确用户名 + 错误密码 | 200* | `{"code": 40101, "message": "用户名或密码错误"}` |
| 1.2.4 | 账号被禁用 | status=DISABLED 的用户 | 200* | `{"code": 40102, "message": "账号已被禁用"}` |
| 1.2.5 | username 为空 | `{"username": "", "password": "xxx"}` | 400 | `{"code": 40000, "message": "用户名不能为空"}` |
| 1.2.6 | 请求体缺少 password 字段 | `{"username": "admin"}` | 400 | `{"code": 40000, "message": "密码不能为空"}` |
| 1.2.7 | 登录成功后验证 token 结构 | 正常登录，解析返回 token | — | token 包含 userId、role、username 三个声明，且未过期 |

**预期登录成功响应结构**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "<JWT>",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userInfo": {
      "userId": 1,
      "username": "student_test01",
      "realName": "张学生",
      "role": "STUDENT",
      "department": "计算机学院",
      "avatarUrl": null
    }
  }
}
```

---

## 二、 竞赛模块 (Competition)

> **测试策略说明**
>
> 竞赛模块采用与认证模块一致的“双层测试”策略，覆盖正常流程与异常边界：
>
> | 层次 | 方式 | 说明 |
> |------|------|------|
> | **Service 单元测试** | JUnit 5 + Mockito（含静态方法 Mock） | 隔离 Repository 与安全上下文，验证业务分支、状态流转与字段更新 |
> | **Controller 集成测试** | `@WebMvcTest` + MockMvc + `@MockBean` | 验证 HTTP 路由、参数绑定、JSON 序列化与异常返回结构 |
>
> **注意**：成功返回 `code=0`；业务异常通常由 `BusinessException` 返回 HTTP 200 + 业务 `code`；参数校验失败返回 HTTP 400 + `code=40000`。

---

### 2.1 获取竞赛分页列表 (GET `/api/v1/competitions`)

#### 请求参数

| 字段 | 说明 |
|------|------|
| `page` | 页码，默认 `1` |
| `size` | 每页条数，默认 `10` |
| `status` | 可选过滤（如 `SIGNING`） |
| `type` | 可选过滤（`INDIVIDUAL` / `TEAM`） |
| `keyword` | 可选标题关键字 |

#### 测试用例

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| 2.1.1 | **正常分页查询** | `page=1,size=10,status=SIGNING,type=INDIVIDUAL` | HTTP 200，`code=0`，返回 `PageVO` 结构与 `list` |
| 2.1.2 | 默认分页参数 | 不传 `page,size` | HTTP 200，`code=0`，返回默认分页结果 |

---

### 2.2 获取竞赛详情 (GET `/api/v1/competitions/{id}`)

#### 测试用例

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| 2.2.1 | **查询存在的竞赛** | `id=1` | HTTP 200，`code=0`，`data` 为竞赛完整对象 |
| 2.2.2 | 竞赛不存在 | `id=9999` | HTTP 200，`code=40400`，`message=竞赛不存在` |
| 2.2.3 | 已下架且非管理员非创建人访问（Service） | `status=OFFLINE` | 抛业务异常，`code=40400` |

---

### 2.3 发布竞赛 (POST `/api/v1/competitions`)

#### 接口约束（来自 `CompetitionSaveDTO` + 业务规则）

| 字段 | 约束 |
|------|------|
| `title` | 非空 |
| `type` | 非空（`INDIVIDUAL` / `TEAM`） |
| `organizer` | 非空 |
| `signupStart` | 非空 |
| `signupEnd` | 非空，且必须晚于 `signupStart` |
| `hasQuota` | 非空 |
| `maxQuota` | 当 `hasQuota=true` 时必须 `>=1` |
| `minTeamSize` | 当 `type=TEAM` 时必须 `>=2` |

#### 测试用例

| # | 场景 | 输入要点 | 预期 |
|---|------|---------|------|
| 2.3.1 | **个人赛正常发布** | 合法字段 | HTTP 200，`code=0`，返回新建 `id` |
| 2.3.2 | 重复发布 | 标题/类型/主办方/开始时间重复且未下架 | `code=40124` |
| 2.3.3 | 报名时间非法 | `signupEnd < signupStart` | `code=40000` |
| 2.3.4 | 配额非法 | `hasQuota=true` 且 `maxQuota` 缺失或 `<1` | `code=40000` |
| 2.3.5 | 团队赛最小人数非法 | `type=TEAM,minTeamSize<2` | `code=40000` |
| 2.3.6 | 请求体缺必填字段 | 例如 `title` 为空 | HTTP 400，`code=40000` |

**最小成功示例（个人赛）**：
```json
{
  "title": "蓝桥杯全国软件大赛",
  "type": "INDIVIDUAL",
  "organizer": "工信部",
  "signupStart": "2026-05-01T00:00:00+08:00",
  "signupEnd": "2026-06-01T00:00:00+08:00",
  "competitionStart": "2026-06-15T00:00:00+08:00",
  "competitionEnd": "2026-06-30T00:00:00+08:00",
  "hasQuota": true,
  "maxQuota": 100,
  "description": "算法大赛"
}
```

---

### 2.4 修改竞赛 (PUT `/api/v1/competitions/{id}`)

#### 测试用例

| # | 场景 | 输入要点 | 预期 |
|---|------|---------|------|
| 2.4.1 | **正常修改** | 存在竞赛 + 合法字段 | HTTP 200，`code=0` |
| 2.4.2 | 竞赛不存在 | `id` 不存在 | `code=40400` |
| 2.4.3 | 已结束竞赛不可编辑 | `status=FINISHED` | `code=40132` |
| 2.4.4 | 请求体校验失败 | 必填字段缺失 | HTTP 400，`code=40000` |

---

### 2.5 变更竞赛状态 (PATCH `/api/v1/competitions/{id}/status`)

#### 请求体示例
```json
{"action":"OFFLINE"}
```

或

```json
{"action":"RESTORE"}
```

#### 测试用例

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| 2.5.1 | 下架竞赛 | `action=OFFLINE` | HTTP 200，`code=0`，状态变为 `OFFLINE` |
| 2.5.2 | 恢复竞赛 | `action=RESTORE` | HTTP 200，`code=0`，按当前时间恢复为计算状态（如 `SIGNING`） |

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
