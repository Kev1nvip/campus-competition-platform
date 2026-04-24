# 竞赛模块接口文档

> 模块：competition
> 前缀：/api/v1/competitions
> 说明：竞赛的发布、编辑、状态管理、列表查询接口

---

## 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/v1/competitions | 竞赛列表 | 登录用户 |
| GET | /api/v1/competitions/{id} | 竞赛详情 | 登录用户 |
| POST | /api/v1/competitions | 发布竞赛 | 管理员/老师 |
| PUT | /api/v1/competitions/{id} | 编辑竞赛 | 管理员/发布人 |
| PATCH | /api/v1/competitions/{id}/status | 变更竞赛状态 | 管理员/发布人 |

---

## 状态机说明

### 竞赛状态（competition.status）

```
UPCOMING（未开始）
    │
    │ 到达 signupStart，定时任务自动切换
    ↓
SIGNING（报名中）
    │
    │ 到达 signupEnd，定时任务自动切换
    ↓
CLOSED（报名截止）
    │
    │ 到达 competitionStart，定时任务自动切换
    │ competitionStart 为空时跳过此状态直接到 FINISHED
    ↓
ONGOING（进行中）
    │
    │ 到达 competitionEnd，定时任务自动切换
    │ competitionEnd 为空时保持 ONGOING 直到手动操作
    ↓
FINISHED（已结束）

手动操作（任意状态均可）：
  发布人/管理员 → OFFLINE（下架）
  OFFLINE → 恢复到当前时间对应的正确状态（重新上架）
```

**状态说明**

| 状态 | 说明 | 学生是否可见 | 是否可报名 |
|------|------|------------|----------|
| UPCOMING | 未到报名时间 | ✅ | ❌ |
| SIGNING | 报名进行中 | ✅ | ✅ |
| CLOSED | 报名已截止 | ✅ | ❌ |
| ONGOING | 比赛进行中 | ✅ | ❌ |
| FINISHED | 比赛已结束 | ✅ | ❌ |
| OFFLINE | 已下架 | ❌ | ❌ |

**定时任务说明**

```
实现方式：Spring @Scheduled 定时任务
执行频率：每5分钟扫描一次
扫描逻辑：
  UPDATE competition SET status = 'SIGNING'
    WHERE status = 'UPCOMING' AND signup_start <= NOW()
  UPDATE competition SET status = 'CLOSED'
    WHERE status = 'SIGNING' AND signup_end <= NOW()
  UPDATE competition SET status = 'ONGOING'
    WHERE status = 'CLOSED'
      AND competition_start IS NOT NULL
      AND competition_start <= NOW()
  UPDATE competition SET status = 'FINISHED'
    WHERE status IN ('CLOSED', 'ONGOING')
      AND competition_end IS NOT NULL
      AND competition_end <= NOW()
```

---

## 编辑限制说明

```
不同状态下允许编辑的字段：

UPCOMING（未开始）：
  允许编辑全部字段

SIGNING（报名中）：
  不允许修改：type、signupStart、hasQuota
  允许修改：title、organizer、requirement、signupEnd、
            competitionStart、competitionEnd、maxQuota、
            minTeamSize、maxTeamSize、maxTeachQuota、
            description、attachmentUrl
  注意：maxQuota 只能增大，不能小于当前 enrolled_count

CLOSED / ONGOING：
  不允许修改：type、signupStart、signupEnd、hasQuota、
              maxQuota、minTeamSize、maxTeamSize
  允许修改：title、organizer、requirement、competitionStart、
            competitionEnd、maxTeachQuota、description、attachmentUrl

FINISHED：
  不允许修改任何字段，返回 40132

OFFLINE：
  允许修改全部字段（为重新上架做准备）
```

---

## 接口详情

---

### 1. 竞赛列表

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | GET |
| 请求路径 | /api/v1/competitions |
| 权限要求 | 登录用户 |

#### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|-------|------|
| page | Integer | 否 | 1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 每页数量，最大20 |
| status | String | 否 | 无 | 状态筛选：UPCOMING/SIGNING/CLOSED/ONGOING/FINISHED/OFFLINE |
| type | String | 否 | 无 | 类型筛选：INDIVIDUAL/TEAM |
| keyword | String | 否 | 无 | 竞赛名称模糊搜索，最长50字 |

#### 权限与可见范围

```
学生/老师：
  只能看到 status != OFFLINE 的竞赛
  status 筛选参数传入 OFFLINE 时，忽略该参数

管理员：
  可以看到所有状态的竞赛，包括 OFFLINE
  status 筛选参数完全生效
```

#### 排序规则

```
第一优先级：status 排序
  SIGNING > UPCOMING > CLOSED > ONGOING > FINISHED > OFFLINE

第二优先级：signup_end 升序
  同一状态内，报名截止时间越近越靠前
```

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "competitionId": 1,
        "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛",
        "type": "INDIVIDUAL",
        "organizer": "工业和信息化部人才交流中心",
        "status": "SIGNING",
        "signupStart": "2026-03-01T00:00:00+08:00",
        "signupEnd": "2026-04-30T23:59:59+08:00",
        "competitionStart": "2026-05-01T00:00:00+08:00",
        "competitionEnd": "2026-05-02T23:59:59+08:00",
        "hasQuota": true,
        "maxQuota": 100,
        "remainingQuota": 38,
        "createdBy": 2,
        "createdByName": "李老师",
        "createdAt": "2026-02-01T10:00:00+08:00"
      }
    ],
    "total": 25,
    "page": 1,
    "size": 10,
    "totalPages": 3
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| competitionId | Long | 竞赛ID |
| title | String | 竞赛名称 |
| type | String | 竞赛类型：INDIVIDUAL/TEAM |
| organizer | String | 主办方 |
| status | String | 竞赛状态 |
| signupStart | String | 报名开始时间 |
| signupEnd | String | 报名截止时间 |
| competitionStart | String | 比赛开始时间，可为null |
| competitionEnd | String | 比赛结束时间，可为null |
| hasQuota | Boolean | 是否有名额限制 |
| maxQuota | Integer | 名额上限，hasQuota为false时为null |
| remainingQuota | Integer | 剩余名额，hasQuota为false时为null，从Redis读取 |
| createdBy | Long | 发布人ID |
| createdByName | String | 发布人姓名 |
| createdAt | String | 发布时间 |

---

### 2. 竞赛详情

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | GET |
| 请求路径 | /api/v1/competitions/{id} |
| 权限要求 | 登录用户 |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 竞赛ID |

#### 权限控制

```
OFFLINE 状态的竞赛：
  学生/老师访问：返回 40400（资源不存在）
  管理员/发布人访问：正常返回
```

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "competitionId": 1,
    "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛",
    "type": "INDIVIDUAL",
    "organizer": "工业和信息化部人才交流中心",
    "requirement": "全国在校大学生，编程基础扎实",
    "signupStart": "2026-03-01T00:00:00+08:00",
    "signupEnd": "2026-04-30T23:59:59+08:00",
    "competitionStart": "2026-05-01T00:00:00+08:00",
    "competitionEnd": "2026-05-02T23:59:59+08:00",
    "hasQuota": true,
    "maxQuota": 100,
    "enrolledCount": 62,
    "remainingQuota": 38,
    "minTeamSize": null,
    "maxTeamSize": null,
    "maxTeachQuota": 3,
    "description": "蓝桥杯全国软件和信息技术专业人才大赛是由工业和信息化部人才交流中心举办...",
    "attachmentUrl": "/uploads/attachments/2026/02/notice.pdf",
    "status": "SIGNING",
    "createdBy": 2,
    "createdByName": "李老师",
    "createdAt": "2026-02-01T10:00:00+08:00",
    "updatedAt": "2026-03-01T00:00:00+08:00",
    "mySignupStatus": "PENDING"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| competitionId | Long | 竞赛ID |
| title | String | 竞赛名称 |
| type | String | 竞赛类型：INDIVIDUAL/TEAM |
| organizer | String | 主办方 |
| requirement | String | 参赛要求，可为null |
| signupStart | String | 报名开始时间 |
| signupEnd | String | 报名截止时间 |
| competitionStart | String | 比赛开始时间，可为null |
| competitionEnd | String | 比赛结束时间，可为null |
| hasQuota | Boolean | 是否有名额限制 |
| maxQuota | Integer | 名额上限，hasQuota为false时为null |
| enrolledCount | Integer | 已报名人数 |
| remainingQuota | Integer | 剩余名额，hasQuota为false时为null |
| minTeamSize | Integer | 最少队伍人数，个人赛为null |
| maxTeamSize | Integer | 最多队伍人数，个人赛为null |
| maxTeachQuota | Integer | 每位老师最多带队数，null表示不限制 |
| description | String | 竞赛详情，可为null |
| attachmentUrl | String | 附件地址，可为null |
| status | String | 竞赛状态 |
| createdBy | Long | 发布人ID |
| createdByName | String | 发布人姓名 |
| createdAt | String | 发布时间 |
| updatedAt | String | 最后更新时间 |
| mySignupStatus | String | 当前用户的报名状态，未报名为null，管理员固定返回null |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40400 | 资源不存在 | id无效，或竞赛已下架且调用方无权查看 |

---

### 3. 发布竞赛

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/competitions |
| 权限要求 | 管理员（ADMIN）或老师（TEACHER） |
| Content-Type | application/json |

#### 请求体

```json
{
  "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛",
  "type": "INDIVIDUAL",
  "organizer": "工业和信息化部人才交流中心",
  "requirement": "全国在校大学生，编程基础扎实",
  "signupStart": "2026-03-01T00:00:00+08:00",
  "signupEnd": "2026-04-30T23:59:59+08:00",
  "competitionStart": "2026-05-01T00:00:00+08:00",
  "competitionEnd": "2026-05-02T23:59:59+08:00",
  "hasQuota": true,
  "maxQuota": 100,
  "minTeamSize": null,
  "maxTeamSize": null,
  "maxTeachQuota": 3,
  "description": "蓝桥杯全国软件和信息技术专业人才大赛...",
  "attachmentUrl": "/uploads/attachments/2026/02/notice.pdf"
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|---------|------|
| title | String | 是 | 1-128字 | 竞赛名称 |
| type | String | 是 | INDIVIDUAL/TEAM | 竞赛类型 |
| organizer | String | 是 | 1-128字 | 主办方 |
| requirement | String | 否 | 最长1000字 | 参赛要求 |
| signupStart | String | 是 | ISO8601，必须晚于当前时间 | 报名开始时间 |
| signupEnd | String | 是 | ISO8601，必须晚于signupStart | 报名截止时间 |
| competitionStart | String | 否 | ISO8601，填写时必须晚于signupEnd | 比赛开始时间 |
| competitionEnd | String | 否 | ISO8601，填写时必须晚于competitionStart | 比赛结束时间 |
| hasQuota | Boolean | 是 | - | 是否有名额限制 |
| maxQuota | Integer | 条件必填 | hasQuota为true时必填，最小值1 | 名额上限 |
| minTeamSize | Integer | 条件必填 | type为TEAM时必填，最小值2 | 最少队伍人数 |
| maxTeamSize | Integer | 条件必填 | type为TEAM时必填，必须>=minTeamSize | 最多队伍人数 |
| maxTeachQuota | Integer | 否 | 最小值1 | 每位老师最多带队数，不填表示不限制 |
| description | String | 否 | 最长5000字 | 竞赛详情 |
| attachmentUrl | String | 否 | 最长512字 | 附件地址 |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "competitionId": 1,
    "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛",
    "status": "UPCOMING",
    "createdAt": "2026-02-01T10:00:00+08:00"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| competitionId | Long | 新建竞赛ID |
| title | String | 竞赛名称 |
| status | String | 初始状态，固定为 UPCOMING |
| createdAt | String | 创建时间 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40000 | 请求参数错误 | 参数校验不通过 |
| 40001 | 参数不能为空 | 必填字段为空 |
| 40002 | 参数格式错误 | 时间格式错误、枚举值非法 |
| 40300 | 无操作权限 | STUDENT角色调用 |

#### 错误响应示例

```json
{
  "code": 40002,
  "message": "报名截止时间必须晚于报名开始时间",
  "data": null
}
```

#### 业务规则

```
1. 只有 ADMIN 和 TEACHER 可以发布竞赛
2. signupEnd 必须晚于 signupStart
3. competitionStart 填写时必须晚于 signupEnd
4. competitionEnd 填写时必须晚于 competitionStart
5. type 为 TEAM 时，minTeamSize 和 maxTeamSize 必填
6. type 为 TEAM 时，minTeamSize >= 2
7. type 为 TEAM 时，maxTeamSize >= minTeamSize
8. hasQuota 为 true 时，maxQuota 必填且 >= 1
9. 发布后初始状态为 UPCOMING
10. created_by 由后端从当前登录用户自动填入，前端不传
11. 发布成功后，若 hasQuota 为 true，初始化 Redis 计数器：
    SET competition:quota:{competitionId} {maxQuota}
```

---

### 4. 编辑竞赛

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | PUT |
| 请求路径 | /api/v1/competitions/{id} |
| 权限要求 | 管理员（ADMIN）或发布人 |
| Content-Type | application/json |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 竞赛ID |

#### 请求体

```json
{
  "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛（更新）",
  "organizer": "工业和信息化部人才交流中心",
  "requirement": "全国在校大学生",
  "signupStart": "2026-03-01T00:00:00+08:00",
  "signupEnd": "2026-05-15T23:59:59+08:00",
  "competitionStart": "2026-06-01T00:00:00+08:00",
  "competitionEnd": "2026-06-02T23:59:59+08:00",
  "hasQuota": true,
  "maxQuota": 150,
  "minTeamSize": null,
  "maxTeamSize": null,
  "maxTeachQuota": 5,
  "description": "更新后的竞赛详情",
  "attachmentUrl": "/uploads/attachments/2026/03/notice_v2.pdf"
}
```

#### 请求字段说明

**全量更新，所有字段均需传入，后端根据当前竞赛状态决定哪些字段生效**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 竞赛名称 |
| organizer | String | 是 | 主办方 |
| requirement | String | 否 | 参赛要求 |
| signupStart | String | 是 | 报名开始时间 |
| signupEnd | String | 是 | 报名截止时间 |
| competitionStart | String | 否 | 比赛开始时间 |
| competitionEnd | String | 否 | 比赛结束时间 |
| hasQuota | Boolean | 是 | 是否有名额限制 |
| maxQuota | Integer | 条件必填 | hasQuota为true时必填 |
| minTeamSize | Integer | 条件必填 | type为TEAM时必填 |
| maxTeamSize | Integer | 条件必填 | type为TEAM时必填 |
| maxTeachQuota | Integer | 否 | 每位老师最多带队数 |
| description | String | 否 | 竞赛详情 |
| attachmentUrl | String | 否 | 附件地址 |

**注意：`type` 字段不在请求体中，任何状态下均不允许修改竞赛类型**

#### 各状态允许修改的字段

| 字段 | UPCOMING | SIGNING | CLOSED/ONGOING | FINISHED | OFFLINE |
|------|---------|---------|---------------|---------|---------|
| title | ✅ | ✅ | ✅ | ❌ | ✅ |
| organizer | ✅ | ✅ | ✅ | ❌ | ✅ |
| requirement | ✅ | ✅ | ✅ | ❌ | ✅ |
| signupStart | ✅ | ❌ | ❌ | ❌ | ✅ |
| signupEnd | ✅ | ✅ | ❌ | ❌ | ✅ |
| competitionStart | ✅ | ✅ | ✅ | ❌ | ✅ |
| competitionEnd | ✅ | ✅ | ✅ | ❌ | ✅ |
| hasQuota | ✅ | ❌ | ❌ | ❌ | ✅ |
| maxQuota | ✅ | ✅（只能增大）| ❌ | ❌ | ✅ |
| minTeamSize | ✅ | ❌ | ❌ | ❌ | ✅ |
| maxTeamSize | ✅ | ✅ | ❌ | ❌ | ✅ |
| maxTeachQuota | ✅ | ✅ | ✅ | ❌ | ✅ |
| description | ✅ | ✅ | ✅ | ❌ | ✅ |
| attachmentUrl | ✅ | ✅ | ✅ | ❌ | ✅ |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "competitionId": 1,
    "title": "第十五届蓝桥杯全国软件和信息技术专业人才大赛（更新）",
    "status": "SIGNING",
    "updatedAt": "2026-03-15T14:00:00+08:00"
  }
}
```

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40000 | 请求参数错误 | 参数校验不通过 |
| 40120 | 竞赛不存在 | id无效 |
| 40123 | 无权限操作该竞赛 | 既不是管理员也不是发布人 |
| 40132 | 当前状态不允许该操作 | FINISHED状态下尝试编辑 |
| 40900 | 资源冲突 | SIGNING状态下maxQuota小于当前enrolled_count |

#### 错误响应示例

```json
{
  "code": 40900,
  "message": "名额上限不能小于当前已报名人数（62人）",
  "data": null
}
```

#### 业务规则

```
1. 只有管理员或发布人可以编辑
2. FINISHED 状态下不允许任何修改，返回 40132
3. 根据当前状态，后端忽略不允许修改的字段（即使前端传了也不生效）
4. SIGNING 状态下修改 maxQuota：
   新值必须 >= 当前 enrolled_count，否则返回 40900
   修改成功后同步更新 Redis 计数器：
   SET competition:quota:{competitionId} {newMaxQuota - enrolledCount}
5. 使用乐观锁防止并发编辑冲突：
   UPDATE ... WHERE id = ? AND version = ?
   影响行数为0时返回 40900，提示"数据已被他人修改，请刷新后重试"
6. 更新成功后 version + 1
```

---

### 5. 变更竞赛状态

#### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | PATCH |
| 请求路径 | /api/v1/competitions/{id}/status |
| 权限要求 | 管理员（ADMIN）或发布人 |
| Content-Type | application/json |

#### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 竞赛ID |

#### 请求体

```json
{
  "action": "OFFLINE"
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 可选值 | 说明 |
|------|------|------|-------|------|
| action | String | 是 | OFFLINE / RESTORE | OFFLINE：下架；RESTORE：恢复上架 |

#### 响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "competitionId": 1,
    "previousStatus": "SIGNING",
    "currentStatus": "OFFLINE",
    "updatedAt": "2026-03-20T10:00:00+08:00"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| competitionId | Long | 竞赛ID |
| previousStatus | String | 变更前的状态 |
| currentStatus | String | 变更后的状态 |
| updatedAt | String | 变更时间 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|--------|------|---------|
| 40120 | 竞赛不存在 | id无效 |
| 40123 | 无权限操作该竞赛 | 既不是管理员也不是发布人 |
| 40132 | 当前状态不允许该操作 | 重复下架或竞赛已结束 |
| 40000 | 请求参数错误 | action值非法 |

#### 错误响应示例

```json
{
  "code": 40132,
  "message": "竞赛已结束，不支持下架操作",
  "data": null
}
```

#### 业务规则

```
action = OFFLINE（下架）：
  1. 当前状态为 FINISHED 时，不允许下架，返回 40132
  2. 当前状态已经是 OFFLINE 时，不允许重复下架，返回 40132
  3. 下架成功后，将当前状态记录到扩展字段备用（用于 RESTORE 时恢复）
  4. 下架后学生不可见，不可新增报名
  5. 已存在的报名记录不受影响

action = RESTORE（恢复上架）：
  1. 当前状态不是 OFFLINE 时，返回 40132
  2. 恢复时根据当前时间重新计算正确状态：
     当前时间 < signupStart              → UPCOMING
     signupStart <= 当前时间 < signupEnd → SIGNING
     signupEnd <= 当前时间，
       且 competitionStart 不为空
       且 当前时间 < competitionStart    → CLOSED
     competitionStart <= 当前时间，
       且 competitionEnd 不为空
       且 当前时间 < competitionEnd      → ONGOING
     其他情况                            → FINISHED
```

---

## Redis 缓存设计

```
竞赛名额计数器：
  Key:    competition:quota:{competitionId}
  Value:  剩余名额数量（Integer）
  初始化：发布竞赛时，hasQuota=true 则写入 maxQuota
  操作：
    报名时 DECR，值<0时 INCR 回滚
    取消报名时 INCR
    修改 maxQuota 时重新 SET

竞赛列表缓存（可选，后期性能优化）：
  Key:    competition:list:{status}:{type}:{page}:{size}
  TTL:    60秒
  说明：发布/编辑/状态变更时主动清除对应缓存
```