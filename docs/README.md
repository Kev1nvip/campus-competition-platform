# 项目文档导航

## 文档列表

| 文档 | 说明 | 维护人 | 最后更新 |
|------|------|--------|----------|
| [PRD](./01-requirements/PRD.md) | 产品需求文档 | 负责人 | - |
| [系统架构](./02-architecture/system-architecture.md) | 架构设计 | 负责人 | - |
| [技术选型](./02-architecture/tech-stack.md) | 技术选型说明 | 负责人 | - |
| [数据库表结构](./03-database/schema.md) | 表结构说明 | 负责人 | - |
| [数据库初始化SQL](./03-database/init.sql) | 建表脚本 | 负责人 | - |
| [接口总览](./04-api/overview.md) | 接口规范与状态码 | 后端 | - |
| [认证接口](./04-api/auth.md) | 登录注册 | 后端 | - |
| [竞赛接口](./04-api/competition.md) | 竞赛管理 | 后端 | - |
| [报名接口](./04-api/signup.md) | 竞赛报名 | 后端 | - |
| [组队接口](./04-api/team.md) | 队伍管理 | 后端 | - |
| [招募接口](./04-api/recruitment.md) | 招募帖管理 | 后端 | - |
| [审核接口](./04-api/audit.md) | 审核流程 | 后端 | - |
| [获奖接口](./04-api/award.md) | 获奖记录 | 后端 | - |
| [通知接口](./04-api/notification.md) | 消息通知 | 后端 | - |
| [本地环境搭建](./05-dev-guide/local-setup.md) | 新人必读 | 负责人 | - |
| [编码规范](./05-dev-guide/coding-standards.md) | 代码规范 | 负责人 | - |
| [Git协作流程](./05-dev-guide/git-workflow.md) | 分支与提交规范 | 负责人 | - |
| [成员分工](./06-team/member-contribution.md) | 贡献记录 | 负责人 | - |
| [会议记录](./06-team/meeting-notes.md) | 讨论结论 | 轮流 | - |

## 快速入口

新成员第一步看这里：
→ [本地环境搭建](./05-dev-guide/local-setup.md)

开发前必读：
→ [PRD](./01-requirements/PRD.md)
→ [数据库表结构](./03-database/schema.md)
→ [接口总览](./04-api/overview.md)

联调时看这里：
→ [各模块接口文档](./04-api/)
→ Knife4j在线文档：http://localhost:8080/doc.html

---
认真写
PRD.md
  谁写：你（负责人）
  何时写：开发前
  内容：业务流程、功能说明、验收标准
  用途：所有人对齐需求，防止返工

init.sql
  谁写：你（负责人）
  何时写：第一周
  内容：完整建表SQL，可直接执行
  用途：所有人本地初始化数据库

schema.md
  谁写：你（负责人）
  何时写：第一周，和init.sql同步
  内容：每张表的字段说明、关联关系、设计原因
  用途：开发时查阅，答辩时展示

api/overview.md + 各模块接口文档
  谁写：后端开发者写初稿，前端联调时补充
  何时写：接口开发完成后立即更新
  内容：请求方式、路径、参数、响应示例
  用途：前后端联调依据

local-setup.md
  谁写：你（负责人）
  何时写：第一周环境搭建完成后
  内容：从零到本地跑通的完整步骤
  用途：队友环境搭建，答辩前临时换电脑

---
简单写
system-architecture.md
  内容：一张架构图 + 各层说明
  用途：答辩展示技术深度

tech-stack.md
  内容：技术选型列表 + 每项选型理由一句话
  用途：答辩时被问到"为什么用这个"

coding-standards.md
  内容：命名规范 + 注释规范 + 分层规范
  用途：统一代码风格，CR时有依据

git-workflow.md
  内容：分支命名 + commit规范 + PR流程
  用途：已经在上一步给你了，直接粘贴进去

member-contribution.md
  内容：每人负责模块 + 每周完成情况
  用途：课程评分依据，证明每人贡献

meeting-notes.md
  内容：每次讨论的结论，3-5条即可
  用途：证明团队有协作过程