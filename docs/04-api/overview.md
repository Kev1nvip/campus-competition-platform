# 接口总览

> 版本：v1.0
> 创建日期：2026-04
> 维护人：后端开发成员
> 在线文档：http://localhost:8080/doc.html（后端启动后访问）

---

## 一、基本规范

### 1.1 Base URL

| 环境 | 地址 |
|------|------|
| 本地开发 | http://localhost:8080/api/v1 |
| Docker部署 | http://localhost/api/v1 |

### 1.2 请求规范

```
请求格式：
  Content-Type: application/json

认证方式：
  除登录和注册接口外，所有接口必须携带Token
  请求头：Authorization: Bearer {token}

字符编码：
  统一 UTF-8
```

### 1.3 HTTP方法语义

| 方法 | 语义 | 示例 |
|------|------|------|
| GET | 查询资源 | 获取竞赛列表 |
| POST | 创建资源 | 发布竞赛、提交报名 |
| PUT | 全量更新 | 更新个人信息 |
| PATCH | 部分更新 | 变更竞赛状态 |
| DELETE | 删除资源 | 取消报名 |

---

## 二、统一返回体

### 2.1 格式定义

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 2.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 0表示成功，非0表示失败 |
| message | String | 成功时为success，失败时为错误描述 |
| data | Object/Array/null | 业务数据，失败时为null |

### 2.3 分页返回格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 10,
    "totalPages": 10
  }
}
```

### 2.4 Java代码实现

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    // 成功（有数据）
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .build();
    }

    // 成功（无数据）
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(0)
                .message("success")
                .data(null)
                .build();
    }

    // 失败
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
```

```java
@Data
@Builder
public class PageVO<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;

    public static <T> PageVO<T> of(Page<T> page) {
        return PageVO.<T>builder()
                .list(page.getContent())
                .total(page.getTotalElements())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

    // 需要转换VO时使用
    public static <T, R> PageVO<R> of(Page<T> page, Function<T, R> converter) {
        return PageVO.<R>builder()
                .list(page.getContent().stream()
                        .map(converter)
                        .collect(Collectors.toList()))
                .total(page.getTotalElements())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }
}
```

---

## 三、错误码规范

### 3.1 错误码格式

```
5位数字

前两位：错误类别
  40 → 客户端错误（参数/权限/资源）
  50 → 服务端错误

后三位：具体错误序号
  000 → 通用错误
  001起 → 具体业务错误
```

### 3.2 错误码表

**通用错误**

| 错误码 | HTTP状态码 | 说明 |
|--------|-----------|------|
| 40000 | 400 | 请求参数错误 |
| 40001 | 400 | 参数不能为空 |
| 40002 | 400 | 参数格式错误 |
| 40100 | 401 | 未登录或Token已过期 |
| 40101 | 401 | Token无效 |
| 40300 | 403 | 无操作权限 |
| 40400 | 404 | 资源不存在 |
| 40900 | 409 | 资源冲突（重复操作）|
| 50000 | 500 | 服务器内部错误 |

**用户模块（101xx）**

| 错误码 | 说明 |
|--------|------|
| 40101 | 用户名或密码错误 |
| 40102 | 账号已被禁用 |
| 40103 | 用户名已存在 |
| 40104 | 学号已被注册 |
| 40105 | 用户不存在 |

**竞赛模块（102xx）**

| 错误码 | 说明 |
|--------|------|
| 40120 | 竞赛不存在 |
| 40121 | 竞赛不在报名时间内 |
| 40122 | 竞赛名额已满 |
| 40123 | 无权限操作该竞赛 |

**报名模块（103xx）**

| 错误码 | 说明 |
|--------|------|
| 40130 | 已报名该竞赛，不能重复报名 |
| 40131 | 报名记录不存在 |
| 40132 | 当前状态不允许该操作 |
| 40133 | 老师带队名额已满 |

**队伍模块（104xx）**

| 错误码 | 说明 |
|--------|------|
| 40140 | 队伍不存在 |
| 40141 | 已加入该竞赛的队伍 |
| 40142 | 队伍人数已满 |
| 40143 | 无队长权限 |
| 40144 | 队伍已提交审核，不能修改 |
| 40145 | 老师尚未确认带队，不能发布招募帖 |

**申请模块（105xx）**

| 错误码 | 说明 |
|--------|------|
| 40150 | 申请不存在 |
| 40151 | 已有待处理的申请，请勿重复申请 |
| 40152 | 申请已处理，不能重复操作 |

**审核模块（106xx）**

| 错误码 | 说明 |
|--------|------|
| 40160 | 审核记录不存在 |
| 40161 | 当前状态无需审核 |

**获奖模块（107xx）**

| 错误码 | 说明 |
|--------|------|
| 40170 | 获奖记录不存在 |
| 40171 | 该报名已有获奖记录 |
| 40172 | 报名未生效，不能提交获奖记录 |

### 3.3 Java错误码常量类

```java
public class ErrorCode {

    // 通用
    public static final int PARAM_ERROR = 40000;
    public static final int PARAM_NULL = 40001;
    public static final int PARAM_FORMAT = 40002;
    public static final int UNAUTHORIZED = 40100;
    public static final int TOKEN_INVALID = 40101;
    public static final int FORBIDDEN = 40300;
    public static final int NOT_FOUND = 40400;
    public static final int CONFLICT = 40900;
    public static final int SERVER_ERROR = 50000;

    // 用户
    public static final int USER_PASSWORD_ERROR = 40101;
    public static final int USER_DISABLED = 40102;
    public static final int USER_NAME_EXISTS = 40103;
    public static final int STUDENT_NO_EXISTS = 40104;
    public static final int USER_NOT_FOUND = 40105;

    // 竞赛
    public static final int COMPETITION_NOT_FOUND = 40120;
    public static final int COMPETITION_NOT_SIGNING = 40121;
    public static final int COMPETITION_QUOTA_FULL = 40122;
    public static final int COMPETITION_NO_PERMISSION = 40123;

    // 报名
    public static final int SIGNUP_DUPLICATE = 40130;
    public static final int SIGNUP_NOT_FOUND = 40131;
    public static final int SIGNUP_STATUS_ERROR = 40132;
    public static final int TEACHER_QUOTA_FULL = 40133;

    // 队伍
    public static final int TEAM_NOT_FOUND = 40140;
    public static final int TEAM_ALREADY_JOINED = 40141;
    public static final int TEAM_MEMBER_FULL = 40142;
    public static final int TEAM_NO_LEADER = 40143;
    public static final int TEAM_SUBMITTED = 40144;
    public static final int TEAM_TEACHER_NOT_CONFIRMED = 40145;

    // 申请
    public static final int APPLY_NOT_FOUND = 40150;
    public static final int APPLY_DUPLICATE = 40151;
    public static final int APPLY_ALREADY_HANDLED = 40152;

    // 审核
    public static final int AUDIT_NOT_FOUND = 40160;
    public static final int AUDIT_NOT_NEEDED = 40161;

    // 获奖
    public static final int AWARD_NOT_FOUND = 40170;
    public static final int AWARD_DUPLICATE = 40171;
    public static final int AWARD_SIGNUP_NOT_APPROVED = 40172;
}
```

---

## 四、接口清单

### 4.1 认证模块 `/api/v1/auth`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /auth/register | 用户注册 | 无需登录 |
| POST | /auth/login | 用户登录 | 无需登录 |

### 4.2 用户模块 `/api/v1/users`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /users/me | 获取当前用户信息 | 登录用户 |
| PUT | /users/me | 更新个人信息 | 登录用户 |
| PUT | /users/me/password | 修改密码 | 登录用户 |
| GET | /users/{userId}/profile | 查看用户主页 | 登录用户 |
| GET | /users | 用户列表 | 管理员 |
| PATCH | /users/{userId}/status | 启用/禁用用户 | 管理员 |

### 4.3 竞赛模块 `/api/v1/competitions`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /competitions | 竞赛列表 | 登录用户 |
| GET | /competitions/{id} | 竞赛详情 | 登录用户 |
| POST | /competitions | 发布竞赛 | 管理员/老师 |
| PUT | /competitions/{id} | 编辑竞赛 | 管理员/发布人 |
| PATCH | /competitions/{id}/status | 变更竞赛状态 | 管理员/发布人 |

### 4.4 报名模块 `/api/v1/signups`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /signups/individual | 个人赛报名 | 学生 |
| POST | /signups/individual/{id}/submit | 提交个人赛审核 | 学生 |
| GET | /signups/individual/my | 我的个人赛报名 | 学生 |
| GET | /signups/individual/{id} | 个人赛报名详情 | 登录用户 |
| POST | /signups/team | 团队赛报名（队长提交）| 学生 |
| POST | /signups/team/{id}/submit | 提交团队赛审核 | 队长 |
| GET | /signups/team/my | 我的团队赛报名 | 学生 |
| GET | /signups/team/{id} | 团队赛报名详情 | 登录用户 |

### 4.5 队伍模块 `/api/v1/teams`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /teams | 创建队伍 | 学生 |
| GET | /teams/{id} | 队伍详情 | 登录用户 |
| GET | /teams/my | 我参与的队伍 | 学生 |
| DELETE | /teams/{id}/members/me | 退出队伍 | 队员 |
| DELETE | /teams/{id}/members/{memberId} | 踢出成员 | 队长 |
| DELETE | /teams/{id} | 解散队伍 | 队长 |

### 4.6 招募模块 `/api/v1/recruitments`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /recruitments/teacher | 老师发布招募帖 | 老师 |
| GET | /recruitments/teacher | 老师招募帖列表 | 登录用户 |
| GET | /recruitments/teacher/{id} | 老师招募帖详情 | 登录用户 |
| PATCH | /recruitments/teacher/{id}/close | 关闭老师招募帖 | 发布老师 |
| POST | /recruitments/team | 队长发布组队招募帖 | 队长 |
| GET | /recruitments/team | 组队招募帖列表 | 登录用户 |
| GET | /recruitments/team/{id} | 组队招募帖详情 | 登录用户 |
| PATCH | /recruitments/team/{id}/close | 关闭组队招募帖 | 队长 |

### 4.7 申请模块 `/api/v1/applies`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /applies | 发起申请 | 学生/队长 |
| GET | /applies/sent | 我发起的申请 | 登录用户 |
| GET | /applies/received | 我收到的申请 | 登录用户 |
| PATCH | /applies/{id}/approve | 同意申请 | 接收人 |
| PATCH | /applies/{id}/reject | 拒绝申请 | 接收人 |
| DELETE | /applies/{id} | 撤回申请 | 发起人 |

### 4.8 审核模块 `/api/v1/audits`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /audits/pending | 待审核列表 | 管理员 |
| POST | /audits/signup/{id}/approve | 审核报名通过 | 管理员 |
| POST | /audits/signup/{id}/reject | 审核报名驳回 | 管理员 |
| GET | /audits/history | 审核历史 | 管理员 |

### 4.9 获奖模块 `/api/v1/awards`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /awards | 提交获奖记录 | 学生 |
| GET | /awards/my | 我的获奖记录 | 学生 |
| GET | /awards | 获奖记录列表 | 管理员 |
| GET | /awards/{id} | 获奖记录详情 | 登录用户 |
| POST | /awards/{id}/approve | 审核获奖通过 | 管理员 |
| POST | /awards/{id}/reject | 审核获奖驳回 | 管理员 |

### 4.10 通知模块 `/api/v1/notifications`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /notifications | 通知列表 | 登录用户 |
| GET | /notifications/unread/count | 未读通知数量 | 登录用户 |
| PATCH | /notifications/{id}/read | 标记单条已读 | 登录用户 |
| PATCH | /notifications/read/all | 全部标记已读 | 登录用户 |

### 4.11 统计模块 `/api/v1/statistics`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /statistics/overview | 平台总览数据 | 管理员 |
| GET | /statistics/competitions | 竞赛统计 | 管理员 |
| GET | /statistics/awards | 获奖统计 | 管理员 |

### 4.12 AI模块 `/api/v1/ai`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /ai/recommend | 竞赛智能推荐 | 学生 |
| POST | /ai/generate-intro | AI辅助生成参赛简介 | 学生 |

---

## 五、通用请求参数

### 5.1 分页参数

```
所有列表查询接口统一使用以下分页参数：

page  → 页码，从1开始，默认1
size  → 每页数量，默认10，最大20

示例：GET /api/v1/competitions?page=1&size=10
```

### 5.2 认证Header

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

Token获取：调用 POST /api/v1/auth/login 接口
Token有效期：24小时
Token过期：返回错误码 40100，前端跳转登录页
```

---

## 六、接口数量统计

| 模块 | 接口数量 |
|------|---------|
| 认证模块 | 2 |
| 用户模块 | 6 |
| 竞赛模块 | 5 |
| 报名模块 | 8 |
| 队伍模块 | 6 |
| 招募模块 | 8 |
| 申请模块 | 6 |
| 审核模块 | 4 |
| 获奖模块 | 6 |
| 通知模块 | 4 |
| 统计模块 | 3 |
| AI模块 | 2 |
| **合计** | **60** |

---

## 七、Knife4j 配置

```java
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园学术竞赛管理平台 API")
                        .description("提供竞赛管理、报名、组队、审核等功能接口")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("开发团队")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

```
访问地址：
  http://localhost:8080/doc.html

使用方式：
  1. 打开文档页面
  2. 右上角点击"Authorize"
  3. 输入登录后获取的Token
  4. 即可在文档页面直接调试接口
```