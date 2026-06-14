# PRD 功能需求完成度审查报告（更新版）

**审查日期：** 2026-06-14  
**对照文档：** docs/01-requirements/PRD.md v1.0  
**上次审查：** 2026-06-13  
**审查结论：** 相比上次审查，主要功能模块已大幅完善。仍存在 **3 个未修复的后端 Bug** 和 **1 个前端功能缺口**。

---

## 一、总体完成度（与上次对比）

| 模块 | 上次状态 | 本次状态 |
|------|---------|---------|
| 3.1 用户模块 | ⚠️ 偏弱 | ✅ 基本完成 |
| 3.2 竞赛模块 | ⚠️ 状态不流转 | ✅ 定时任务已实现 |
| 3.3 报名模块（个人赛） | ⚠️ 老师列表 404 | ✅ 流程已打通 |
| 3.3 报名模块（团队赛） | ⚠️ 路径 B 残缺 | ✅ 完整路径已实现 |
| 3.4 招募帖模块 | ❌ 学生招募帖缺失 | ✅ 已实现（队大厅） |
| 3.5 审核模块 | ⚠️ 无权限/无界面 | ✅ 管理员审核页面已补全 |
| 3.6 获奖记录模块 | ⚠️ 无上传接口 | ✅ 上传/审核已实现 |
| 3.7 消息通知模块 | ❌ 完全缺失 | ✅ 已实现（含轮询+操作按钮） |
| 3.8 数据统计模块 | ⚠️ 枚举错误 | ⚠️ **枚举值仍未修复** |
| 3.9 AI 推荐模块 | ✅ 已实现 | ⚠️ API Key 失效，服务不可用 |

---

## 二、已修复的 Bug（相比上次报告）

以下条目在上次报告中标注为 Bug，本次确认已修复：

| 编号 | 问题 | 修复状态 |
|------|------|---------|
| P0-1 | AuditController 无权限注解 | ✅ 已加 `@PreAuthorize("hasRole('ADMIN')")` |
| P0-5 | 个人赛报名 teacherId 字段不匹配 | ✅ 前端改为 `t.id` 与后端对齐 |
| P0-6 | 教师列表接口不存在 | ✅ 已加 `GET /api/v1/user/teachers` 且加入白名单 |
| P1-1 | 管理员/教师页面双重 `.data` 解析 | ✅ 已全部修正 |
| P1-2 | API 文件用 `request.get/post` | ✅ 全部改为 `request({...})` 格式 |
| P1-3 | 发布竞赛表单字段不足 | ✅ 已补全所有必填字段 |
| P1-4 | 获奖等级枚举中文 vs 英文 | ✅ 已改为枚举值 |
| P1-5 | ApplyList 调用 ADMIN 接口导致 403 | ✅ 改为教师专用接口 |
| P2-2 | 用户禁用/启用功能 | ✅ 后端接口 + 前端按钮已实现 |
| P2-4 | 审核列表无关联信息 | ✅ 管理员审核页展示竞赛名/学生信息 |
| P2-5 | 获奖录入竞赛下拉为空 | ✅ 从接口加载 |
| P3-1 | admin/111.vue 测试文件 | （不影响功能，可保留） |

---

## 三、仍未修复的 Bug

### 🔴 Bug-1（P0）：StatisticsServiceImpl 使用不存在的竞赛状态枚举

**文件：** `backend/src/main/java/com/competition/backend/service/impl/StatisticsServiceImpl.java:33-34`

```java
.judgingCount(competitionRepository.countByStatus("JUDGING"))  // 数据库无此状态
.endedCount(competitionRepository.countByStatus("ENDED"))      // 数据库无此状态
```

数据库 `competition` 表的 `status` CHECK 约束只允许：`UPCOMING / SIGNING / CLOSED / ONGOING / FINISHED / OFFLINE`，不存在 `JUDGING` 和 `ENDED`，这两个查询永远返回 0，管理员数据统计"进行中"和"已结束"数字始终为 0。

**修复：**
```java
.judgingCount(competitionRepository.countByStatus("ONGOING"))
.endedCount(competitionRepository.countByStatus("FINISHED"))
```

---

### 🔴 Bug-2（P0）：SignupServiceImpl.submitTeam 裸 `.get()` 风险

**文件：** `backend/src/main/java/com/competition/backend/service/impl/SignupServiceImpl.java:222,225`

```java
Team team = teamRepository.findById(signup.getTeamId()).get();     // 无检查
Competition comp = competitionRepository.findById(signup.getCompetitionId()).get(); // 无检查
```

数据不一致时抛 `NoSuchElementException`，500 错误无有效提示。

**修复：**
```java
Team team = teamRepository.findById(signup.getTeamId())
    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));
Competition comp = competitionRepository.findById(signup.getCompetitionId())
    .orElseThrow(() -> new BusinessException(ErrorCode.COMPETITION_NOT_FOUND, "竞赛不存在"));
```

---

### 🟡 Bug-3（P1）：RedisSyncTask 类型转换 ClassCastException 风险

**文件：** `backend/src/main/java/com/competition/backend/task/RedisSyncTask.java:35`

```java
Integer remainingQuota = (Integer) redisTemplate.opsForValue().get(key);
```

使用 JSON 序列化时，Redis 返回的数字会被反序列化为 `Long`，强转 `Integer` 会抛 `ClassCastException`，导致每 10 分钟的同步任务崩溃，报名人数无法同步。

**修复：**
```java
Object raw = redisTemplate.opsForValue().get(key);
if (raw == null) continue;
int remainingQuota = ((Number) raw).intValue();
```

---

## 四、功能缺口（前端）

### ⚠️ 缺口-1：团队赛报名"提交审核"入口缺失

**现状：** 队长创建了队伍、老师已确认带队，但**前端没有任何入口**让队长提交团队赛报名给管理员审核。

- 后端接口 `POST /api/v1/signups/team/{id}/submit` ✅ 存在
- 后端接口 `POST /api/v1/signups/team` ✅ 创建报名草稿存在
- 前端 `TeamDetail.vue` ❌ 无"提交审核"按钮
- 前端 `MyTeams.vue` ❌ 无相关入口

**需要实现：** 在 `TeamDetail.vue` 的队长操作区，当 `team.status === 'FORMING'` 且 `team.teacherConfirmed === true` 时，显示"创建报名并提交审核"按钮，调用：
1. `POST /v1/signups/team`（body: `{ teamId }`）创建草稿
2. `POST /v1/signups/team/{signupId}/submit` 提交审核

---

### ⚠️ 缺口-2：AI 推荐服务不可用

**现状：** 硅基流动 API Key（`sk-eiqfjrjjytjwwzkkrsnsvubavkkjoqubkhlpkcwfjzmmvnbl`）已失效，后端返回 `code: 40200 "AI推荐服务暂时不可用"`。

- 本地开发用 VS Code 启动，Spring Boot 不会自动读取 `.env` 文件
- 即使 `.env` 里有 Key，`SILICONFLOW_API_KEY` 环境变量实际为空
- 需要更新有效 Key 并配置到 `application-dev.yml` 或 VS Code 启动配置

---

## 五、课程验收项对照（6.2）

### 软件工程课程

| 验收项 | 状态 |
|--------|------|
| 前端增删改查操作，数据库同步变化 | ✅ |
| 多端完整闭环（报名→审核→通知反馈） | ✅ 个人赛闭环完整；团队赛缺提交入口 |
| GitHub 4人均有有效提交记录 | 需确认 |
| 系统可正常运行，核心流程无阻塞性 Bug | ⚠️ 团队赛提交审核入口缺失 |

### 服务端开发课程

| 验收项 | 状态 |
|--------|------|
| 无 JSP/Thymeleaf，严格前后端分离 | ✅ |
| Controller/Service/Repository 分层清晰 | ✅ |
| ≥15 个 RESTful 接口，统一返回体 | ✅（约 55 个接口） |
| PostgreSQL ≥6 张核心表，关联完整 | ✅（14 张表） |
| Maven 工程化，Git 协作记录完整 | ✅ |
| Redis 缓存已实现（带队计数+名额控制） | ✅ |
| RabbitMQ 异步消息已实现 | ⚠️ **配置存在但无 Producer/Consumer 代码** |
| 并发控制已实现（Redis+乐观锁） | ✅ Redis Lua 脚本已实现 |
| Spring Security 权限已实现（三角色） | ✅ |
| Docker 一键启动已实现 | ✅ |
| AI 推荐功能已实现（LangChain4j+RAG） | ⚠️ 代码已实现，API Key 失效 |

---

## 六、优先修复建议

按答辩影响排序：

1. **团队赛提交审核入口**（缺口-1）—— 演示时直接影响团队赛核心流程
2. **StatisticsServiceImpl 枚举值**（Bug-1）—— 数据统计全为 0，管理员看板数据错误
3. **AI Key 更新**（缺口-2）—— 演示 AI 功能时不可用
4. **RedisSyncTask ClassCastException**（Bug-3）—— 不影响功能演示，但长时间运行会导致计数漂移
5. **submitTeam 裸 .get()**（Bug-2）—— 正常流程不触发，但数据异常时报 500

---

## 七、已完成功能总览

以下功能经代码审查确认已完整实现：

**用户模块：** 注册（含角色校验）、统一登录（按 role 分流）、个人信息编辑、用户禁用/启用、教师列表

**竞赛模块：** 发布竞赛（完整字段）、竞赛列表（筛选分页）、竞赛详情、状态自动流转（定时任务每分钟）、下架/恢复

**个人赛报名：** 学生选老师报名 → 老师同意指导（通知） → 学生提交管理员 → 管理员审核（通知） → 被驳回可重提

**团队赛：** 创建队伍、申请老师带队、老师同意（teacherConfirmed=true）、邀请/招募队友、申请加入、队伍大厅、成员管理

**招募帖：** 老师发布/关闭招募帖、学生申请加入、老师处理申请；学生组队招募帖（队大厅）

**审核模块：** 管理员个人赛/团队赛报名审核页面（含驳回填原因）、获奖审核页面

**获奖记录：** 提交记录（含证书上传）、管理员审核

**消息通知：** 写入数据库、前端铃铛（30秒轮询未读数）、通知面板（支持直接操作同意/拒绝）、全部已读

**数据统计：** 用户/竞赛/获奖统计（竞赛枚举值有误，见 Bug-1）

**AI 推荐：** LangChain4j + PGVector RAG 链路代码已实现（API Key 失效）
