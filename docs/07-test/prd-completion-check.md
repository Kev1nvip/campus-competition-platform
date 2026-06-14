# PRD 功能需求完成度审查报告

**审查日期：** 2026-06-13  
**对照文档：** docs/01-requirements/PRD.md v1.0  
**审查结论：** 核心骨架已实现，存在 **7 个未实现模块** 和 **多个接口缺失**，影响演示完整性

---

## 一、总体完成度概览

| 模块 | PRD要求 | 实现状态 |
|------|---------|---------|
| 3.1 用户模块（注册/登录/个人主页） | ✅ | ⚠️ 基本完成，个人主页偏弱 |
| 3.2 竞赛模块（发布/列表/详情） | ✅ | ⚠️ 状态自动流转未实现 |
| 3.3 报名模块（个人赛/团队赛全流程） | ✅ | ⚠️ 团队赛路径 B 部分缺失 |
| 3.4 招募帖模块 | ✅ | ❌ 学生组队招募帖未实现 |
| 3.5 审核模块 | ✅ | ⚠️ 接口权限控制缺失 |
| 3.6 获奖记录模块 | ✅ | ❌ 文件上传接口缺失 |
| 3.7 消息通知模块 | ✅（PRD要求） | ❌ 完全未实现 |
| 3.8 数据统计模块 | ✅ | ⚠️ 指标不完整 |
| 3.9 AI推荐模块 | ✅ | ✅ 已实现 |

---

## 二、逐模块详细审查

### 3.1 用户模块

**已完成**
- 注册（学生/教师，字段校验，学号/用户名唯一）
- 统一登录（根据 role 跳转三端）
- JWT Token 鉴权
- 用户信息查询/更新

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 | 影响 |
|------|---------|---------|------|
| U-01 | 学生个人主页展示"进行中的竞赛（含状态）"和"历史参赛记录" | `StudentProfile.vue` 只展示了报名 ID 和状态枚举原始值，无竞赛名称关联 | 演示体验差 |
| U-02 | 老师个人主页：历史带队列表、每个竞赛获奖情况、当前带队数/上限 | 老师端无独立个人主页，`TeacherSelect.vue` 存在但未整合进路由 | 功能缺失 |
| U-03 | 管理员可禁用/启用用户（`status: DISABLED`） | `UserManage.vue` 禁用按钮无实现，后端无禁用接口 | 功能缺失 |
| U-04 | 教师列表接口（学生报名时选老师用） | 前端调 `/v1/users/teachers`，后端无此接口（`UserController` 只有 `/list` 带 ADMIN 权限） | 报名页老师列表永远为空 |

**需修复文件：**
- `backend/controller/UserController.java`：新增 `GET /api/v1/users/teachers`（不限角色可访问，供学生选老师）
- `backend/controller/UserController.java`：新增 `PUT /api/v1/admin/users/{id}/status`（管理员禁用用户）
- `frontend/views/StudentProfile.vue`：关联查询竞赛名称展示
- `frontend/views/teacher/`：新增教师个人主页页面

---

### 3.2 竞赛模块

**已完成**
- 发布竞赛（字段完整，基础校验）
- 竞赛列表（分页、筛选、搜索）
- 竞赛详情
- 手动下架/恢复

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 | 影响 |
|------|---------|---------|------|
| C-01 | 竞赛状态根据时间自动流转（UPCOMING→SIGNING→CLOSED→ONGOING→FINISHED） | 无定时任务，状态只在创建时设为 `UPCOMING`，永不自动变更 | 竞赛永远显示"未开始"，无法演示报名中状态 |
| C-02 | `minTeamSize ≤ maxTeamSize` 校验 | `CompetitionServiceImpl.validateCompetition` 未校验此关系 | 数据异常 |
| C-03 | 发布竞赛时前端缺少 `competitionStart/End` 时间字段 | `PublishComp.vue` 只有 `signupEnd`，缺少比赛时间和 `signupStart` | 状态流转前提缺失 |

**需修复文件：**
- `backend/task/`：新增 `CompetitionStatusTask.java`，每分钟扫描时间判断自动切换状态
- `backend/service/impl/CompetitionServiceImpl.java`：补充 `minTeamSize ≤ maxTeamSize` 校验
- `frontend/views/teacher/PublishComp.vue`：补充 `signupStart`、`competitionStart`、`competitionEnd` 字段

---

### 3.3 报名模块

**已完成**
- 个人赛报名（创建草稿、老师指导申请、提交管理员审核）
- 团队赛路径 B 步骤1-3（创建队伍、提交审核）
- 报名状态管理

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 | 影响 |
|------|---------|---------|------|
| S-01 | 老师同意后报名变为 DRAFT，学生手动提交才进入 PENDING | 后端逻辑正确，但前端 `StudentSignup.vue` 直接创建草稿后没有"提交"按钮的明确流程引导 | 流程不清晰 |
| S-02 | 竞赛达到名额上限后不可新增报名 | Redis 配额校验已实现，但竞赛发布时没有初始化 Redis 计数器（`createCompetition` 有 TODO 注释）| 名额控制实际无效 |
| S-03 | 路径 B 中老师主导：老师确认人数满足后的"确认组队完成"操作 | 无对应接口，`TeamServiceImpl` 缺少此操作 | 团队赛路径 A 流程中断 |
| S-04 | 驳回后可修改重新提交（RESUBMITTED 状态） | 后端支持，前端无对应"重新提交"入口 | 流程无法闭环 |

**需修复文件：**
- `backend/service/impl/CompetitionServiceImpl.java`：`createCompetition` 中初始化 Redis 名额计数器（删除 TODO）
- `frontend/views/StudentProfile.vue`：报名记录加"提交审核"按钮（DRAFT 状态时显示）
- `frontend/views/StudentProfile.vue`：REJECTED 状态加"重新提交"入口

---

### 3.4 招募帖模块

**已完成**
- 老师发布招募帖
- 老师关闭招募帖
- 学生申请加入老师招募帖
- 老师审核申请

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 | 影响 |
|------|---------|---------|------|
| R-01 | **学生组队招募帖**：队长在获老师同意后发布寻找队友的招募帖 | 后端有 `team_recruitment` 表和 `TeamRecruitment` 实体，但 `RecruitmentController` 只实现了老师招募帖，无学生组队招募帖接口 | 团队赛核心功能缺失 |
| R-02 | 招募帖列表页（学生查看） | 无对应前端页面和后端 `GET` 接口 | 学生无法发现招募帖 |
| R-03 | 竞赛报名截止后自动关闭招募帖 | 无实现（依赖定时任务 C-01 未实现） | 规则无法执行 |

**需修复文件：**
- `backend/controller/RecruitmentController.java`：补充学生组队招募帖的 CRUD + 申请接口
- `backend/service/impl/RecruitmentServiceImpl.java`：实现学生招募帖业务逻辑
- `frontend/`：新建招募帖列表页面

---

### 3.5 审核模块

**已完成**
- 管理员审核报名（INDIVIDUAL/TEAM 两类型）
- 审核状态流转（APPROVED/REJECTED）

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 | 影响 |
|------|---------|---------|------|
| A-01 | `AuditController` 无 `@PreAuthorize`，任何登录用户均可调用 | 越权漏洞（已在 code-review 报告中标注） | 安全风险 |
| A-02 | 管理员驳回后通知老师，老师通知学生/队长修改并重提 | 通知模块未实现，驳回只更新状态无通知 | 流程中断 |
| A-03 | 前端审核列表需展示参赛人员基本信息、老师信息、竞赛信息 | `ApplyList.vue` 只展示 ID 和状态，无关联信息 | 演示体验差 |

**需修复文件：**
- `backend/controller/AuditController.java`：加 `@PreAuthorize("hasRole('ADMIN')")`
- `frontend/views/teacher/ApplyList.vue`：关联展示学生姓名、学号、竞赛名称

---

### 3.6 获奖记录模块

**已完成**
- 提交获奖记录（字段完整，奖项等级枚举正确）
- 管理员审核获奖记录

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 | 影响 |
|------|---------|---------|------|
| W-01 | 上传获奖证书图片（`certificateUrl` 为本地存储路径） | 后端无文件上传接口，`AwardController` 无 `/upload` 端点，前端 `AwardInput.vue` 直接填 URL 文本 | 证书无法上传 |
| W-02 | 只有报名已生效（APPROVED）的学生才能提交获奖记录 | `AwardServiceImpl` 未校验关联报名是否 APPROVED | 数据准确性风险 |
| W-03 | 审核通过后展示在老师主页带队成绩中 | 老师主页未实现（U-02 问题） | 功能缺失 |
| W-04 | 前端 `AwardInput.vue` 的竞赛下拉列表为空 | `compId` 的 `<select>` 无数据源 | 无法选择竞赛 |

**需修复文件：**
- `backend/controller/AwardController.java`：新增 `POST /api/v1/award/upload` 文件上传接口
- `backend/service/impl/AwardServiceImpl.java`：创建前校验关联报名状态
- `frontend/views/teacher/AwardInput.vue`：竞赛下拉列表从接口加载数据

---

### 3.7 消息通知模块

**完全未实现**

| 编号 | PRD要求 | 实际状态 |
|------|---------|---------|
| N-01 | RabbitMQ 异步推送通知 | 后端有 RabbitMQ 配置（docker-compose.yml），但无任何 Producer/Consumer 代码 |
| N-02 | `sys_notification` 表已建，无写入逻辑 | 表存在，Service/Controller 层完全空缺 |
| N-03 | 前端顶栏显示未读通知数（红点），支持查看/已读 | 三端均无通知入口 |
| N-04 | 前端每 30 秒轮询未读数 | 未实现 |

**需新建文件：**
- `backend/service/NotificationService.java` + impl
- `backend/controller/NotificationController.java`（`GET /api/v1/notifications`、`PUT /api/v1/notifications/{id}/read`）
- `backend/mq/NotificationProducer.java` + `NotificationConsumer.java`
- 前端三端 Layout 通知图标组件

---

### 3.8 数据统计模块

**已完成**
- 用户统计（总数、学生数、教师数）
- 竞赛统计（总数、各状态数）
- 获奖统计（总数、通过数、待审核数）

**未完成 / 偏差**

| 编号 | PRD要求 | 实际状态 |
|------|---------|---------|
| D-01 | "活跃用户数（近30天有操作记录）" | 未实现，无操作记录追踪 |
| D-02 | "各竞赛报名人数" | 未实现 |
| D-03 | "各竞赛审核通过率" | 未实现 |
| D-04 | "按奖项等级分布的获奖数量" | 未实现 |
| D-05 | `StatisticsVO.CompetitionStats` 中 `judgingCount` 使用状态 `"JUDGING"` 不存在 | 已在上次报告修复，但后端服务未重启 | 数据统计全为0 |

---

### 3.9 AI 推荐模块

**基本完成**
- LangChain4j + PGVector RAG 链路已实现
- 接口 `POST /api/v1/ai/recommend` 已实现
- 知识库刷新接口已实现

**偏差**

| 编号 | PRD要求 | 实际状态 |
|------|---------|---------|
| AI-01 | 出参包含 `competitionName`、`reason`、`source`、`matchScore` 结构化列表 | 实际返回的是 LLM 原始文本字符串，非结构化 JSON | 演示时展示格式与 PRD 不符 |
| AI-02 | 入参字段名为 `direction` | 后端 `AiRecommendRequest` 字段为 `prompt` | 轻微偏差，不影响功能 |

---

## 三、课程验收项对照（6.2）

| 验收项 | 状态 |
|--------|------|
| 前端增删改查，数据库同步变化 | ✅ |
| 多端完整闭环（报名→审核→通知） | ❌ 通知未实现，闭环不完整 |
| GitHub 4人均有提交 | 待确认 |
| 核心流程无阻塞性 Bug | ⚠️ 老师列表 404 导致个人赛报名阻塞 |
| 无 JSP，严格前后端分离 | ✅ |
| ≥15 个 RESTful 接口 | ✅（现有约 25 个） |
| PostgreSQL ≥6 张核心表 | ✅（14 张表） |
| Redis 缓存已实现 | ⚠️ 已实现但名额初始化 TODO 未完成 |
| RabbitMQ 异步消息已实现 | ❌ 完全未实现 |
| 并发控制已实现 | ⚠️ 代码存在但名额初始化缺失导致实际无效 |
| Spring Security 三角色 | ✅ |
| Docker 一键启动 | ✅ |
| AI 推荐已实现 | ✅ |

---

## 四、优先修复建议（按影响排序）

### 🔴 P0 — 影响演示核心流程

1. **老师列表接口缺失**（U-04）：`POST /api/v1/users/teachers`，不加权限，学生报名时才能选老师
2. **竞赛状态自动流转**（C-01）：加定时任务，否则所有竞赛显示"未开始"，报名入口不可用
3. **Redis 名额初始化**（S-02）：删除 `createCompetition` 中的 TODO，创建竞赛时初始化计数器

### 🟡 P1 — 影响流程完整度

4. **消息通知基础实现**（N-01~04）：至少实现数据库写入 + 查询，RabbitMQ 可简化为同步调用
5. **AuditController 权限补充**（A-01）：加 `@PreAuthorize("hasRole('ADMIN')")` 修复安全漏洞
6. **文件上传接口**（W-01）：证书上传是获奖记录的前提
7. **前端报名重提流程**（S-04）：DRAFT/REJECTED 状态下加操作按钮

### 🟢 P2 — 提升完整度

8. **学生组队招募帖**（R-01）：团队赛路径 B 的关键功能
9. **发布竞赛表单补全**（C-03）：加报名开始时间和比赛时间字段
10. **用户禁用功能**（U-03）：管理员的基础用户管理能力
