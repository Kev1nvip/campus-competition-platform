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

> **测试策略说明**
>
> 报名模块采用与前两部分一致的“双层测试”策略，覆盖个人赛与团队赛的核心流程、状态机约束和并发回滚逻辑：
>
> | 层次 | 方式 | 说明 |
> |------|------|------|
> | **Service 单元测试** | JUnit 5 + Mockito（含静态方法 Mock） | 隔离 Repository/RedisService/SecurityUtil，验证业务分支与回滚逻辑 |
> | **Controller 集成测试** | `@WebMvcTest` + MockMvc + `@MockBean` | 验证 HTTP 路由、参数校验、JSON 响应结构与业务异常映射 |
>
> **注意**：业务异常通常返回 HTTP 200 + 业务 `code`；参数校验失败返回 HTTP 400 + `code=40000`。

---

### 3.1 个人赛报名草稿 (POST `/api/v1/signups/individual`)

#### 接口约束（来自 `IndividualSignupDTO` + 业务规则）

| 字段 | 约束 |
|------|------|
| `competitionId` | 非空，且竞赛必须存在 |
| `teacherId` | 非空，且必须是 `TEACHER` 角色 |
| `competition.status` | 必须为 `SIGNING` |
| 重复报名 | 同一学生同一竞赛不允许重复创建 |
| 配额/教师名额 | 受 Redis 并发控制，满额时报业务异常 |

#### 测试用例

| # | 场景 | 输入要点 | 预期 |
|---|------|---------|------|
| 3.1.1 | **个人赛报名成功** | 合法 `competitionId/teacherId`，竞赛在报名中，名额充足 | HTTP 200，`code=0`，返回 `signupId` 与 `status=DRAFT` |
| 3.1.2 | 竞赛不存在 | `competitionId` 不存在 | `code=40120` |
| 3.1.3 | 竞赛不在报名期 | `status!=SIGNING` | `code=40121` |
| 3.1.4 | 重复报名 | 同一学生重复报名同一竞赛 | `code=40130` |
| 3.1.5 | 指导老师不存在 | `teacherId` 不存在 | `code=40105` |
| 3.1.6 | 指导老师角色非法 | teacher 角色不是 `TEACHER` | `code=40000` |
| 3.1.7 | 老师带队名额已满 | Redis 返回教师额度满 | `code=40133` |
| 3.1.8 | 竞赛名额已满 | Redis 返回竞赛配额满 | `code=40122` |
| 3.1.9 | 请求体缺必填字段 | `competitionId` 或 `teacherId` 为空 | HTTP 400，`code=40000` |

**最小成功示例（个人赛报名草稿）**：
```json
{
  "competitionId": 1,
  "teacherId": 2,
  "motivation": "希望能通过比赛提升算法能力",
  "introduction": "掌握 Java 和数据结构"
}
```

---

### 3.2 提交个人赛审核 (POST `/api/v1/signups/individual/{id}/submit`)

#### 接口约束（业务规则）

| 字段 | 约束 |
|------|------|
| `id` | 报名记录必须存在 |
| 当前用户 | 必须是报名学生本人 |
| `signup.status` | 仅 `DRAFT` / `REJECTED` 允许提交 |
| 指导申请状态 | `ApplyRecord.status` 必须为 `APPROVED` |

#### 测试用例

| # | 场景 | 输入要点 | 预期 |
|---|------|---------|------|
| 3.2.1 | **提交成功** | 本人提交，状态合法，指导申请已通过 | HTTP 200，`code=0`，状态变为 `PENDING` |
| 3.2.2 | 报名记录不存在 | `id` 不存在 | `code=40131` |
| 3.2.3 | 状态不允许提交 | 例如当前状态为 `PENDING` | `code=40132` |
| 3.2.4 | 未找到指导申请 | 缺失 `INDIVIDUAL_GUIDE` 记录 | `code=40000` |
| 3.2.5 | 指导申请未通过 | 申请状态非 `APPROVED` | `code=40000` |

---

### 3.3 我的个人赛记录 (GET `/api/v1/signups/individual/my`)

#### 请求参数

| 字段 | 说明 |
|------|------|
| `page` | 页码，默认 `1` |
| `size` | 每页条数，默认 `10` |
| `status` | 可选过滤（如 `DRAFT`） |

#### 测试用例

| # | 场景 | 输入 | 预期 |
|---|------|------|------|
| 3.3.1 | **分页查询成功（无状态过滤）** | `page=1,size=10` | HTTP 200，`code=0`，返回 `PageVO` |
| 3.3.2 | 分页查询成功（按状态过滤） | `status=DRAFT` | HTTP 200，`code=0`，仅返回对应状态记录 |

---

### 3.4 创建团队赛报名草稿 (POST `/api/v1/signups/team`)

#### 接口约束（业务规则）

| 字段 | 约束 |
|------|------|
| `teamId` | 队伍必须存在 |
| 当前用户 | 必须是队长本人 |
| `teacherConfirmed` | 必须为 `true` |
| 重复报名 | 同一队伍同一竞赛仅允许一条报名记录 |

#### 测试用例

| # | 场景 | 输入要点 | 预期 |
|---|------|---------|------|
| 3.4.1 | **团队赛草稿创建成功** | 队伍存在、队长本人、老师已确认 | HTTP 200，`code=0`，返回 `signupId` 与 `status=DRAFT` |
| 3.4.2 | 队伍不存在 | `teamId` 不存在 | `code=40140` |
| 3.4.3 | 指导老师未确认 | `teacherConfirmed=false` | `code=40145` |
| 3.4.4 | 重复创建草稿 | 已存在 team signup | `code=40130` |

**最小成功示例（团队赛报名草稿）**：
```json
{
  "teamId": 101
}
```

---

### 3.5 提交团队赛审核 (POST `/api/v1/signups/team/{id}/submit`)

#### 接口约束（业务规则）

| 字段 | 约束 |
|------|------|
| `id` | 团队报名记录必须存在 |
| 当前用户 | 必须是该队伍队长 |
| 队伍人数 | `memberCount >= competition.minTeamSize` |

#### 测试用例

| # | 场景 | 输入要点 | 预期 |
|---|------|---------|------|
| 3.5.1 | **提交成功** | 报名记录存在，队长本人，队伍人数达标 | HTTP 200，`code=0`，报名状态变 `PENDING`，队伍状态变 `SUBMITTED` |
| 3.5.2 | 报名记录不存在 | `id` 不存在 | `code=40131` |
| 3.5.3 | 队伍人数不足 | `memberCount < minTeamSize` | `code=40000` |

---

## 四、 AI 推荐模块 (AI)

> **测试策略说明**
>
> AI 推荐模块依赖外部 LLM 服务（硅基流动）和 PGVector 向量数据库，测试时需完全隔离这些外部依赖：
>
> | 层次 | 方式 | 说明 |
> |------|------|------|
> | **Service 单元测试** | JUnit 5 + Mockito | Mock `AiAssistant`，验证 `recommend()` 方法的调用链与返回值传递 |
> | **Controller 集成测试** | `@WebMvcTest` + MockMvc + `@MockBean` | Mock `AiService` 与 `KnowledgeBaseServiceImpl`，验证 HTTP 路由、请求体解析、响应结构 |
>
> **注意**：`AiController` 接收 `Map<String, String>` 请求体，取 `prompt` 字段传入 Service；`KnowledgeBaseServiceImpl` 的 `refreshKnowledgeBase()` 为 void 方法，测试时验证其被调用即可。

---

### 4.1 智能推荐 (POST `/api/v1/ai/recommend`)

#### 请求体

| 字段 | 约束 |
|------|------|
| `prompt` | 用户描述文本，Controller 层直接从 Map 中取值，无 Bean Validation |

#### 测试用例

| # | 场景 | 输入要点 | 预期 HTTP 状态 | 预期响应 |
|---|------|---------|---------------|---------|
| 4.1.1 | **正常推荐请求** | `{"prompt": "我是大一学生，擅长 Python 和数学建模，请推荐适合我的竞赛"}` | 200 | `{"code": 0, "data": "<AI 生成的 Markdown 文本>"}` |
| 4.1.2 | prompt 为空字符串 | `{"prompt": ""}` | 200 | `{"code": 0, "data": "<AI 返回内容>"}` （Controller 不校验，透传给 Service） |
| 4.1.3 | 请求体缺少 prompt 字段 | `{}` | 200 | `{"code": 0, "data": null}` （`map.get("prompt")` 返回 null，透传给 Service） |

**最小成功示例**：
```json
{
  "prompt": "我是大一学生，擅长 Python 和数学建模，请推荐适合我的竞赛"
}
```

**预期成功响应结构**：
```json
{
  "code": 0,
  "message": "success",
  "data": "## 推荐竞赛\n\n根据您的背景，推荐以下竞赛：..."
}
```

---

### 4.2 手动刷新知识库 (POST `/api/v1/ai/knowledge/refresh`)

#### 测试用例

| # | 场景 | 输入要点 | 预期 HTTP 状态 | 预期响应 |
|---|------|---------|---------------|---------|
| 4.2.1 | **正常刷新请求** | 无请求体 | 200 | `{"code": 0, "data": "知识库刷新任务已启动"}` |
| 4.2.2 | 验证 Service 方法被调用 | 无请求体 | 200 | `knowledgeBaseService.refreshKnowledgeBase()` 被调用一次 |

---

### 4.3 Service 层单元测试

#### AiServiceImpl 测试用例

| # | 场景 | Mock 行为 | 预期 |
|---|------|----------|------|
| 4.3.1 | **recommend 正常返回** | `assistant.chat(prompt)` 返回固定字符串 | `aiService.recommend(prompt)` 返回相同字符串 |
| 4.3.2 | prompt 为 null | `assistant.chat(null)` 返回空字符串 | `aiService.recommend(null)` 返回空字符串 |

---

## 五、 系统与异常验证

> **测试策略说明**
>
> 本节验证 `GlobalExceptionHandler` 对各类异常的统一处理行为，确保所有模块的异常响应格式一致。
>
> | 层次 | 方式 | 说明 |
> |------|------|------|
> | **Handler 集成测试** | `@WebMvcTest` + MockMvc + `@MockBean` | 通过一个最小 Controller 触发各类异常，验证 HTTP 状态码与响应体格式 |
>
> **注意**：业务异常（`BusinessException`）HTTP 状态码为 **200**，靠 `code` 字段区分；参数校验失败返回 **400**；认证失败返回 **401**；权限不足返回 **403**；兜底异常返回 **500**。

---

### 5.1 业务异常处理

| # | 场景 | 触发方式 | 预期 HTTP 状态 | 预期响应 |
|---|------|---------|---------------|---------|
| 5.1.1 | **密码错误（业务异常）** | `authService.login()` 抛 `BusinessException(40101, "用户名或密码错误")` | 200 | `{"code": 40101, "message": "用户名或密码错误"}` |
| 5.1.2 | 账号被禁用（业务异常） | `authService.login()` 抛 `BusinessException(40102, "账号已被禁用")` | 200 | `{"code": 40102, "message": "账号已被禁用"}` |

---

### 5.2 参数校验异常处理（`@Valid` / `@Validated`）

| # | 场景 | 触发方式 | 预期 HTTP 状态 | 预期响应 |
|---|------|---------|---------------|---------|
| 5.2.1 | **必填字段缺失** | 登录请求体 `{}` | 400 | `{"code": 40000, "message": "<校验错误信息>"}` |
| 5.2.2 | 字段格式不合法 | 注册请求 `username="test@!"` | 400 | `{"code": 40000, "message": "用户名只能包含字母、数字和下划线"}` |

---

### 5.3 认证与权限异常处理

| # | 场景 | 触发方式 | 预期 HTTP 状态 | 预期响应 |
|---|------|---------|---------------|---------|
| 5.3.1 | **未登录 / Token 无效** | `GlobalExceptionHandler` 处理 `AuthenticationException` | 401 | `{"code": 40100, "message": "未登录或Token已过期"}` |
| 5.3.2 | 权限不足 | `GlobalExceptionHandler` 处理 `AccessDeniedException` | 403 | `{"code": 40300, "message": "无操作权限"}` |

---

### 5.4 兜底异常处理

| # | 场景 | 触发方式 | 预期 HTTP 状态 | 预期响应 |
|---|------|---------|---------------|---------|
| 5.4.1 | **未预期的运行时异常** | Service 抛 `RuntimeException("数据库连接失败")` | 500 | `{"code": 50000, "message": "服务器内部错误：数据库连接失败"}` |

---

## 六、 并发控制测试

> **测试策略说明**
>
> 原方案为 JMeter 手工压测，改为自动化单元测试，通过 `CountDownLatch` + `ExecutorService` 模拟多线程并发，在不依赖外部环境的前提下验证并发控制逻辑的正确性。
>
> | 层次 | 方式 | 说明 |
> |------|------|------|
> | **Lua 脚本逻辑测试** | JUnit 5 + Mockito | Mock `RedisTemplate`，直接测试 `RedisService` 的 Lua 脚本返回值语义（-1/-2/正常扣减） |
> | **并发名额扣减测试** | JUnit 5 + `CountDownLatch` + `ExecutorService` | 多线程并发调用 `signUpIndividual()`，Mock Redis 返回 -2（名额不足），验证超额线程抛 `COMPETITION_QUOTA_FULL` 且 Redis 回滚被调用 |
> | **乐观锁冲突测试** | JUnit 5 + Mockito | Mock `competitionRepository.saveAndFlush()` 抛 `ObjectOptimisticLockingFailureException`，验证 Service 捕获后抛 `CONFLICT` 业务异常并回滚 Redis |

---

### 6.1 Redis Lua 脚本语义测试（`RedisService`）

| # | 场景 | Mock `redisTemplate.execute()` 返回值 | 预期 `decrCompetitionQuota()` 返回 |
|---|------|--------------------------------------|----------------------------------|
| 6.1.1 | **名额充足，正常扣减** | `5L`（剩余 5） | `5L` |
| 6.1.2 | **名额不足** | `-2L` | `-2L` |
| 6.1.3 | **缓存 Key 不存在** | `-1L` | `-1L` |
| 6.1.4 | 老师带队未超限 | `1L`（当前带队数） | `1L` |
| 6.1.5 | 老师带队已满 | `-1L` | `-1L` |

---

### 6.2 并发名额扣减测试（`SignupServiceImpl`）

| # | 场景 | 调用次数 | Mock 行为 | 预期 |
|---|------|---------|----------|------|
| 6.2.1 | **名额不足时所有请求均被拒绝** | 3 次串行 | `decrCompetitionQuota()` 始终返回 `-2L` | 所有调用抛 `BusinessException(COMPETITION_QUOTA_FULL)`；`incrCompetitionQuota()` **不被调用**（`-2` 表示 Lua 脚本未执行 `decrby`，名额未实际扣减，无需回滚） |
| 6.2.2 | **首次成功，后续名额不足被拒绝** | 3 次串行 | 第 1 次返回 `8L`（成功），第 2、3 次返回 `-2L` | 1 次成功，2 次抛 `COMPETITION_QUOTA_FULL`；`incrCompetitionQuota()` **不被调用**（失败路径 `compQuotaDeced=false`） |

---

### 6.3 乐观锁冲突测试（`SignupServiceImpl`）

| # | 场景 | Mock 行为 | 预期 |
|---|------|----------|------|
| 6.3.1 | **乐观锁冲突时抛业务异常并回滚 Redis** | `competitionRepository.saveAndFlush()` 抛 `ObjectOptimisticLockingFailureException` | 抛 `BusinessException(CONFLICT, "当前报名人数较多，请稍后重试")`，`incrCompetitionQuota()` 被调用 1 次（回滚名额），`decrTeacherCount()` 被调用 1 次（回滚老师计数） |
