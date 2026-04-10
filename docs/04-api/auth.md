# 认证模块接口文档

> 模块：auth
> 前缀：/api/v1/auth
> 说明：注册和登录接口，无需携带Token

> Knife4j在线文档：http://localhost:8080/doc.html
> 本文档与Knife4j保持同步，以Knife4j为准

---
## 接口列表

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/v1/auth/register | 用户注册 | 无需登录 |
| POST | /api/v1/auth/login | 用户登录 | 无需登录 |

---

## 1. 用户注册

### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/auth/register |
| 权限要求 | 无需登录 |
| Content-Type | application/json |

### 请求体

```json
{
  "username": "zhangsan",
  "password": "Zhang123456",
  "realName": "张三",
  "role": "STUDENT",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "studentNo": "2021010001",
  "department": "计算机学院",
  "title": null
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名，4-64位，只能包含字母、数字、下划线 |
| password | String | 是 | 密码，8-32位，必须包含字母和数字 |
| realName | String | 是 | 真实姓名，1-32位 |
| role | String | 是 | 角色，枚举值：STUDENT / TEACHER，注册不能选ADMIN |
| phone | String | 否 | 手机号，11位数字 |
| email | String | 否 | 邮箱地址 |
| studentNo | String | 条件必填 | 学号，role为STUDENT时必填，全局唯一 |
| department | String | 否 | 院系，如计算机学院 |
| title | String | 条件必填 | 职称，role为TEACHER时必填，如讲师/副教授/教授 |

### 响应体

#### 成功

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "zhangsan",
    "realName": "张三",
    "role": "STUDENT"
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| userId | Long | 用户ID |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| role | String | 角色 |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|---|---|---|
| 40000 | 请求参数错误 | 参数校验不通过 |
| 40001 | 参数不能为空 | 必填字段为空 |
| 40002 | 参数格式错误 | 用户名/密码/手机号格式不符合要求 |
| 40103 | 用户名已存在 | username 已被注册 |
| 40104 | 学号已被注册 | studentNo 已被注册 |

#### 错误响应示例

```json
{
  "code": 40103,
  "message": "用户名已存在",
  "data": null
}
```

#### 业务规则

- role 只允许 STUDENT 或 TEACHER，不允许注册 ADMIN
- role 为 STUDENT 时，studentNo 必填
- role 为 TEACHER 时，title 必填
- username 全局唯一，重复则返回 40103
- studentNo 全局唯一，重复则返回 40104
- 密码存储前使用 BCrypt 加密，不明文存储

---

## 2. 用户登录

### 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | POST |
| 请求路径 | /api/v1/auth/login |
| 权限要求 | 无需登录 |
| Content-Type | application/json |

### 请求体

```json
{
  "username": "zhangsan",
  "password": "Zhang123456"
}
```

#### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码，明文传输（依赖HTTPS加密传输） |

### 响应体

#### 成功

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ6aGFuZ3NhbiIsInVzZXJJZCI6MSwicm9sZSI6IlNUVURFTlQiLCJpYXQiOjE3MTk4MDAwMDAsImV4cCI6MTcxOTg4NjQwMH0.xxx",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userInfo": {
      "userId": 1,
      "username": "zhangsan",
      "realName": "张三",
      "role": "STUDENT",
      "department": "计算机学院",
      "avatarUrl": null
    }
  }
}
```

#### 响应字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| token | String | JWT Token，后续请求放入Header |
| tokenType | String | 固定值 Bearer |
| expiresIn | Integer | Token有效期，单位秒，固定86400（24小时） |
| userInfo.userId | Long | 用户ID |
| userInfo.username | String | 用户名 |
| userInfo.realName | String | 真实姓名 |
| userInfo.role | String | 角色：STUDENT / TEACHER / ADMIN |
| userInfo.department | String | 院系，可为null |
| userInfo.avatarUrl | String | 头像URL，可为null |

#### 错误码

| 错误码 | 说明 | 触发场景 |
|---|---|---|
| 40000 | 请求参数错误 | 用户名或密码为空 |
| 40101 | 用户名或密码错误 | 用户不存在或密码不匹配 |
| 40102 | 账号已被禁用 | 用户status为DISABLED |

#### 错误响应示例

```json
{
  "code": 40101,
  "message": "用户名或密码错误",
  "data": null
}
```

#### 业务规则

- 用户名或密码任意一个错误，统一返回 40101，不区分具体原因（防止用户名枚举攻击）
- 账号被禁用时返回 40102
- 登录成功后返回 JWT Token
- Token 有效期 24 小时，过期后需重新登录
- Token 携带信息：userId、username、role
- 前端后续请求在 Header 中携带：Authorization: Bearer {token}

---

## JWT 说明

### Token 结构

```text
Header:  { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "zhangsan", "userId": 1, "role": "STUDENT", "iat": 时间戳, "exp": 时间戳 }
```

### Token 使用方式

```text
请求头：
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

Token过期响应：
{
  "code": 40100,
  "message": "未登录或Token已过期",
  "data": null
}

Token无效响应：
{
  "code": 40101,
  "message": "Token无效",
  "data": null
}
```

---

## Spring Security 放行规则

| 路径 | 是否放行 |
|---|---|
| POST /api/v1/auth/register | ✅ 放行 |
| POST /api/v1/auth/login | ✅ 放行 |
| 其他所有接口 | ❌ 需要Token |