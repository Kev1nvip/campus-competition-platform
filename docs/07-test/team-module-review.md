# 队伍模块业务逻辑审查报告

**审查日期：** 2026-06-13  
**文档范围：** 队伍模块全链路（创建队伍 → 邀请/招募 → 指导老师确认 → 提交审核）  
**审查结论：** 发现 **6 个 Bug**，其中 3 个会导致数据不一致，1 个影响消息通知，2 个影响前端显示逻辑

---

## 一、业务流程梳理（基于 PRD 3.3.2）

### 路径 B：学生主导（当前实现的路径）

```
Step1  学生创建队伍（成为队长）
         → DB: 插入 team 记录（status=FORMING, memberCount=1）
         → DB: 插入 team_member 记录（leaderId, role=LEADER）  ← 当前缺失

Step2  队长选择指导老师，发送带队申请
         → DB: 插入 apply_record（type=TEAM_GUIDE, receiverId=teacherId）
         → DB: 发通知给老师（type=APPLY_RECEIVED）

Step3  老师同意/拒绝带队
         同意：→ team.teacherConfirmed = true
               → team.teacherId = teacherId
               → 发通知给队长（type=APPLY_APPROVED）
         拒绝：→ apply_record.status = REJECTED
               → 发通知给队长（type=APPLY_REJECTED）

Step4  老师同意后，队长邀请队友（或发招募帖）
         直接邀请：
           → DB: 插入 apply_record（type=TEAM_INVITE, receiverId=targetUserId）
           → DB: 发通知给被邀请人（type=TEAM_INVITE）  ← 当前缺失

Step5  被邀请人同意/拒绝
         同意：→ team.memberCount + 1
               → DB: 插入 team_member 记录（studentId, role=MEMBER）  ← 当前缺失
               → 发通知给队长（type=APPLY_APPROVED）
         拒绝：→ apply_record.status = REJECTED
               → 发通知给队长（type=APPLY_REJECTED）

Step6  人数满足 minTeamSize 后，队长提交管理员审核
         → team_signup.status = PENDING
         → team.status = SUBMITTED
         → 发通知给管理员（type=AUDIT_SUBMITTED）

Step7  管理员审核
         通过：→ team_signup.status = APPROVED
               → 发通知给队长（type=AUDIT_APPROVED）
         驳回：→ team_signup.status = REJECTED
               → 发通知给老师（type=AUDIT_REJECTED）

Step8  修改后重新提交（状态变为 RESUBMITTED）
```

---

## 二、发现的 Bug

### Bug 1 【数据一致性 P0】：创建队伍时未插入 TeamMember 记录

**文件：** `service/impl/TeamServiceImpl.java:createTeam()`

**问题描述：**  
创建队伍时只插入了 `team` 表，但 `team_member` 表里没有队长的记录。后续查询成员列表（`teamMemberRepository.findByTeamId(teamId)`）返回空，`team.members` 为空数组，队伍详情页的成员列表永远为空。

**当前代码（问题处）：**
```java
Team team = Team.builder()
    .competitionId(dto.getCompetitionId())
    .teamName(dto.getTeamName())
    .leaderId(userId)
    .teacherConfirmed(false)
    .memberCount(1)
    .status("FORMING")
    .build();
teamRepository.save(team);
// 缺失：未插入 team_member 记录
```

**修复方案：**
```java
Team saved = teamRepository.save(team);
// 插入队长的成员记录
TeamMember leaderMember = TeamMember.builder()
    .teamId(saved.getId())
    .studentId(userId)
    .role("LEADER")
    .joinedAt(OffsetDateTime.now())
    .build();
teamMemberRepository.save(leaderMember);
```

---

### Bug 2 【数据一致性 P0】：同意邀请时未插入 TeamMember 记录

**文件：** `service/impl/TeamServiceImpl.java:handleInvite()`

**问题描述：**  
处理邀请同意时只更新了 `team.memberCount`，但未在 `team_member` 表插入新成员记录。  
- 成员列表页面永远只显示空（或只有队长，如 Bug1 修复后）
- `teamRepository.findByStudentId()` 的"我的队伍"队员查询无法找到该学生

**当前代码（问题处）：**
```java
if ("APPROVED".equals(status)) {
    Team team = teamRepository.findById(apply.getBizId())...;
    team.setMemberCount(team.getMemberCount() + 1);
    teamRepository.save(team);
    apply.setStatus("APPROVED");
    // 缺失：未插入 team_member 记录
}
```

**修复方案：**
```java
if ("APPROVED".equals(status)) {
    Team team = teamRepository.findById(apply.getBizId())...;
    team.setMemberCount(team.getMemberCount() + 1);
    teamRepository.save(team);
    apply.setStatus("APPROVED");
    // 插入成员记录
    TeamMember member = TeamMember.builder()
        .teamId(team.getId())
        .studentId(userId)   // 被邀请人（接受邀请的人）
        .role("MEMBER")
        .joinedAt(OffsetDateTime.now())
        .build();
    teamMemberRepository.save(member);
}
```

---

### Bug 3 【数据一致性 P0】：退出队伍时未删除 TeamMember 记录

**文件：** `service/impl/TeamServiceImpl.java:quitTeam()`

**问题描述：**  
退出队伍只减少了 `team.memberCount`，但未删除 `team_member` 表对应记录。  
- 成员列表仍然显示已退出的成员
- `teamRepository.findByStudentId()` 仍然能查到已退出的队伍

**修复方案：**
```java
// 删除 team_member 记录
teamMemberRepository.findByTeamId(teamId).stream()
    .filter(m -> m.getStudentId().equals(userId))
    .findFirst()
    .ifPresent(teamMemberRepository::delete);
```

---

### Bug 4 【消息通知 P1】：邀请队友时未发送通知

**文件：** `service/impl/TeamServiceImpl.java:inviteMember()`

**问题描述：**  
调用邀请接口后，被邀请人没有收到任何通知，不知道有邀请待处理，无法同意/拒绝。  
这是"邀请后收不到消息"问题的直接原因。

**修复方案：**  
在 `inviteMember` 中注入 `NotificationService`，保存 `apply_record` 后发送通知：
```java
notificationService.send(
    targetUserId,
    "TEAM_INVITE",
    "你收到一个队伍邀请",
    "队长 [队长姓名] 邀请你加入队伍 [队伍名]，请查看并处理。",
    teamId
);
```

---

### Bug 5 【前端显示 P1】："退出队伍"按钮出现在未加入的队伍

**文件：** `frontend/src/views/TeamDetail.vue`

**问题描述：**  
按钮显示条件是 `!isLeader && team.status === 'FORMING'`，但未判断当前用户是否是该队伍成员。  
在"队伍大厅"点进任意一个非自己队伍的详情时，所有非队长用户都会看到"退出队伍"按钮。

**当前代码（问题处）：**
```html
<el-button v-if="!isLeader && team.status === 'FORMING'">退出队伍</el-button>
```

**修复方案：**  
后端详情接口需要返回 `isMember` 字段（判断当前请求用户是否是成员），或者前端从 `team.members` 中检查：
```js
const isMember = computed(() =>
  team.value?.members?.some((m: any) => m.studentId === currentUserId.value)
)
```
按钮条件改为：
```html
<el-button v-if="isMember && !isLeader && team.status === 'FORMING'">退出队伍</el-button>
```

---

### Bug 6 【缺失功能 P1】：被邀请人无法看到待处理邀请列表

**问题描述：**  
目前没有任何前端页面展示"我收到的邀请"列表，被邀请人即使通过通知知道有邀请，也无法找到接受/拒绝的入口。  

`apply_record` 表里有数据，后端也有 `PUT /api/v1/team/invite/{applyId}` 接口，但前端完全没有接入。

**修复方案：**  
在"我的队伍"页面或"个人中心"添加"待处理邀请"section，从 `ApplyRecordRepository.findByReceiverId()` 查询 `type=TEAM_INVITE AND status=PENDING` 的记录。

---

## 三、数据库表关系说明

```
team (id, competition_id, team_name, leader_id, teacher_id, teacher_confirmed, member_count, status)
  ↕ 1:N
team_member (id, team_id, student_id, role[LEADER/MEMBER], joined_at)

apply_record (id, type, applicant_id, receiver_id, biz_id, status)
  type=TEAM_INVITE  → biz_id = team.id
  type=TEAM_GUIDE   → biz_id = team.id (队长向老师发带队申请)

team_signup (id, competition_id, team_id, teacher_id, status)
  → 队长提交审核后创建

team_recruitment (id, competition_id, team_id, leader_id, recruit_count, status)
  → 队长发布招募帖后创建
```

**关键不变式（当前代码违反了这些）：**
- `team.member_count` 必须等于 `team_member WHERE team_id=?` 的记录数
- 创建队伍时，队长必须有一条 `team_member(role=LEADER)` 记录
- 同意邀请时，新成员必须有一条 `team_member(role=MEMBER)` 记录
- 退出队伍时，必须删除对应 `team_member` 记录

---

## 四、前端状态机（TeamDetail 页面操作权限）

| 用户身份 | 队伍状态 | 可见按钮 |
|---------|---------|---------|
| 队长 | FORMING | 邀请队友、提交审核（人数满足时）、解散队伍 |
| 队员 | FORMING | 退出队伍 |
| 非成员 | FORMING | 申请加入（当有招募帖时） |
| 任何人 | SUBMITTED/APPROVED | 仅查看，无操作按钮 |
| 任何人 | REJECTED | 队长可重新提交 |

**当前前端缺少判断"是否为成员"的逻辑，导致非成员也显示"退出队伍"。**

---

## 五、修复优先级

| 编号 | 文件 | 优先级 | 修复内容 |
|------|------|--------|---------|
| Bug1 | `TeamServiceImpl.createTeam` | P0 | 创建队伍时插入队长的 `team_member` 记录 |
| Bug2 | `TeamServiceImpl.handleInvite` | P0 | 同意邀请时插入 `team_member` 记录 |
| Bug3 | `TeamServiceImpl.quitTeam` | P0 | 退出队伍时删除 `team_member` 记录 |
| Bug4 | `TeamServiceImpl.inviteMember` | P1 | 邀请时调用 `notificationService.send()` 发通知 |
| Bug5 | `TeamDetail.vue` | P1 | "退出队伍"按钮加 `isMember` 判断条件 |
| Bug6 | `MyTeams.vue` 或新建页面 | P1 | 新增"待处理邀请"列表，接入同意/拒绝接口 |
