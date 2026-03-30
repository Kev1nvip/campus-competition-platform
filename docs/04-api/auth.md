# 认证模块接口

> Knife4j在线文档：http://localhost:8080/doc.html
> 本文档与Knife4j保持同步，以Knife4j为准

---

## POST /api/v1/auth/register 用户注册

**权限**：无需登录

**请求体**
```json
{
  "username": "zhangsan",
  "password": "123456",
  "realName": "张三",
  "role": "student",
  "phone": "13800138000",
  "studentNo": "2021001",
  "department": "计算机学院"
}
```

**字段说明**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名，4-20位 |
| password | String | 是 | 密码，6-20位 |
| realName | String | 是 | 真实姓名 |
| role | String | 是 | student/teacher |
| phone | String | 否 | 手机号 |
| studentNo | String | 否 | 学号，student角色必填 |
| department | String | 否 | 院系 |

**成功响应**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "realName": "张三",
    "role": "student"
  }
}
```

**失败响应**
```json
{
  "code": 40001,
  "message": "用户名已存在",
  "data": null
}
```

**错误码**
| code | 说明 |
|------|------|
| 40001 | 用户名已存在 |
| 40002 | 参数格式错误 |

---

## POST /api/v1/auth/login 用户登录

**权限**：无需登录

**请求体**
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**成功响应**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "zhangsan",
      "realName": "张三",
      "role": "student"
    }
  }
}
```

**错误码**
| code | 说明 |
|------|------|
| 40101 | 用户名或密码错误 |
| 40102 | 账号已被禁用 |