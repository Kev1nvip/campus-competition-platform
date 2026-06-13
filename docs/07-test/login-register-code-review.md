# 登录注册功能代码审查报告

**审查日期：** 2026-06-13  
**审查范围：** 登录 / 注册全链路（前端 → Nginx → 后端 Spring Boot → PostgreSQL）  
**结论：** 共发现 **7 个必须修复的 Bug**，其中 3 个是导致功能完全失败的根本原因。

---

## 一、Bug 列表（按严重度排序）

### [P0] Bug 1 — 前端 API 路径缺少 `/api` 前缀段，导致所有请求 404

**文件：** `frontend/src/api/auth.ts`  
**表现：** 登录和注册请求发向 `http://localhost:8080/api/v1/auth/login`，后端映射路径是 `/api/v1/auth/login`，看似匹配，但在 **Docker 环境**下请求走的是 Nginx 反向代理：

```
前端 baseURL = 'http://localhost:8080/api'   ← 直连后端端口
Nginx proxy_pass 规则： /api/  →  http://backend:8080
```

**直接在浏览器访问（开发模式/Docker 均不配置代理）时：**  
`request.ts` 的 `baseURL` 是 `http://localhost:8080/api`，所以请求路径是：

```
http://localhost:8080/api/v1/auth/login
```

而后端 `AuthController` 注册路径是 `/api/v1/auth/login`，这在开发模式下理论上能通，但**Vite 开发服务器没有配置代理**（`vite.config.ts` 中无 `proxy` 字段），前端跑在 `localhost:5173`，后端跑在 `localhost:8080`，会触发 **CORS 错误**。

**CORS 根本原因：** `SecurityConfig.java` 第 55 行：

```java
.cors(AbstractHttpConfigurer::disable)  // ← 直接关闭了 CORS 支持！
```

关闭 CORS 意味着不会发送任何 `Access-Control-Allow-*` 响应头，浏览器的预检请求（OPTIONS）或正式请求会被直接拦截，返回的响应没有跨域头，浏览器报错。

**修复方案：**

1. 在 `SecurityConfig.java` 中启用并配置 CORS，允许 `http://localhost:5173`（开发）和生产域名：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:80"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return new UrlBasedCorsConfigurationSource(){{addMapping("/api/**", config);}};
}

// SecurityFilterChain 中改为：
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

2. 或者在 `vite.config.ts` 中配置开发代理（更简单的开发期方案）：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

同时把 `request.ts` 的 `baseURL` 改为 `/api`（相对路径），以便同时兼容开发代理和 Nginx 生产环境。

---

### [P0] Bug 2 — ErrorCode 常量值冲突，导致错误码逻辑混乱

**文件：** `backend/src/main/java/com/competition/backend/common/constant/ErrorCode.java`  
**具体冲突：**

```java
public static final int UNAUTHORIZED      = 40100;
public static final int TOKEN_INVALID     = 40101;  // ← 与下方重复！
// ...
public static final int USER_PASSWORD_ERROR = 40101; // ← 同值！
```

`TOKEN_INVALID` 和 `USER_PASSWORD_ERROR` 均为 `40101`。前端如果根据 code 值判断错误类型（如是否跳转登录页），会出现逻辑混乱：密码错误可能被误判为 Token 失效，触发意外的登出跳转。

**修复方案：** 给 `USER_PASSWORD_ERROR` 一个独立值，例如 `40110`，并同步更新前端对 code 的判断逻辑。

---

### [P0] Bug 3 — 教师端和管理员端登录完全绕过后端，硬编码 mock 账号

**文件：** `frontend/src/views/teacher/Login.vue`、`frontend/src/views/admin/Login.vue`

两个后台登录页面都内置了硬编码测试账号并优先匹配，完全不走后端：

```js
// teacher/Login.vue
const TEST_TEACHER = { tid: "111111", pwd: "111111", ... }
if (form.value.tid === TEST_TEACHER.tid && form.value.pwd === TEST_TEACHER.pwd) {
    teacherStore.setUser(TEST_TEACHER.mockData)  // 直接设置状态，不发请求
    return
}

// admin/Login.vue
const TEST_ADMIN = { account: "admin", pwd: "111111", ... }
```

此外，这两个页面的真实登录分支调用的后端接口路径 `/teacher/login`、`/admin/login` 在后端根本不存在——后端只有 `/api/v1/auth/login`。即使 mock 被绕过，真实请求也必然 404。

**修复方案：** 删除所有硬编码 mock 分支，将教师/管理员登录统一复用 `/api/v1/auth/login` 接口，通过返回的 `role` 字段区分身份后分别存储到各自的 store。

---

### [P1] Bug 4 — 教师/管理员 API 使用独立 axios 实例，无 Token，且 baseURL 不含版本路径

**文件：** `frontend/src/api/teacher.ts`、`frontend/src/api/admin.ts`

```js
const service = axios.create({
  baseURL: '/api',  // ← 没有版本号 /v1，且是独立实例
  timeout: 6000
})
```

这两个文件创建了独立的 axios 实例，**没有接入 `request.ts` 中的请求拦截器**，因此：
- 不会自动附带 `Authorization: Bearer <token>` 请求头
- 所有需要认证的接口都会返回 401
- 路径格式与后端不一致（缺少 `/v1/`）

**修复方案：** 删除这两个文件中独立的 axios 实例，统一改为引入并使用 `@/utils/request`。

---

### [P1] Bug 5 — 教师/管理员响应码判断错误

**文件：** `frontend/src/views/teacher/Login.vue`、`frontend/src/views/admin/Login.vue`

```js
if (res.code === 200) {  // ← 错误！
```

后端统一响应格式（`Result.java`）中成功码是 `0`，不是 `200`。  
这意味着即使后端成功返回数据，前端也会走 `else` 分支报错。

**修复方案：** 将所有 `res.code === 200` 改为 `res.code === 0`，与学生端 `Login.vue` 保持一致。

---

### [P2] Bug 6 — Register.vue 不支持 TEACHER 角色的 title 字段填写，但后端强制要求

**文件：** `frontend/src/views/Register.vue`

注册表单中没有 `title`（职称）输入框，但后端 `AuthServiceImpl.java:45` 明确校验：

```java
} else if ("TEACHER".equals(registerDTO.getRole())) {
    if (!StringUtils.hasText(registerDTO.getTitle())) {
        throw new BusinessException(ErrorCode.PARAM_NULL, "职称不能为空");
    }
}
```

用户选择 TEACHER 角色时，`title` 字段始终为空，后端必然抛出异常，教师注册**100% 失败**。

**修复方案：** 在注册表单中添加职称输入框，当 `role === 'TEACHER'` 时显示并设为必填：

```html
<div class="form-item" v-if="form.role === 'TEACHER'">
  <label for="title">职称 *</label>
  <input id="title" v-model="form.title" placeholder="如：讲师/副教授/教授" />
</div>
```

同时在 `validateForm()` 中补充对应校验。

---

### [P2] Bug 7 — JWT 密钥长度不足，运行时可能抛出异常

**文件：** `backend/src/main/resources/application.yml`

```yaml
jwt:
  secret: ${JWT_SECRET:campus-competition-jwt-secret-key-2026}
```

默认密钥 `campus-competition-jwt-secret-key-2026` 为 38 个字符 = 304 bits。  
JJWT 使用 `Keys.hmacShaKeyFor()` 时，对于 HS256 要求最少 256 bits（32 字节）。虽然 304 bits 理论上满足 HS256，但 `docker-compose.yml` 中生产环境设置的：

```yaml
JWT_SECRET: campus-competition-jwt-secret-key-2024
```

同样是 38 字符，而部分 JJWT 版本对弱密钥会抛出 `WeakKeyException`，导致 Token 生成失败，登录接口抛出 500。

**修复方案：** 将密钥统一替换为 64 字符以上的随机字符串（建议 Base64 编码的 512-bit 随机数），并通过环境变量注入，不要硬编码到配置文件中。

---

## 二、问题汇总表

| # | 严重度 | 位置 | 问题描述 | 影响 |
|---|--------|------|----------|------|
| 1 | **P0** | `SecurityConfig.java:55` + `vite.config.ts` | CORS 被完全禁用，开发模式跨域请求全部失败 | 学生端登录/注册完全不可用 |
| 2 | **P0** | `ErrorCode.java:23-24` | `TOKEN_INVALID` 和 `USER_PASSWORD_ERROR` 值均为 40101 | 错误码混乱，前端逻辑误判 |
| 3 | **P0** | `teacher/Login.vue`、`admin/Login.vue` | 硬编码 mock 账号绕过后端；真实分支接口路径不存在 | 教师/管理员登录完全不对接后端 |
| 4 | **P1** | `api/teacher.ts`、`api/admin.ts` | 独立 axios 实例无 Token 拦截器，baseURL 路径错误 | 所有需认证的教师/管理员接口返回 401 |
| 5 | **P1** | `teacher/Login.vue`、`admin/Login.vue` | 响应码判断 `=== 200`，后端成功码为 `0` | 登录永远被判为失败 |
| 6 | **P2** | `Register.vue` + `AuthServiceImpl.java` | 注册表单缺少职称字段，教师注册必然失败 | 教师角色无法注册 |
| 7 | **P2** | `application.yml`、`docker-compose.yml` | JWT 密钥强度不足，部分 JJWT 版本会抛异常 | 登录时 Token 生成失败返回 500 |

---

## 三、修复优先级路线图

### 阶段一：让学生端登录/注册先跑通（修复 Bug 1、2）

1. `SecurityConfig.java`：将 `.cors(AbstractHttpConfigurer::disable)` 替换为正确的 CORS 配置，允许开发域名 `http://localhost:5173`
2. `vite.config.ts`：添加 `server.proxy` 代理配置，`request.ts` 中 `baseURL` 改为 `/api`（去掉端口）
3. `ErrorCode.java`：修复 `USER_PASSWORD_ERROR` 的值，避免与 `TOKEN_INVALID` 冲突

### 阶段二：修复教师/管理员登录（修复 Bug 3、4、5）

4. 删除 `teacher/Login.vue` 和 `admin/Login.vue` 中的 mock 硬编码分支
5. 将两者统一接入 `/api/v1/auth/login`，根据 `role` 字段存储到各自 store
6. 删除 `api/teacher.ts`、`api/admin.ts` 中独立的 axios 实例，改用统一的 `request.ts`
7. 将响应码判断从 `=== 200` 改为 `=== 0`

### 阶段三：业务完整性修复（修复 Bug 6、7）

8. `Register.vue`：增加职称字段（TEACHER 角色时显示），补充前端校验
9. 更新 JWT 密钥为更长的随机字符串，通过 `.env` 文件或 Docker Secret 管理

---

## 四、架构层面的补充观察

以下问题不会导致当前登录失败，但在后续联调中会引发问题，建议一并处理：

**路由守卫存在逻辑缺陷（`router/index.ts:98、115`）：**

```js
if (adminStore.id && !teacherStore.id) {  // ← 永假：teacherStore.id 非空才进入这个块
    return next('/teacher/competition')
}
```

第 98 行和 115 行的反向身份隔离条件写反了，逻辑永远不触发。这不影响登录，但修复后会影响路由鉴权。

**Nginx 反向代理路径（`nginx.conf:14`）：**

```nginx
location /api/ {
    proxy_pass http://backend:8080;  # ← 注意：不带尾斜杠 + 带尾斜杠的 location
```

当 `location` 为 `/api/` 而 `proxy_pass` 不带路径时，Nginx 会保留 `/api/` 前缀转发给后端。后端 `@RequestMapping("/api/v1/...")` 包含了 `/api` 前缀，所以路径是匹配的，这一段没有问题。

**前端 `request.ts` 响应拦截器丢失了错误 HTTP 状态码：**

```js
(error: Error) => {
    return Promise.reject(error)  // ← 没有统一处理 401/403，不会自动清除 token 并跳转登录
}
```

后续应在这里补充 401 时清除 localStorage token 并跳转 `/login` 的逻辑。

---

*报告结束。建议按阶段一 → 二 → 三顺序逐步修复并在每阶段结束后验证功能。*
