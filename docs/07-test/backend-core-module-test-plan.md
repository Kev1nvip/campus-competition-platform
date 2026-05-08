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

## 二、基础搭建与环境测试 (基础架构)

### 2.1 环境配置验证
- **测试动作**：运行 `docker-compose up -d`，验证所有中间件容器是否在 30 秒内健康启动。
- **预期结果**：PostgreSQL, Redis, RabbitMQ 均正常提供服务，不抛出端口冲突或配置异常。
- **后端连通性**：启动 SpringBoot 成功，控制台无 DataSource 注入失败、Redis 连接超时等报错。

### 2.2 全局异常与返回体统一
- **测试动作**：构造非法请求（如缺少必填参数的POST请求、请求不存在的资源路径）。
- **预期结果**：返回的 JSON 结构严格符合 `Result` 规范，例如 `{"code": 400, "message": "参数校验失败", "data": null}`。

---

## 三、认证模块测试 (Auth)

### 3.1 接口级测试 (API)
| 接口描述 | 方法 | 路径 | 核心校验点 |
|---------|-----|------|----------|
| 用户注册 | POST| `/api/v1/auth/register` | 密码是否加密存储(Bcrypt)，重复注册是否抛出 `BusinessException`。 |
| 用户登录 | POST| `/api/v1/auth/login` | 账号密码错误提示；登录成功后是否返回标准 JWT 格式的 token。 |

### 3.2 功能级测试
- **未授权拦截**：不带 JWT 访问需鉴权的接口（如 `/api/v1/signups/individual`），预期返回 HTTP 401。
- **权限校验 (RBAC)**：使用 `STUDENT` 角色账号访问管理员特权接口，预期返回 HTTP 403 Forbidden。

---

## 四、竞赛模块测试 (Competition)

### 4.1 接口级测试 (API)
| 接口描述 | 方法 | 路径 | 核心校验点 |
|---------|-----|------|----------|
| 竞赛分页列表 | GET | `/api/v1/competitions` | 验证多条件筛选（状态、类型）及分页逻辑的正确性。 |
| 发布竞赛 | POST| `/api/v1/competitions` | 管理员权限 `@PreAuthorize("hasRole('ADMIN')")` 生效情况。 |
| 竞赛详情 | GET | `/api/v1/competitions/{id}` | 数据完整性，状态映射正确性。 |

### 4.2 功能测试
- **状态流转**：测试竞赛从“报名未开始” -> “报名进行中” -> “报名结束”等时间节点和手动控制的状态变更逻辑。

---

## 五、报名模块与并发控制测试 (Signup & Concurrency)
*(此模块为重中之重，需验证锁与 Redis 限制)*

### 5.1 接口级测试 (API)
| 接口描述 | 方法 | 路径 | 核心校验点 |
|---------|-----|------|----------|
| 个人赛报名 | POST| `/api/v1/signups/individual` | 参数校验，同一学生重复报名防刷。 |
| 个人赛提交审核 | POST| `/api/v1/signups/individual/{id}/submit` | 状态机流转是否正确，指导老师是否合法。 |
| 团队赛报名草稿 | POST| `/api/v1/signups/team` | 发起者必须是队长身份验证，验证关联队伍是否存在。 |

### 5.2 功能与高并发压测 (JMeter)
- **并发名额超卖测试**：
  - **场景**：设置某竞赛名额只有 5 人，使用 JMeter 设置 100 个并发线程同时发起报名请求。
  - **预期结果**：前 5 个请求报名成功，后 95 个请求被 Redis 拦截（`decrCompetitionQuota`），报“名额不足”，数据库无超发记录。
- **乐观锁冲突测试**：
  - **场景**：模拟管理员同时通过多个端修改同一竞赛信息或状态（操作相同记录）。
  - **预期结果**：数据库 `@Version` 发挥作用，第一个请求成功，第二个抛出乐观锁异常（如 `ObjectOptimisticLockingFailureException`），向用户提示“数据已被修改”。
- **老师带队上限控制验证**：
  - **场景**：并发下多个队伍/个人选择同一个指导老师（老师上限设为 3 个）。
  - **预期结果**：Lua 脚本原子性生效，只有前 3 个成功关联，超出部分抛出上限提示。

---

## 六、AI推荐模块测试 (AI Recommendation)

### 6.1 接口级测试 (API)
| 接口描述 | 方法 | 路径 | 核心校验点 |
|---------|-----|------|----------|
| 获取AI推荐 | POST| `/api/v1/ai/recommend` | 请求超时处理、大模型API密钥校验。 |
| 更新知识库文档 | POST| `/api/v1/ai/knowledge` | 文本切片及 PGVector 向量化存储是否成功入库。 |

### 6.2 功能级测试
- **向量存储验证**：上传竞赛资料后，通过客户端直连 PG 数据库查看 `RagDocument` 表，确认 `embedding` 字段存在向量数据。
- **RAG 检索召回率**：输入相关专业标签，测试向量相似度检索（Cosine Similarity 等）是否能准确召回最相关的竞赛。
- **LangChain4j 容灾测试**：手动阻断模型接口网络，验证系统 fallback 机制或给出的友好错误提示。
