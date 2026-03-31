# 编码规范

> 适用范围：全体开发成员
> 最后更新：2026-03
> 维护人：负责人
> 
> 本规范靠自觉遵守，CR时互相检查。
> 不确定的地方看这份文档，文档没有的地方保持和已有代码风格一致。

---

## 一、通用规范

### 命名原则

```
核心原则：名字要能表达意图，看名字就知道是干什么的

✅ 好的命名
  getUserById
  competitionSignupCount
  isSignupExpired
  handleLoginSubmit

❌ 差的命名
  getData
  num
  flag
  temp
  a, b, c
```

### 注释原则

```
注释解释"为什么"，而不是"做什么"
代码本身应该能说明"做什么"

✅ 有价值的注释
  // 并发场景下先扣减Redis计数器，再写数据库，失败时回滚计数器
  // 乐观锁version字段防止并发超报

❌ 无价值的注释
  // 获取用户
  getUserById(id);
  
  // i加1
  i++;
```

### 魔法数字

```
❌ 直接使用数字
  if (status == 1) {}
  if (role == 3) {}

✅ 用常量或枚举代替
  if (status == CompetitionStatus.SIGNING) {}
  if (role == UserRole.ADMIN) {}
```

---

## 二、后端规范（Java + SpringBoot3）

### 2.1 包结构

```
com.competition.backend
├── common/                    # 公共模块
│   ├── config/                # 配置类
│   ├── constant/              # 常量
│   ├── exception/             # 异常定义
│   ├── result/                # 统一返回体
│   └── utils/                 # 工具类
├── module/                    # 业务模块
│   ├── user/                  # 用户模块
│   │   ├── controller/
│   │   ├── service/
│   │   │   └── impl/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/               # 入参对象
│   │   └── vo/                # 出参对象
│   ├── competition/           # 竞赛模块
│   ├── signup/                # 报名模块
│   ├── team/                  # 组队模块
│   ├── audit/                 # 审核模块
│   ├── award/                 # 获奖模块
│   └── notification/          # 通知模块
└── BackendApplication.java
```

### 2.2 命名规范

```java
// 类名：PascalCase，有意义的后缀
CompetitionController
CompetitionService
CompetitionServiceImpl
CompetitionRepository
CompetitionDTO        // 入参
CompetitionVO         // 出参
Competition           // 数据库实体

// 方法名：camelCase，动词开头
getCompetitionById()
createCompetition()
updateCompetitionStatus()
deleteCompetition()
isSignupExpired()
hasReachedQuota()

// 变量名：camelCase
Long competitionId
Integer enrolledCount
Boolean isExpired

// 常量：UPPER_SNAKE_CASE，定义在常量类中
public static final Integer MAX_TEAM_SIZE = 5;
public static final String CACHE_KEY_COMPETITION = "competition:";
public static final Long TOKEN_EXPIRE_TIME = 7200L;

// 枚举：类名PascalCase，成员UPPER_SNAKE_CASE
public enum CompetitionStatus {
    UPCOMING,   // 未开始
    SIGNING,    // 报名中
    CLOSED,     // 报名截止
    ONGOING,    // 进行中
    FINISHED    // 已结束
}

public enum UserRole {
    STUDENT,  // 学生
    TEACHER,  // 老师
    ADMIN     // 管理员
}
```

### 2.3 分层职责

```
Controller层：
  ✅ 接收请求参数
  ✅ 参数基础校验（@Valid）
  ✅ 调用Service
  ✅ 返回统一响应体
  ❌ 不写业务逻辑
  ❌ 不直接操作数据库

Service层：
  ✅ 业务逻辑处理
  ✅ 事务控制（@Transactional）
  ✅ 调用Repository
  ✅ 调用其他Service
  ❌ 不写SQL
  ❌ 不处理HTTP请求/响应

Repository层：
  ✅ 数据库操作
  ✅ 自定义SQL查询
  ❌ 不写业务逻辑
```

### 2.4 代码示例

```java
// ================================
// Controller 示例
// ================================
@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
@Slf4j
public class CompetitionController {

    private final CompetitionService competitionService;

    /**
     * 获取竞赛列表
     */
    @GetMapping
    public Result<PageVO<CompetitionVO>> listCompetitions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        PageVO<CompetitionVO> result = 
            competitionService.listCompetitions(page, size, status);
        return Result.success(result);
    }

    /**
     * 发布竞赛（管理员/老师）
     */
    @PostMapping
    public Result<CompetitionVO> createCompetition(
            @RequestBody @Valid CompetitionDTO dto) {
        CompetitionVO competition = competitionService.createCompetition(dto);
        return Result.success(competition);
    }
}


// ================================
// Service 示例
// ================================
@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageVO<CompetitionVO> listCompetitions(
            Integer page, Integer size, String status) {
        // 优先从缓存获取
        String cacheKey = CACHE_KEY_COMPETITION + "list:" + status + ":" + page;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("竞赛列表命中缓存，key={}", cacheKey);
            return (PageVO<CompetitionVO>) cached;
        }

        // 缓存未命中，查数据库
        Page<Competition> competitionPage = 
            competitionRepository.findByStatus(status, PageRequest.of(page - 1, size));

        PageVO<CompetitionVO> result = PageVO.of(
            competitionPage,
            competition -> convertToVO(competition)
        );

        // 写入缓存，TTL 5分钟
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionVO createCompetition(CompetitionDTO dto) {
        Competition competition = Competition.builder()
            .title(dto.getTitle())
            .organizer(dto.getOrganizer())
            .maxQuota(dto.getMaxQuota())
            .enrolledCount(0)
            .status(CompetitionStatus.UPCOMING)
            .signupStart(dto.getSignupStart())
            .signupEnd(dto.getSignupEnd())
            .version(0)
            .build();

        competitionRepository.save(competition);
        log.info("竞赛发布成功，competitionId={}, title={}", 
                 competition.getId(), competition.getTitle());

        return convertToVO(competition);
    }
}


// ================================
// Entity 示例（Lombok）
// ================================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "competition")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 128)
    private String organizer;

    /**
     * 名额上限，null表示不限制
     */
    @Column
    private Integer maxQuota;

    /**
     * 已报名人数
     */
    @Column(nullable = false)
    private Integer enrolledCount;

    /**
     * 竞赛状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CompetitionStatus status;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    @Column(nullable = false)
    private LocalDateTime signupStart;

    @Column(nullable = false)
    private LocalDateTime signupEnd;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}


// ================================
// DTO 示例（入参）
// ================================
@Data
public class CompetitionDTO {

    @NotBlank(message = "竞赛名称不能为空")
    @Length(max = 128, message = "竞赛名称不能超过128个字符")
    private String title;

    @NotBlank(message = "主办方不能为空")
    private String organizer;

    /**
     * 名额上限，不填表示不限制
     */
    @Min(value = 1, message = "名额上限至少为1")
    private Integer maxQuota;

    @NotNull(message = "报名开始时间不能为空")
    private LocalDateTime signupStart;

    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime signupEnd;
}


// ================================
// 统一返回体
// ================================
@Data
@Builder
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
            .code(0)
            .message("success")
            .data(data)
            .build();
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
            .code(code)
            .message(message)
            .data(null)
            .build();
    }
}


// ================================
// 全局异常处理
// ================================
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("参数错误");
        return Result.fail(40000, message);
    }

    /**
     * 兜底异常，未知错误
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return Result.fail(50000, "服务器内部错误，请稍后重试");
    }
}
```

### 2.5 注释规范

```java
// ================================
// 类注释（每个类必须有）
// ================================
/**
 * 竞赛服务实现类
 * 处理竞赛发布、报名、状态管理等核心业务
 */
@Service
public class CompetitionServiceImpl implements CompetitionService {}


// ================================
// 方法注释
// ================================
// public方法：必须写注释
// private方法：逻辑复杂时写注释，简单的不用写

/**
 * 竞赛报名（含并发控制）
 * 先扣减Redis计数器，再写数据库
 * 数据库写入失败时回滚Redis计数器
 *
 * @param competitionId 竞赛ID
 * @param studentId     学生ID
 * @return 报名记录
 */
public SignupVO signup(Long competitionId, Long studentId) {}


// ================================
// 行内注释（解释为什么，不解释做什么）
// ================================

// ✅ 有价值：解释业务原因
// 先扣Redis再写DB，失败时补偿Redis，保证不超报
redisTemplate.opsForValue().decrement(quotaKey);

// ✅ 有价值：解释特殊处理
// 乐观锁冲突说明有并发，直接返回失败而不是重试
// 重试可能导致用户等待时间过长
if (e instanceof OptimisticLockException) {
    throw new BusinessException(40001, "报名人数已满");
}

// ❌ 无价值：代码本身已经说明
// 保存竞赛
competitionRepository.save(competition);
```

### 2.6 异常处理规范

```java
// ================================
// 自定义业务异常
// ================================
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    // 预定义常用异常，统一管理错误码
    public static BusinessException competitionNotFound() {
        return new BusinessException(40400, "竞赛不存在");
    }

    public static BusinessException signupQuotaFull() {
        return new BusinessException(40001, "报名人数已满");
    }

    public static BusinessException noPermission() {
        return new BusinessException(40300, "无操作权限");
    }
}

// ================================
// 使用方式
// ================================

// ✅ 用业务异常，让全局异常处理器统一处理
Competition competition = competitionRepository.findById(id)
    .orElseThrow(BusinessException::competitionNotFound);

// ❌ 不要吞掉异常
try {
    competitionRepository.save(competition);
} catch (Exception e) {
    // 什么都不做，绝对禁止
}

// ❌ 不要随意打印堆栈
} catch (Exception e) {
    e.printStackTrace();  // 禁止
}

// ✅ 用log记录异常
} catch (Exception e) {
    log.error("保存竞赛失败，competitionId={}", competitionId, e);
    throw new BusinessException(50000, "操作失败，请稍后重试");
}
```

### 2.7 日志规范

```java
// 使用 @Slf4j 注解，不要手动创建Logger
@Slf4j
public class CompetitionServiceImpl {}

// 日志级别使用规范
log.debug("竞赛列表命中缓存，key={}", cacheKey);        // 调试信息
log.info("竞赛发布成功，competitionId={}", id);          // 关键业务操作
log.warn("竞赛名额不足，competitionId={}", id);          // 需要关注但不影响功能
log.error("竞赛保存失败，competitionId={}", id, e);      // 需要立即处理的错误

// 使用占位符，不要用字符串拼接
// ✅ 正确
log.info("用户登录成功，userId={}, role={}", userId, role);

// ❌ 错误（性能差）
log.info("用户登录成功，userId=" + userId + ", role=" + role);

// 敏感信息不记录日志
// ❌ 禁止记录密码
log.info("用户登录，username={}, password={}", username, password);

// ✅ 只记录必要信息
log.info("用户登录成功，username={}", username);
```

---

## 三、前端规范（Vue3 + TypeScript）

### 3.1 目录结构

```
frontend/src/
├── api/                   # 接口请求
│   ├── request.ts         # axios封装
│   ├── auth.ts
│   ├── competition.ts
│   └── index.ts           # 统一导出
├── assets/                # 静态资源
├── components/            # 公共组件
│   ├── common/            # 通用组件
│   └── business/          # 业务组件
├── composables/           # 组合式函数（hooks）
├── router/                # 路由配置
├── stores/                # 状态管理（Pinia）
├── types/                 # TypeScript类型定义
├── utils/                 # 工具函数
├── views/                 # 页面组件
│   ├── student/           # 学生端页面
│   ├── teacher/           # 老师端页面
│   └── admin/             # 管理员端页面
└── main.ts
```

### 3.2 命名规范

```
文件命名：
  页面组件：PascalCase        CompetitionList.vue
  普通组件：PascalCase        CompetitionCard.vue
  组合函数：camelCase         useCompetition.ts
  工具函数：camelCase         formatDate.ts
  API文件：camelCase          competition.ts
  类型文件：camelCase         competition.d.ts

变量命名：
  普通变量：camelCase         competitionList
  布尔变量：is/has/can前缀    isLoading, hasPermission, canSignup
  常量：    UPPER_SNAKE_CASE  MAX_PAGE_SIZE

组件命名：
  页面级组件：PascalCase + 模块前缀
    StudentCompetitionList.vue
    TeacherAuditPanel.vue
    AdminDashboard.vue

  公共组件：PascalCase
    CompetitionCard.vue
    StatusBadge.vue
    PageHeader.vue
```

### 3.3 组件规范

```vue
<!-- 
  组件：竞赛卡片
  说明：展示竞赛基本信息，支持点击查看详情
-->
<template>
  <div class="competition-card" @click="handleCardClick">
    <div class="competition-card__header">
      <h3 class="competition-card__title">{{ competition.title }}</h3>
      <StatusBadge :status="competition.status" />
    </div>

    <div class="competition-card__body">
      <p class="competition-card__organizer">{{ competition.organizer }}</p>
      <p class="competition-card__deadline">
        报名截止：{{ formatDate(competition.signupEnd) }}
      </p>
    </div>

    <div class="competition-card__footer">
      <!-- 有名额限制时展示剩余名额 -->
      <span v-if="competition.maxQuota" class="competition-card__quota">
        剩余名额：{{ remainingQuota }}
      </span>
      <el-button type="primary" size="small" @click.stop="handleSignup">
        立即报名
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { formatDate } from '@/utils/date'
import type { CompetitionVO } from '@/types/competition'

// ================================
// Props 定义
// ================================
interface Props {
  competition: CompetitionVO
}

const props = defineProps<Props>()

// ================================
// Emits 定义
// ================================
const emit = defineEmits<{
  signup: [competitionId: number]
}>()

// ================================
// 响应式数据
// ================================
const router = useRouter()

// ================================
// 计算属性
// ================================
// 剩余名额
const remainingQuota = computed(() => {
  if (!props.competition.maxQuota) return null
  return props.competition.maxQuota - props.competition.enrolledCount
})

// ================================
// 事件处理
// ================================
const handleCardClick = () => {
  router.push(`/competition/${props.competition.id}`)
}

const handleSignup = () => {
  emit('signup', props.competition.id)
}
</script>

<style scoped>
.competition-card {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.competition-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.competition-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
```

### 3.4 TypeScript 规范

```typescript
// ================================
// 类型定义文件 types/competition.d.ts
// ================================

// 竞赛状态枚举
export type CompetitionStatus =
  | 'UPCOMING'   // 未开始
  | 'SIGNING'    // 报名中
  | 'CLOSED'     // 报名截止
  | 'ONGOING'    // 进行中
  | 'FINISHED'   // 已结束

// 竞赛视图对象（接口返回）
export interface CompetitionVO {
  id: number
  title: string
  organizer: string
  status: CompetitionStatus
  maxQuota: number | null      // null表示不限制
  enrolledCount: number
  signupStart: string
  signupEnd: string
  competitionStart: string | null
  competitionEnd: string | null
  description: string | null
  attachmentUrl: string | null
}

// 竞赛创建入参
export interface CompetitionDTO {
  title: string
  organizer: string
  maxQuota?: number
  signupStart: string
  signupEnd: string
  competitionStart?: string
  competitionEnd?: string
  description?: string
}

// 分页返回
export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

// 统一返回体
export interface Result<T> {
  code: number
  message: string
  data: T
}
```

```typescript
// ================================
// API 封装 api/competition.ts
// ================================
import request from './request'
import type { CompetitionVO, CompetitionDTO, PageVO } from '@/types/competition'

export const competitionApi = {
  // 获取竞赛列表
  getList: (params: {
    page: number
    size: number
    status?: string
  }) => request.get<PageVO<CompetitionVO>>('/competitions', { params }),

  // 获取竞赛详情
  getById: (id: number) =>
    request.get<CompetitionVO>(`/competitions/${id}`),

  // 发布竞赛
  create: (data: CompetitionDTO) =>
    request.post<CompetitionVO>('/competitions', data),

  // 更新竞赛
  update: (id: number, data: Partial<CompetitionDTO>) =>
    request.put<CompetitionVO>(`/competitions/${id}`, data),

  // 变更竞赛状态
  updateStatus: (id: number, status: string) =>
    request.patch(`/competitions/${id}/status`, { status }),
}
```

```typescript
// ================================
// 组合式函数 composables/useCompetition.ts
// ================================
import { ref, reactive } from 'vue'
import { competitionApi } from '@/api/competition'
import { ElMessage } from 'element-plus'
import type { CompetitionVO, PageVO } from '@/types/competition'

export function useCompetitionList() {
  // 列表数据
  const list = ref<CompetitionVO[]>([])

  // 加载状态
  const isLoading = ref(false)

  // 分页参数
  const pagination = reactive({
    page: 1,
    size: 10,
    total: 0,
  })

  // 筛选参数
  const filters = reactive({
    status: '',
  })

  // 获取竞赛列表
  const fetchList = async () => {
    isLoading.value = true
    try {
      const data = await competitionApi.getList({
        page: pagination.page,
        size: pagination.size,
        status: filters.status || undefined,
      })
      list.value = data.list
      pagination.total = data.total
    } catch (error) {
      ElMessage.error('获取竞赛列表失败')
    } finally {
      isLoading.value = false
    }
  }

  // 切换页码
  const handlePageChange = (page: number) => {
    pagination.page = page
    fetchList()
  }

  return {
    list,
    isLoading,
    pagination,
    filters,
    fetchList,
    handlePageChange,
  }
}
```

### 3.5 ESLint + Prettier 配置

```javascript
// .eslintrc.cjs
module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:@typescript-eslint/recommended',
    'prettier',
  ],
  rules: {
    // 禁止使用 any（尽量避免）
    '@typescript-eslint/no-explicit-any': 'warn',
    // 未使用变量报错
    '@typescript-eslint/no-unused-vars': 'error',
    // 组件名必须多个单词（避免和HTML标签冲突）
    'vue/multi-word-component-names': 'off',
    // console 只在开发时允许
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
  },
}
```

```json
// .prettierrc
{
  "semi": false,
  "singleQuote": true,
  "printWidth": 80,
  "tabWidth": 2,
  "trailingComma": "es5",
  "endOfLine": "lf"
}
```

```json
// package.json 添加格式化脚本
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "lint": "eslint src --ext .ts,.vue --fix",
    "format": "prettier --write src/**/*.{ts,vue}"
  }
}
```

---

## 四、数据库规范

```sql
-- 表名：小写+下划线，业务前缀
sys_user
competition
competition_signup
team_member

-- 字段名：小写+下划线
user_id
enrolled_count
signup_start
created_at

-- 每张表必须有的字段
id          BIGSERIAL PRIMARY KEY    -- 主键
created_at  TIMESTAMPTZ DEFAULT NOW() -- 创建时间
updated_at  TIMESTAMPTZ DEFAULT NOW() -- 更新时间

-- 软删除（需要时使用）
is_deleted  BOOLEAN DEFAULT FALSE

-- 乐观锁（并发控制时使用）
version     INT DEFAULT 0

-- 每张表必须有注释
COMMENT ON TABLE competition IS '竞赛信息表';
COMMENT ON COLUMN competition.max_quota IS '名额上限，NULL表示不限制';
```

---

## 五、CR 检查清单

```
提交PR前自查：

命名
  □ 类名、方法名、变量名是否有意义
  □ 没有使用 a、b、temp、data 这类无意义命名

注释
  □ 每个类有类注释
  □ 复杂业务逻辑有行内注释说明原因
  □ 没有无意义的注释（如：// 获取用户）

分层
  □ Controller 没有业务逻辑
  □ Service 没有直接SQL操作
  □ Repository 没有业务逻辑

异常处理
  □ 没有空的 catch 块
  □ 没有 e.printStackTrace()
  □ 使用了 BusinessException 统一处理

前端
  □ 所有变量有 TypeScript 类型
  □ 没有使用 any 类型（除非必要）
  □ 运行 npm run lint 没有报错
  □ 运行 npm run format 格式化过代码

通用
  □ 没有提交 .env 文件
  □ 没有提交 node_modules 目录
  □ 没有注释掉的废弃代码大段保留
```