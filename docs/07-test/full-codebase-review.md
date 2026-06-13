# 全量代码审查报告

**审查日期：** 2026-06-13  
**审查范围：** 全项目（后端 Spring Boot + 前端 Vue3 + 数据库 + 基础设施）   

---

## 目录

1. [严重Bug（P0）- 导致功能完全失效](#一严重bugp0)
2. [高优先级Bug（P1）- 功能错误或数据错误](#二高优先级bugp1)
3. [中优先级Bug（P2）- 功能残缺或逻辑错误](#三中优先级bugp2)
4. [低优先级问题（P3）- 代码质量和潜在隐患](#四低优先级问题p3)
5. [修复文件汇总](#五修复文件汇总)

---

## 一、严重Bug（P0）

### P0-1：AuditController 无权限控制，任何人可审核报名

**文件：** `backend/src/main/java/com/competition/backend/controller/AuditController.java`

```java
@PostMapping("/signup")
public Result<Void> auditSignup(@Valid @RequestBody SignupAuditDTO dto) {
    // 没有任何 @PreAuthorize 注解！
```

`AuditController` 的 `auditSignup` 接口没有 `@PreAuthorize` 注解，任何登录用户（包括学生）都可以调用该接口来审核他人的报名记录，严重的越权漏洞。

**修复：**  
在 `AuditController.java:22` 添加：
```java
@PreAuthorize("hasRole('ADMIN')")
```

---

### P0-2：SignupServiceImpl.submitTeam 使用裸 .get()，NullPointerException 风险极高

**文件：** `backend/src/main/java/com/competition/backend/service/impl/SignupServiceImpl.java`

```java
// 第 222 行
Team team = teamRepository.findById(signup.getTeamId()).get();  // 无检查
// 第 225 行
Competition comp = competitionRepository.findById(signup.getCompetitionId()).get();  // 无检查
```

两处 `.get()` 不检查 Optional 是否存在，数据不一致时直接抛 `NoSuchElementException`，被 GlobalExceptionHandler 兜底为 500，无法给用户提示有意义的错误信息。

**修复：**  
```java
Team team = teamRepository.findById(signup.getTeamId())
    .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND, "队伍不存在"));
Competition comp = competitionRepository.findById(signup.getCompetitionId())
    .orElseThrow(() -> new BusinessException(ErrorCode.COMPETITION_NOT_FOUND, "竞赛不存在"));
```

---

### P0-3：StatisticsServiceImpl 使用不存在的竞赛状态枚举值

**文件：** `backend/src/main/java/com/competition/backend/service/impl/StatisticsServiceImpl.java`

```java
.judgingCount(competitionRepository.countByStatus("JUDGING"))  // 数据库无此状态
.endedCount(competitionRepository.countByStatus("ENDED"))      // 数据库无此状态
```

数据库 `competition` 表的 `status` CHECK 约束只允许：`UPCOMING / SIGNING / CLOSED / ONGOING / FINISHED / OFFLINE`，不存在 `JUDGING` 和 `ENDED`。这两个查询永远返回 0，统计数据完全错误。

**修复：** `StatisticsServiceImpl.java` 第 33-34 行改为：
```java
.judgingCount(competitionRepository.countByStatus("ONGOING"))
.endedCount(competitionRepository.countByStatus("FINISHED"))
```

---

### P0-4：RedisSyncTask 中 remainingQuota 类型转换异常

**文件：** `backend/src/main/java/com/competition/backend/task/RedisSyncTask.java`

```java
Integer remainingQuota = (Integer) redisTemplate.opsForValue().get(key);
```

Redis 存储的值通过 `RedisTemplate<String, Object>` 写入，序列化方式取决于 `RedisConfig` 的配置，如果使用 JSON 序列化可能反序列化为 `Long` 而不是 `Integer`，运行时会抛 `ClassCastException`，导致定时任务崩溃，报名人数无法同步到数据库。

**修复：**
```java
Object raw = redisTemplate.opsForValue().get(key);
if (raw == null) continue;
int remainingQuota = ((Number) raw).intValue();
```

---

### P0-5：前端报名接口与后端字段不匹配，个人赛报名必然失败

**文件：** `frontend/src/types/signup.ts` vs `backend/.../dto/IndividualSignupDTO.java`

前端发送的字段：
```typescript
interface IndividualSignupRequest {
  competitionId: number
  teacherId?: number  // 可选
  phone?: string      // 后端 DTO 中不存在此字段
  email?: string      // 后端 DTO 中不存在此字段
  remark?: string     // 后端 DTO 中不存在此字段
}
```

后端 DTO：
```java
public class IndividualSignupDTO {
    @NotNull(message = "指导老师不能为空")
    private Long teacherId;  // 后端强制非空，但前端标记为可选
    private String motivation;
    private String introduction;
    // 没有 phone、email、remark 字段
}
```

两端字段不对齐，且后端 `teacherId` 是 `@NotNull`，而前端允许不传。用户提交报名时会因参数校验失败返回 400。

**修复选项 A（推荐）：** 修改后端 `IndividualSignupDTO`，去掉 `@NotNull`，并根据业务决定是否增加 `phone`、`email`、`remark` 字段。  
**修复选项 B：** 修改前端 `IndividualSignupRequest`，使 `teacherId` 为必填，并移除后端不存在的字段。

---

### P0-6：signup.ts 调用了后端不存在的接口 /v1/users/teachers

**文件：** `frontend/src/api/signup.ts` 第 94 行

```typescript
const response = await request.get('/v1/users/teachers', { params })
```

后端 `UserController` 的路由只有：
- `GET /api/v1/user/info`
- `PUT /api/v1/user/info`
- `GET /api/v1/user/teacher/{teacherId}`

不存在 `/v1/users/teachers` 列表接口。前端报名页的"选择指导老师"功能请求必然 404，老师列表为空，用户无法选择老师。

**修复：** 需要在 `UserController` 后端新增教师列表接口：
```java
@GetMapping("/teachers")
public Result<PageVO<TeacherProfileVO>> listTeachers(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String keyword) { ... }
```
同时路径应统一：注意后端路径是 `/user`（单数），前端请求的是 `/users`（复数），需对齐。

---

## 二、高优先级Bug（P1）

### P1-1：前端管理员/教师页面 API 响应解析双重 .data 包裹

**文件：** `frontend/src/views/admin/StatData.vue`、`frontend/src/views/admin/CompManage.vue`、`frontend/src/views/admin/UserManage.vue`、`frontend/src/views/admin/DeptManage.vue`、`frontend/src/views/teacher/ApplyList.vue`、`frontend/src/views/teacher/TeamManage.vue`

```javascript
// 所有管理员/教师页面都有类似写法：
stat.value = res.data.data      // 错误
compList.value = res.data.data  // 错误
userList.value = res.data.data  // 错误
```

`request.ts` 的响应拦截器已经 `return response.data`，所以 `res` 已经是 `ApiResponse` 对象（`{ code, message, data }`）。正确访问应该是 `res.data`，而不是 `res.data.data`。

**修复：** 将所有此类页面的解析改为：
```javascript
stat.value = res.data
userList.value = res.data
```

受影响文件：
- `frontend/src/views/admin/StatData.vue:32`
- `frontend/src/views/admin/CompManage.vue:38`
- `frontend/src/views/admin/UserManage.vue:46`
- `frontend/src/views/admin/DeptManage.vue:32`
- `frontend/src/views/teacher/ApplyList.vue:38`
- `frontend/src/views/teacher/TeamManage.vue:40`

---

### P1-2：competition.ts API 使用 request.get/post 实例方法，类型推断错误

**文件：** `frontend/src/api/competition.ts`

```typescript
const response = await request.get<Result<PageVO<CompetitionVO>>>('/v1/competitions', { params })
return response as unknown as Result<PageVO<CompetitionVO>>
```

`request` 现在是包装函数 `function request<T>(config: AxiosRequestConfig): Promise<T>`，它没有 `.get()` 和 `.post()` 子方法，调用 `request.get(...)` 会在运行时报 `TypeError: request.get is not a function`。

**修复：** 将所有 `request.get/post` 改为使用配置对象格式：
```typescript
getList: async (params) => {
  return request<Result<PageVO<CompetitionVO>>>({ url: '/v1/competitions', method: 'GET', params })
},
getById: async (id: number) => {
  return request<Result<CompetitionDetailVO>>({ url: `/v1/competitions/${id}`, method: 'GET' })
}
```

同样问题存在于 `frontend/src/api/signup.ts` 中的 `request.get`、`request.post` 调用（第 20、37、58、70、88 行）。

---

### P1-3：PublishComp.vue 发布竞赛表单字段严重不足，且响应码错误

**文件：** `frontend/src/views/teacher/PublishComp.vue`

```javascript
const form = ref({ name: '', desc: '', endTime: '' })
// 发送的字段：name、desc、endTime
// 后端需要：title、type、organizer、signupStart、signupEnd、hasQuota 等
```

- 前端发送字段名与后端 `CompetitionSaveDTO` 完全不对应（`name` vs `title`，`desc` vs `description`，`endTime` vs `signupEnd`）
- 缺少后端必填字段：`type`（INDIVIDUAL/TEAM）、`organizer`、`signupStart`、`hasQuota`
- 后端 `@NotBlank` 校验会直接拒绝，返回 400
- 响应码判断 `res.data.code === 200`，后端成功码为 `0`，且 `res.data` 已经是解包后的数据

**修复：** 完整重写发布竞赛表单，字段对齐 `CompetitionSaveDTO`，成功码改为 `res.code === 0`。

---

### P1-4：AwardInput.vue 奖项等级与后端枚举不匹配

**文件：** `frontend/src/views/teacher/AwardInput.vue`

```html
<option>一等奖</option>
<option>二等奖</option>
<option>三等奖</option>
```

后端 `AwardRecord` 表的 CHECK 约束要求 `award_level` 值为：`NATIONAL_FIRST / NATIONAL_SECOND / NATIONAL_THIRD / PROVINCIAL_FIRST / PROVINCIAL_SECOND / PROVINCIAL_THIRD / OTHER`，不接受中文值。提交必然触发数据库 CHECK 约束违反，报 500。

**修复：**
```html
<option value="NATIONAL_FIRST">国家一等奖</option>
<option value="NATIONAL_SECOND">国家二等奖</option>
<option value="NATIONAL_THIRD">国家三等奖</option>
<option value="PROVINCIAL_FIRST">省级一等奖</option>
<option value="PROVINCIAL_SECOND">省级二等奖</option>
<option value="PROVINCIAL_THIRD">省级三等奖</option>
<option value="OTHER">其他</option>
```

---

### P1-5：ApplyList.vue handleAudit 中错误调用 onMounted()

**文件：** `frontend/src/views/teacher/ApplyList.vue`

```javascript
const handleAudit = async (row: any, status: number) => {
  await auditApply({ id: row.id, status })
  alert(status === 1 ? '已通过' : '已驳回')
  onMounted()  // 错误！onMounted 是 Vue 生命周期钩子注册函数，不是刷新数据的方法
}
```

`onMounted()` 在这里被当作刷新函数调用，这完全无效——`onMounted` 只接受回调并注册，不会重新触发。审核后列表不会刷新。

**修复：** 提取加载函数并复用：
```javascript
const loadData = async () => {
  const res = await getApplyList()
  tableList.value = res.data  // 注意同时修复双重.data问题
}
onMounted(loadData)

const handleAudit = async (row: any, status: number) => {
  await auditApply({ id: row.id, status })
  alert(status === 1 ? '已通过' : '已驳回')
  await loadData()  // 正确刷新
}
```

---

### P1-6：CompetitionServiceImpl.getCompetitionList 每次查询触发 N 次数据库请求

**文件：** `backend/src/main/java/com/competition/backend/service/impl/CompetitionServiceImpl.java`

```java
private CompetitionListVO convertToListVO(Competition c) {
    // ...
    userRepository.findById(c.getCreatedBy()).ifPresent(...);  // 每条竞赛都查一次用户表
}
```

每次 `getCompetitionList` 返回 N 条竞赛，就会触发 N 次 `userRepository.findById`（N+1 查询问题）。在竞赛列表页面性能会很差。

**修复：** 提前一次性批量查询发布人信息，或在 SQL 层 JOIN 查询，使用 Map 缓存：
```java
// 在 getCompetitionList 方法中：
List<Competition> competitions = competitionPage.getContent();
Set<Long> creatorIds = competitions.stream().map(Competition::getCreatedBy).collect(toSet());
Map<Long, SysUser> creatorMap = userRepository.findAllById(creatorIds)
    .stream().collect(toMap(SysUser::getId, u -> u));
```

---

## 三、中优先级Bug（P2）

### P2-1：StudentSignup.vue 的 IndividualSignupRequest 中 teacherId 可选，但后端强制非空

（与 P0-5 联动，此处指出前端逻辑问题）

**文件：** `frontend/src/views/StudentSignup.vue`

```typescript
const requestData: IndividualSignupRequest = {
  competitionId: Number(route.params.id),
  teacherId: selectedTeacher.value?.userId,  // 可能为 undefined
  ...
}
```

用户勾选"不需要指导老师"时 `teacherId` 为 undefined，但后端 `@NotNull` 会拒绝。前端表单设计与后端业务逻辑存在矛盾，需要业务决策：是允许不选老师还是必须选。

---

### P2-2：TeamServiceImpl 接受邀请时未校验队伍人数上限

**文件：** `backend/src/main/java/com/competition/backend/service/impl/TeamServiceImpl.java`

```java
if ("APPROVED".equals(status)) {
    Team team = teamRepository.findById(apply.getBizId())...;
    team.setMemberCount(team.getMemberCount() + 1);  // 未校验是否超过 maxTeamSize
    teamRepository.save(team);
```

接受队友邀请时没有校验当前人数是否已达到竞赛的 `maxTeamSize` 上限，可能导致队伍人数超出限制。

**修复：** 邀请通过前先查询对应竞赛的 `maxTeamSize` 并校验：
```java
Competition comp = competitionRepository.findById(team.getCompetitionId())...;
if (comp.getMaxTeamSize() != null && team.getMemberCount() >= comp.getMaxTeamSize()) {
    throw new BusinessException(ErrorCode.TEAM_MEMBER_FULL, "队伍人数已满");
}
```

---

### P2-3：RecruitmentServiceImpl.applyTeacherRecruitment 缺少重复申请校验

**文件：** `backend/src/main/java/com/competition/backend/service/impl/RecruitmentServiceImpl.java`

`applyTeacherRecruitment` 方法没有检查同一学生是否已经申请过同一招募帖，会导致重复申请记录积累。

**修复：** 添加重复申请检查：
```java
boolean exists = applyRecordRepository.existsByTypeAndApplicantIdAndBizId(
    "TEACHER_RECRUIT_APPLY", studentId, recruitmentId);
if (exists) {
    throw new BusinessException(ErrorCode.APPLY_DUPLICATE, "您已申请过该招募帖");
}
```
同时需要在 `ApplyRecordRepository` 中添加对应方法。

---

### P2-4：AwardServiceImpl.createAward 缺少重复获奖校验

**文件：** `backend/src/main/java/com/competition/backend/service/impl/AwardServiceImpl.java`

数据库 `award_record` 表有唯一索引 `uk_award_record(biz_type, biz_id)`，但 Service 层在 `save` 之前没有做软校验，会直接触发数据库唯一约束异常，返回的是 500 而不是有意义的业务错误。

**修复：** 添加业务层重复校验：
```java
if (awardRecordRepository.existsByBizTypeAndBizId(dto.getBizType(), dto.getBizId())) {
    throw new BusinessException(ErrorCode.AWARD_DUPLICATE, "该报名记录已提交过获奖申请");
}
```

---

### P2-5：CompetitionServiceImpl.updateCompetition 字段更新不完整（TODO 未完成）

**文件：** `backend/src/main/java/com/competition/backend/service/impl/CompetitionServiceImpl.java`

```java
// 根据状态决定是否允许修改敏感字段（逻辑略，此处实现全量更新，需注意 enrolledCount 校验）
competition.setTitle(saveDTO.getTitle());
competition.setOrganizer(saveDTO.getOrganizer());
competition.setRequirement(saveDTO.getRequirement());
competition.setSignupEnd(saveDTO.getSignupEnd());
competition.setMaxQuota(saveDTO.getMaxQuota());
// ... 其他字段赋值  ← 注释而非代码，多个字段从未被更新
```

`type`、`signupStart`、`competitionStart`、`competitionEnd`、`hasQuota`、`minTeamSize`、`maxTeamSize`、`maxTeachQuota`、`description`、`attachmentUrl` 等字段在 `updateCompetition` 中永远不会被更新。

**修复：** 完成全量字段赋值，删除 `// ...` 注释占位。

---

### P2-6：signup.ts 类型中 SignupStatus 枚举值与后端不一致

**文件：** `frontend/src/types/signup.ts`

```typescript
export type SignupStatus = 'SIGNED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
```

后端 `IndividualSignup` 实体的 status 值为：`DRAFT / PENDING / APPROVED / REJECTED / RESUBMITTED`，前端定义的 `SIGNED` 和 `CANCELLED` 在后端不存在，会导致前端状态展示映射错误。

**修复：**
```typescript
export type SignupStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'RESUBMITTED'
```
同时更新 `signupStatusMap` 的对应中文文本。

---

### P2-7：Home.vue hero-subtitle 文字颜色几乎不可见

**文件：** `frontend/src/views/Home.vue`

```css
.hero-subtitle {
  color: rgba(255, 255, 255, 0.9);  /* 白色文字 */
```

但 `.home-main` 的背景是白色（默认），不是渐变色，因此副标题文字近乎透明不可见（白字白底）。

**修复：**
```css
.hero-subtitle {
  color: #666;
}
```

---

## 四、低优先级问题（P3）

### P3-1：admin/111.vue 存在未完成的测试页面，应删除

**文件：** `frontend/src/views/admin/111.vue`

文件名是纯数字，应是开发过程中的临时测试文件，不应存在于生产代码中。

---

### P3-2：TeamMember 实体缺少 Repository，quitTeam 逻辑不完整

**文件：** `backend/src/main/java/com/competition/backend/service/impl/TeamServiceImpl.java`

`quitTeam` 方法只更新了 `team.memberCount`，没有删除 `team_member` 表中的成员记录，导致数据库 `team_member` 表与 `team.member_count` 不一致。这是因为整个项目中 `TeamMemberRepository` 缺失。

---

### P3-3：RecruitmentServiceImpl.applyTeacherRecruitment 存在 TOCTOU 竞态条件

**文件：** `backend/src/main/java/com/competition/backend/service/impl/RecruitmentServiceImpl.java`

```java
// 先查 currentCount
if (recruitment.getCurrentCount() >= recruitment.getRecruitCount()) {
// 后 save apply
applyRecordRepository.save(apply);
// 但 recruitment 的 currentCount 更新在 auditApply 中
```

名额检查和名额预占不是原子操作，高并发时多个申请可能通过校验但超出名额。

---

### P3-4：dev 环境 Redis 端口配置为 6735，与 docker-compose 映射不一致

**文件：** `backend/src/main/resources/application-dev.yml`

```yaml
redis:
  port: 6735  # 本机端口
```

`docker-compose.yml` 中 Redis 映射为 `6735:6379`，所以本机端口 6735 实际上映射到容器内部的 6379。这个配置表面上能工作，但非常容易引起混淆（通常习惯用标准端口）。建议统一注释说明这是 Docker 映射的非标端口。

---

### P3-5：AiServiceImpl 中 chatMemory 配置为单例，多用户共享对话历史

**文件：** `backend/src/main/java/com/competition/backend/service/impl/AiServiceImpl.java`

```java
.chatMemory(MessageWindowChatMemory.withMaxMessages(10))
```

`AiAssistant` 在 `@PostConstruct` 中以单例形式创建，`MessageWindowChatMemory` 是同一个实例，所有用户的 AI 对话共享同一个对话窗口，后来的用户会"看到"前一个用户的对话上下文，存在信息泄露风险。

**修复：** 改用 `chatMemoryProvider` 按用户会话隔离，或完全去掉 chatMemory（无状态 AI 推荐场景下不需要持久记忆）。

---

### P3-6：admin/CompManage.vue、UserManage.vue 编辑/禁用按钮无功能实现

**文件：** `frontend/src/views/admin/CompManage.vue`、`frontend/src/views/admin/UserManage.vue`

所有操作按钮（编辑、下架、禁用）都没有绑定任何事件处理器，是 UI 存根，功能完全未实现。

---

## 五、修复文件汇总

| 优先级 | 文件 | 问题编号 |
|--------|------|---------|
| P0 | `backend/.../controller/AuditController.java` | P0-1 缺少权限注解 |
| P0 | `backend/.../service/impl/SignupServiceImpl.java` | P0-2 裸 .get() |
| P0 | `backend/.../service/impl/StatisticsServiceImpl.java` | P0-3 错误枚举值 |
| P0 | `backend/.../task/RedisSyncTask.java` | P0-4 类型转换异常 |
| P0 | `backend/.../dto/IndividualSignupDTO.java` | P0-5 teacherId 必填与前端不符 |
| P0 | `backend/.../controller/UserController.java` | P0-6 缺少教师列表接口 |
| P1 | `frontend/src/views/admin/StatData.vue` | P1-1 双重 .data |
| P1 | `frontend/src/views/admin/CompManage.vue` | P1-1 双重 .data |
| P1 | `frontend/src/views/admin/UserManage.vue` | P1-1 双重 .data |
| P1 | `frontend/src/views/admin/DeptManage.vue` | P1-1 双重 .data |
| P1 | `frontend/src/views/teacher/ApplyList.vue` | P1-1、P1-5 |
| P1 | `frontend/src/views/teacher/TeamManage.vue` | P1-1 双重 .data |
| P1 | `frontend/src/api/competition.ts` | P1-2 request.get/post |
| P1 | `frontend/src/api/signup.ts` | P1-2 request.get/post |
| P1 | `frontend/src/views/teacher/PublishComp.vue` | P1-3 字段不对应 |
| P1 | `frontend/src/views/teacher/AwardInput.vue` | P1-4 奖项枚举 |
| P1 | `backend/.../service/impl/CompetitionServiceImpl.java` | P1-6 N+1 查询 |
| P2 | `frontend/src/views/StudentSignup.vue` | P2-1 teacherId 可选矛盾 |
| P2 | `backend/.../service/impl/TeamServiceImpl.java` | P2-2 未校验人数上限 |
| P2 | `backend/.../service/impl/RecruitmentServiceImpl.java` | P2-3 重复申请 |
| P2 | `backend/.../service/impl/AwardServiceImpl.java` | P2-4 重复获奖 |
| P2 | `backend/.../service/impl/CompetitionServiceImpl.java` | P2-5 字段更新不完整 |
| P2 | `frontend/src/types/signup.ts` | P2-6 状态枚举不一致 |
| P2 | `frontend/src/views/Home.vue` | P2-7 文字颜色不可见 |
| P3 | `frontend/src/views/admin/111.vue` | P3-1 临时测试文件 |
| P3 | `backend/.../service/impl/TeamServiceImpl.java` | P3-2 quitTeam 不完整 |
| P3 | `backend/.../service/impl/AiServiceImpl.java` | P3-5 共享对话历史 |

---

*报告结束。建议优先修复所有 P0 问题，再处理 P1，保证核心功能路径可用后再逐步推进 P2/P3。*
