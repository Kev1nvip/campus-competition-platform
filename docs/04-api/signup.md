# 报名模块接口文档

> 模块：signup
> 前缀：/api/v1/signups
> 说明：个人赛报名和团队赛报名的核心流程接口

---

## 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/v1/signups/individual | 个人赛报名（创建草稿+发送指导申请） | 学生 |
| POST | /api/v1/signups/individual/{id}/submit | 提交个人赛审核（首次/重新提交） | 学生本人 |
| GET | /api/v1/signups/individual/my | 我的个人赛报名列表 | 学生 |
| GET | /api/v1/signups/individual/{id} | 个人赛报名详情 | 登录用户 |
| POST | /api/v1/signups/team | 创建团队赛报名草稿 | 队长 |
| POST | /api/v1/signups/team/{id}/submit | 提交团队赛审核（首次/重新提交） | 队长 |
| GET | /api/v1/signups/team/my | 我的团队赛报名列表 | 学生 |
| GET | /api/v1/signups/team/{id} | 团队赛报名详情 | 登录用户 |

---

## 状态机说明

### 个人赛报名状态（individual_signup.status）

```
DRAFT ──────────────────────────────→ PENDING
（创建草稿，老师已同意）          （学生提交审核）
                                          │
                          ┌───────────────┤
                          ↓               ↓
                       APPROVED        REJECTED
                      （审核通过）    （审核驳回）
                                          │
                                          ↓
                                       PENDING
                                  （学生重新提交）
```

**状态流转规则**

| 当前状态 | 允许的操作 | 目标状态 |
|---------|-----------|---------|
| DRAFT | 学生提交审核（老师已同意后） | PENDING |
| PENDING | 管理员审核通过 | APPROVED |
| PENDING | 管理员审核驳回 | REJECTED |
| REJECTED | 学生修改后重新提交 | RESUBMITTED → PENDING |
| APPROVED | 无（终态） | - |

### 团队赛报名状态（team_signup.status）

```
DRAFT ──────────────────────────────→ PENDING
（队长创建草稿）                  （队长提交审核）
                                          │
                          ┌───────────────┤
                          ↓               ↓
                       APPROVED        REJECTED
                      （审核通过）    （审核驳回）
                                          │
                                          ↓
                                       PENDING
                                  （队长重新提交）
```

**状态流转规则**

| 当前状态 | 允许的操作 | 目标状态 |
|---------|-----------|---------|
| DRAFT | 队长提交审核（人数满足要求且老师已确认） | PENDING |
| PENDING | 管理员审核通过 | APPROVED |
| PENDING | 管理员审核驳回 | REJECTED |
| REJECTED | 队长修改后重新提交 | RESUBMITTED → PENDING |
| APPROVED | 无（终态） | - |

---

## 个人赛接口

---

### 1. 个人赛报名

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/signups/individual |
| 权限要求 | 学生（STUDENT） |
| Content-Type | application/json |

#### 业务说明

```
本接口在一个事务内完成两件事：
  1. 创建 individual_signup 记录，状态为 DRAFT
  2. 创建 apply_record 记录，类型为 INDIVIDUAL_GUIDE，向目标老师发送指导申请

老师同意申请后，学生才可以调用 submit 接口提交审核
老师拒绝申请后，学生可以重新调用本接口选择其他老师（旧草稿作废）
```

#### 请求体

```json
{
  "competitionId": 1,
  "teacherId": 10,
  "motivation": "我对算法竞赛有浓厚兴趣，希望通过参赛提升自己的编程能力",
  "introduction": "本人大二学生，熟悉Java和Python，有ACM校赛经验"
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|---------|------|
| competitionId | Long | 是 | 大于0 | 竞赛ID |
| teacherId | Long | 是 | 大于0 | 指导老师ID，必须是TEACHER角色 |
| motivation | String | 否 | 最长500字 | 参赛动机 |
| introduction | String | 否 | 最长500字 | 个人简介 |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "signupId": 100,
    "applyId": 200,
    "status": "DRAFT",
    "message": "报名草稿已创建，指导申请已发送给老师，请等待老师确认"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| applyId | Long | 指导申请记录ID |
| status | String | 报名状态，固定为 DRAFT |
| message | String | 提示信息 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40000 | 请求参数错误 | 参数校验不通过 |
| 40001 | 参数不能为空 | 必填字段为空 |
| 40105 | 用户不存在 | teacherId 无效或对应用户不是TEACHER角色 |
| 40120 | 竞赛不存在 | competitionId 无效 |
| 40121 | 竞赛不在报名时间内 | 竞赛状态不是 SIGNING |
| 40122 | 竞赛名额已满 | enrolled_count >= max_quota |
| 40130 | 已报名该竞赛 | 同一学生同一竞赛已有报名记录 |
| 40133 | 老师带队名额已满 | 该老师在该竞赛的带队数已达上限 |

#### 错误响应示例

```json
{
  "code": 40133,
  "message": "老师带队名额已满",
  "data": null
}
```

#### 业务规则

```
1. 调用方必须是 STUDENT 角色
2. 竞赛必须处于 SIGNING 状态
3. 竞赛有名额限制时，已满则拒绝（Redis计数器校验）
4. 同一学生同一竞赛只能有一条报名记录，重复报名返回 40130
5. teacherId 必须是 TEACHER 角色的用户
6. 竞赛配置了 maxTeachQuota 时，校验该老师当前带队数是否已满
7. 以上校验全部通过后，事务内同时写入 individual_signup 和 apply_record 两张表
8. 事务失败则全部回滚
```

---

### 2. 提交个人赛审核

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/signups/individual/{id}/submit |
| 权限要求 | 报名记录的本人（STUDENT） |
| Content-Type | application/json |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 个人赛报名记录ID |

#### 请求体

**首次提交（status=DRAFT）时，无需请求体**

**重新提交（status=REJECTED）时，可携带修改后的内容**

```json
{
  "motivation": "修改后的参赛动机",
  "introduction": "修改后的个人简介"
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|---------|------|
| motivation | String | 否 | 最长500字 | 重新提交时可修改，为空则保留原值 |
| introduction | String | 否 | 最长500字 | 重新提交时可修改，为空则保留原值 |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "signupId": 100,
    "status": "PENDING",
    "submittedAt": "2026-04-01T10:00:00+08:00"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| status | String | 提交后的状态，固定为 PENDING |
| submittedAt | String | 提交时间，ISO8601格式 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40131 | 报名记录不存在 | id 无效 |
| 40300 | 无操作权限 | 操作人不是报名本人 |
| 40132 | 当前状态不允许该操作 | status 不是 DRAFT 或 REJECTED |
| 40000 | 请求参数错误 | 老师尚未同意指导申请 |

#### 错误响应示例

```json
{
  "code": 40132,
  "message": "当前状态不允许该操作",
  "data": null
}
```

#### 业务规则

```
1. 只有报名记录的本人可以操作
2. 状态必须是 DRAFT 或 REJECTED，否则返回 40132
3. 提交前校验：对应的 apply_record 状态必须是 APPROVED（老师已同意）
   未同意时返回 40000，提示"老师尚未同意指导申请，不能提交审核"
4. DRAFT → PENDING：
   状态更新为 PENDING，记录 submitted_at 时间
5. REJECTED → PENDING：
   状态先更新为 RESUBMITTED 再更新为 PENDING，submitted_at 更新为当前时间
   最终数据库存储状态为 PENDING
6. 有修改内容时更新对应字段，无修改则保留原值
```

---

### 3. 我的个人赛报名列表

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | GET |
| 请求路径 | /api/v1/signups/individual/my |
| 权限要求 | 学生（STUDENT） |

#### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|-------|------|
| page | Integer | 否 | 1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 每页数量，最大20 |
| status | String | 否 | 无 | 按状态筛选：DRAFT/PENDING/APPROVED/REJECTED |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "signupId": 100,
        "competitionId": 1,
        "competitionTitle": "第十五届蓝桥杯全国软件和信息技术专业人才大赛",
        "competitionStatus": "SIGNING",
        "teacherId": 10,
        "teacherName": "李老师",
        "teacherTitle": "副教授",
        "status": "PENDING",
        "applyStatus": "APPROVED",
        "submittedAt": "2026-04-01T10:00:00+08:00",
        "createdAt": "2026-03-28T14:00:00+08:00"
      }
    ],
    "total": 3,
    "page": 1,
    "size": 10,
    "totalPages": 1
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| competitionId | Long | 竞赛ID |
| competitionTitle | String | 竞赛名称 |
| competitionStatus | String | 竞赛当前状态 |
| teacherId | Long | 指导老师ID |
| teacherName | String | 指导老师姓名 |
| teacherTitle | String | 指导老师职称 |
| status | String | 报名状态 |
| applyStatus | String | 指导申请状态：PENDING/APPROVED/REJECTED |
| submittedAt | String | 提交审核时间，未提交为null |
| createdAt | String | 创建时间 |

---

### 4. 个人赛报名详情

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | GET |
| 请求路径 | /api/v1/signups/individual/{id} |
| 权限要求 | 登录用户（本人/对应老师/管理员） |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 个人赛报名记录ID |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "signupId": 100,
    "competition": {
      "competitionId": 1,
      "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛",
      "type": "INDIVIDUAL",
      "organizer": "工业和信息化部人才交流中心",
      "signupStart": "2026-03-01T00:00:00+08:00",
      "signupEnd": "2026-04-30T23:59:59+08:00",
      "status": "SIGNING"
    },
    "student": {
      "userId": 5,
      "username": "zhangsan",
      "realName": "张三",
      "studentNo": "2021010001",
      "department": "计算机学院"
    },
    "teacher": {
      "userId": 10,
      "realName": "李老师",
      "title": "副教授",
      "department": "计算机学院"
    },
    "motivation": "我对算法竞赛有浓厚兴趣",
    "introduction": "本人大二学生，熟悉Java和Python",
    "status": "PENDING",
    "applyStatus": "APPROVED",
    "rejectReason": null,
    "submittedAt": "2026-04-01T10:00:00+08:00",
    "createdAt": "2026-03-28T14:00:00+08:00",
    "updatedAt": "2026-04-01T10:00:00+08:00"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| competition | Object | 竞赛基本信息 |
| competition.competitionId | Long | 竞赛ID |
| competition.title | String | 竞赛名称 |
| competition.type | String | 竞赛类型 |
| competition.organizer | String | 主办方 |
| competition.signupStart | String | 报名开始时间 |
| competition.signupEnd | String | 报名截止时间 |
| competition.status | String | 竞赛状态 |
| student | Object | 报名学生信息 |
| student.userId | Long | 学生ID |
| student.username | String | 用户名 |
| student.realName | String | 真实姓名 |
| student.studentNo | String | 学号 |
| student.department | String | 院系 |
| teacher | Object | 指导老师信息 |
| teacher.userId | Long | 老师ID |
| teacher.realName | String | 真实姓名 |
| teacher.title | String | 职称 |
| teacher.department | String | 院系 |
| motivation | String | 参赛动机，可为null |
| introduction | String | 个人简介，可为null |
| status | String | 报名状态 |
| applyStatus | String | 指导申请状态 |
| rejectReason | String | 驳回原因，未驳回为null |
| submittedAt | String | 提交审核时间，未提交为null |
| createdAt | String | 创建时间 |
| updatedAt | String | 最后更新时间 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40131 | 报名记录不存在 | id 无效 |
| 40300 | 无操作权限 | 既不是本人，也不是对应老师，也不是管理员 |

---

## 团队赛接口

---

### 5. 创建团队赛报名草稿

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/signups/team |
| 权限要求 | 队长（STUDENT，且是对应队伍的队长） |
| Content-Type | application/json |

#### 业务说明

```
调用前置条件（后端校验）：
  1. 队伍已存在（通过 POST /teams 创建）
  2. 队伍的 teacher_confirmed = TRUE（老师已确认带队）
  3. 队伍状态为 FORMING 或 FULL
  4. 该队伍尚未创建报名记录
  5. 竞赛处于 SIGNING 状态

不校验人数，人数校验在 submit 时进行

调用后：
  创建 team_signup 记录，状态为 DRAFT
  teacher_id 和 competition_id 由后端从 team 表自动读取，前端不传
```

#### 请求体

```json
{
  "teamId": 50
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|---------|------|
| teamId | Long | 是 | 大于0 | 队伍ID，必须是调用人担任队长的队伍 |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "signupId": 200,
    "teamId": 50,
    "competitionId": 2,
    "teacherId": 10,
    "status": "DRAFT",
    "message": "团队赛报名草稿已创建，满足人数要求后可提交审核"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 团队赛报名记录ID |
| teamId | Long | 队伍ID |
| competitionId | Long | 竞赛ID（从队伍读取） |
| teacherId | Long | 指导老师ID（从队伍读取） |
| status | String | 固定为 DRAFT |
| message | String | 提示信息 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40140 | 队伍不存在 | teamId 无效 |
| 40143 | 无队长权限 | 调用人不是该队伍的队长 |
| 40145 | 老师尚未确认带队 | teacher_confirmed = FALSE |
| 40121 | 竞赛不在报名时间内 | 竞赛状态不是 SIGNING |
| 40122 | 竞赛名额已满 | enrolled_count >= max_quota |
| 40130 | 已报名该竞赛 | 该队伍已有报名记录 |

#### 错误响应示例

```json
{
  "code": 40145,
  "message": "老师尚未确认带队，不能发布招募帖",
  "data": null
}
```

#### 业务规则

```
1. 调用方必须是该队伍的队长
2. 队伍的 teacher_confirmed 必须为 TRUE
3. 竞赛必须处于 SIGNING 状态
4. 竞赛有名额限制时，已满则拒绝（Redis计数器校验）
5. 该队伍在该竞赛不能已有报名记录
6. 不校验队伍当前人数，人数校验在 submit 接口进行
7. teacher_id 和 competition_id 由后端从 team 表读取，不从前端接收
```

---

### 6. 提交团队赛审核

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/signups/team/{id}/submit |
| 权限要求 | 队长（STUDENT，且是对应队伍的队长） |
| Content-Type | application/json |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 团队赛报名记录ID |

#### 请求体

**无需请求体**

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "signupId": 200,
    "status": "PENDING",
    "submittedAt": "2026-04-05T09:00:00+08:00"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| status | String | 提交后状态，固定为 PENDING |
| submittedAt | String | 提交时间，ISO8601格式 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40131 | 报名记录不存在 | id 无效 |
| 40143 | 无队长权限 | 调用人不是该队伍的队长 |
| 40132 | 当前状态不允许该操作 | status 不是 DRAFT 或 REJECTED |
| 40000 | 请求参数错误 | 队伍人数不满足竞赛最少人数要求 |

#### 错误响应示例

```json
{
  "code": 40000,
  "message": "队伍人数不足，当前3人，该竞赛要求至少4人",
  "data": null
}
```

#### 业务规则

```
1. 只有队长可以操作
2. 状态必须是 DRAFT 或 REJECTED，否则返回 40132
3. 提交时校验队伍当前人数 >= 竞赛 min_team_size
   不满足时返回 40000，错误信息中包含当前人数和要求人数
4. DRAFT → PENDING：
   状态更新为 PENDING，记录 submitted_at 时间
   同时将队伍状态更新为 SUBMITTED
5. REJECTED → PENDING：
   状态先更新为 RESUBMITTED 再更新为 PENDING，submitted_at 更新为当前时间
   最终数据库存储状态为 PENDING
```

---

### 7. 我的团队赛报名列表

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | GET |
| 请求路径 | /api/v1/signups/team/my |
| 权限要求 | 学生（STUDENT） |

#### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|-------|------|
| page | Integer | 否 | 1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 每页数量，最大20 |
| status | String | 否 | 无 | 按状态筛选：DRAFT/PENDING/APPROVED/REJECTED |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "signupId": 200,
        "competitionId": 2,
        "competitionTitle": "2026年全国大学生数学建模竞赛",
        "competitionStatus": "SIGNING",
        "teamId": 50,
        "teamName": "建模先锋队",
        "memberCount": 3,
        "minTeamSize": 2,
        "maxTeamSize": 4,
        "teacherId": 10,
        "teacherName": "王老师",
        "myRole": "LEADER",
        "status": "PENDING",
        "submittedAt": "2026-04-05T09:00:00+08:00",
        "createdAt": "2026-04-01T10:00:00+08:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10,
    "totalPages": 1
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| competitionId | Long | 竞赛ID |
| competitionTitle | String | 竞赛名称 |
| competitionStatus | String | 竞赛当前状态 |
| teamId | Long | 队伍ID |
| teamName | String | 队伍名称 |
| memberCount | Integer | 当前队伍人数 |
| minTeamSize | Integer | 竞赛要求最少人数 |
| maxTeamSize | Integer | 竞赛要求最多人数 |
| teacherId | Long | 指导老师ID |
| teacherName | String | 指导老师姓名 |
| myRole | String | 当前用户在队伍中的角色：LEADER/MEMBER |
| status | String | 报名状态 |
| submittedAt | String | 提交审核时间，未提交为null |
| createdAt | String | 创建时间 |

---

### 8. 团队赛报名详情

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | GET |
| 请求路径 | /api/v1/signups/team/{id} |
| 权限要求 | 登录用户（队伍成员/对应老师/管理员） |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 团队赛报名记录ID |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "signupId": 200,
    "competition": {
      "competitionId": 2,
      "title": "2026年全国大学生数学建模竞赛",
      "type": "TEAM",
      "organizer": "中国工业与应用数学学会",
      "minTeamSize": 2,
      "maxTeamSize": 4,
      "signupStart": "2026-03-01T00:00:00+08:00",
      "signupEnd": "2026-05-31T23:59:59+08:00",
      "status": "SIGNING"
    },
    "team": {
      "teamId": 50,
      "teamName": "建模先锋队",
      "memberCount": 3,
      "members": [
        {
          "userId": 5,
          "realName": "张三",
          "studentNo": "2021010001",
          "department": "计算机学院",
          "role": "LEADER"
        },
        {
          "userId": 6,
          "realName": "李四",
          "studentNo": "2021010002",
          "department": "数学学院",
          "role": "MEMBER"
        },
        {
          "userId": 7,
          "realName": "王五",
          "studentNo": "2021010003",
          "department": "计算机学院",
          "role": "MEMBER"
        }
      ]
    },
    "teacher": {
      "userId": 10,
      "realName": "王老师",
      "title": "教授",
      "department": "数学学院"
    },
    "status": "PENDING",
    "rejectReason": null,
    "submittedAt": "2026-04-05T09:00:00+08:00",
    "createdAt": "2026-04-01T10:00:00+08:00",
    "updatedAt": "2026-04-05T09:00:00+08:00"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| signupId | Long | 报名记录ID |
| competition | Object | 竞赛基本信息 |
| competition.competitionId | Long | 竞赛ID |
| competition.title | String | 竞赛名称 |
| competition.type | String | 竞赛类型，固定为 TEAM |
| competition.organizer | String | 主办方 |
| competition.minTeamSize | Integer | 最少队伍人数 |
| competition.maxTeamSize | Integer | 最多队伍人数 |
| competition.signupStart | String | 报名开始时间 |
| competition.signupEnd | String | 报名截止时间 |
| competition.status | String | 竞赛状态 |
| team | Object | 队伍信息 |
| team.teamId | Long | 队伍ID |
| team.teamName | String | 队伍名称 |
| team.memberCount | Integer | 当前成员数量 |
| team.members | Array | 成员列表 |
| team.members[].userId | Long | 成员ID |
| team.members[].realName | String | 成员姓名 |
| team.members[].studentNo | String | 成员学号 |
| team.members[].department | String | 成员院系 |
| team.members[].role | String | 成员角色：LEADER/MEMBER |
| teacher | Object | 指导老师信息 |
| teacher.userId | Long | 老师ID |
| teacher.realName | String | 真实姓名 |
| teacher.title | String | 职称 |
| teacher.department | String | 院系 |
| status | String | 报名状态 |
| rejectReason | String | 驳回原因，未驳回为null |
| submittedAt | String | 提交审核时间，未提交为null |
| createdAt | String | 创建时间 |
| updatedAt | String | 最后更新时间 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40131 | 报名记录不存在 | id 无效 |
| 40300 | 无操作权限 | 不是队伍成员、对应老师或管理员 |

---

## 并发控制说明

```
涉及并发控制的场景：

1. 竞赛名额控制
   读：Redis GET competition:quota:{competitionId}
   写：Redis DECR，成功后异步同步到数据库 enrolled_count
   冲突：DECR后值<0，执行INCR回滚，返回 40122

2. 老师带队数量控制
   读：Redis GET teacher:quota:{competitionId}:{teacherId}
   写：Redis INCR，同时数据库乐观锁校验
   冲突：version不匹配，返回 40133

3. 乐观锁
   competition 表有 version 字段
   更新时携带 WHERE version = {oldVersion}
   影响行数为0时，说明并发冲突，重试或返回失败
```