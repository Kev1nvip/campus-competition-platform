# Git 协作流程

> 适用范围：全体开发成员
> 最后更新：2026-03
> 维护人：负责人

---

## 一、整体协作模型

```
负责人仓库
├── main         ← 最终稳定版本，受保护，只接受dev合并
└── dev          ← 集成分支，接受所有队友的PR

队友Fork仓库（每一个）
├── main         ← 同步负责人main，不在这里开发
├── dev          ← 同步负责人dev，功能分支合并到这里
└── feature/xxx  ← 日常开发在这里
```

```
代码流转方向：

本地 feature/xxx
      ↓ push
自己Fork的 feature/xxx
      ↓ PR
自己Fork的 dev
      ↓ PR
负责人仓库的 dev
      ↓ PR（负责人操作）
负责人仓库的 main
```

---

## 二、初始化流程（所有人第一次配置）

### 负责人操作（只做一次）

```bash
# Step1：克隆自己的仓库到本地
git clone git@github.com:负责人用户名/campus-competition-platform.git
cd campus-competition-platform

# Step2：从main创建dev分支
git checkout main
git checkout -b dev

# Step3：推送dev到远程
git push origin dev

# 现在负责人仓库有两个分支：main 和 dev
```

```
Step4：配置分支保护规则（GitHub上操作）

保护 main 分支：
  Settings → Branches → Add rule
  Branch name pattern：main
  ✅ Require a pull request before merging
  保存
```

### 队友操作（每人各自操作一次）

```bash
# Step1：Fork负责人仓库
# 浏览器打开负责人仓库
# 点击右上角 Fork → Create fork

# Step2：克隆自己的Fork到本地
git clone git@github.com:你的用户名/campus-competition-platform.git
cd campus-competition-platform

# Step3：添加负责人仓库为upstream
git remote add upstream git@github.com:负责人用户名/campus-competition-platform.git

# 验证remote配置
git remote -v
# 应看到：
# origin    git@github.com:你的用户名/campus-competition-platform.git (fetch)
# origin    git@github.com:你的用户名/campus-competition-platform.git (push)
# upstream  git@github.com:负责人用户名/campus-competition-platform.git (fetch)
# upstream  git@github.com:负责人用户名/campus-competition-platform.git (push)

# Step4：同步负责人的dev分支到自己的Fork
git fetch upstream
git checkout -b dev upstream/dev
git push origin dev

# 现在你的Fork也有了dev分支
# 验证：
git branch -a
# 应看到：
# * dev
#   main
#   remotes/origin/main
#   remotes/origin/dev
#   remotes/upstream/main
#   remotes/upstream/dev
```

---

## 三、分支规范

### 分支类型

| 分支 | 位置 | 说明 | 保护 |
|------|------|------|------|
| main | 负责人仓库 | 最终稳定版本 | ✅ 受保护 |
| dev | 负责人仓库 | 集成测试分支 | ✅ 受保护 |
| main | 队友Fork | 同步负责人main | 不在这开发 |
| dev | 队友Fork | 功能集成分支 | 功能分支合并到这 |
| feature/xxx | 队友Fork | 日常开发分支 | 自由创建删除 |
| fix/xxx | 队友Fork | Bug修复分支 | 自由创建删除 |

### 功能分支命名

```bash
# 新功能
feature/user-login
feature/competition-list
feature/competition-signup
feature/team-create
feature/team-invite
feature/recruitment-post
feature/audit-panel
feature/award-record
feature/notification
feature/ai-recommend
feature/redis-cache
feature/rabbitmq-notify
feature/docker-config

# Bug修复
fix/login-token-expired
fix/signup-count-error
fix/competition-status-auto-update
```

---

## 四、Commit 规范

### 格式

```
type: 简短描述
```

### type 类型

| type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | feat: 完成竞赛列表分页查询 |
| fix | 修复Bug | fix: 修复报名并发计数错误 |
| docs | 文档更新 | docs: 更新接口文档 |
| style | 代码格式 | style: 统一缩进格式 |
| refactor | 重构 | refactor: 提取竞赛状态计算逻辑 |
| chore | 工程配置 | chore: 添加docker-compose配置 |

### 好的commit vs 差的commit

```bash
# ✅ 好的
feat: 完成用户登录接口
feat: 添加竞赛列表状态筛选
fix: 修复token过期后未跳转登录页
docs: 补充报名接口入参说明
chore: 配置maven阿里云镜像

# ❌ 差的
update
修改
aaa
111
fix bug
```

### 提交粒度

```
✅ 完成一个接口提交一次
✅ 完成一个页面组件提交一次
✅ 修复一个Bug提交一次
❌ 开发三天才提交一次
❌ 把登录和注册塞进一个commit
```

---

## 五、日常开发完整流程

### 第一步：同步最新代码（每天开始工作前）

```bash
# 同步负责人的dev到本地
git checkout dev
git fetch upstream
git merge upstream/dev

# 推送到自己的Fork的dev（保持同步）
git push origin dev
```

### 第二步：创建功能分支

```bash
# 从本地dev创建功能分支
git checkout dev
git checkout -b feature/你的功能名

# 示例
git checkout -b feature/competition-list
```

### 第三步：开发并提交

```bash
# 查看修改了哪些文件
git status

# 查看具体修改内容
git diff

# 添加到暂存区
git add .
# 或指定文件
git add src/views/CompetitionList.vue

# 提交
git commit -m "feat: 完成竞赛列表基础展示"

# 继续开发，继续提交
git commit -m "feat: 添加竞赛状态筛选"
git commit -m "feat: 添加分页功能"
git commit -m "fix: 修复筛选条件重置问题"

# 推送到自己Fork的功能分支
git push origin feature/competition-list
```

### 第四步：功能分支合并到自己Fork的dev

```bash
# 方式：在GitHub上操作（推荐）

# 1. 打开自己的Fork仓库
# https://github.com/你的用户名/campus-competition-platform

# 2. 点击 "Branches" 找到你的功能分支
#    或者GitHub会自动提示 "Compare & pull request"

# 3. 创建PR：
#    base repository：你的用户名/campus-competition-platform
#    base：dev                        ← 目标是自己Fork的dev
#    head：feature/competition-list   ← 来源是功能分支

# 4. 填写PR描述（见下方模板）

# 5. 自己合并（这个PR不需要别人review）
#    点击 "Merge pull request"
#    选择 "Squash and merge"
#    确认合并

# 6. 删除功能分支
#    点击 "Delete branch"
```

### 第五步：从自己Fork的dev向负责人dev发PR

```bash
# 在GitHub上操作

# 1. 打开自己的Fork仓库
# 2. 切换到dev分支
# 3. 点击 "Contribute" → "Open pull request"

# 4. 配置PR：
#    base repository：负责人用户名/campus-competition-platform
#    base：dev                        ← 目标是负责人的dev
#    head repository：你的用户名/campus-competition-platform
#    compare：dev                     ← 来源是自己Fork的dev

# 5. 填写PR描述

# 6. 提交PR，等待负责人合并
```

### 第六步：负责人合并dev → main

```bash
# 负责人操作
# 当dev分支积累了一定功能，测试没问题后
# 在GitHub上从dev向main发PR

# 1. 进入负责人仓库
# 2. 点击 "Pull requests" → "New pull request"
# 3. 配置：
#    base：main
#    compare：dev
# 4. 确认合并内容
# 5. Merge pull request
```

---

## 六、PR 描述模板

### 功能分支 → 自己Fork的dev（自己合并，简单写）

```markdown
## 完成内容
- 竞赛列表页面
- 状态筛选功能
- 分页功能

## 测试情况
- 本地测试正常
- 接口联调：待后端完成
```

### 自己Fork的dev → 负责人dev（负责人审核，认真写）

```markdown
## 本次包含的功能
- feat: 竞赛列表页面（含筛选和分页）
- feat: 竞赛详情页面
- fix: 修复状态筛选重置问题

## 测试说明
- 已本地完整测试
- 与后端接口联调通过
- 测试账号：student001 / 123456

## 注意事项
- 依赖后端竞赛列表接口（已联调完成）
- 暂未实现报名功能，下次PR包含
```

---

## 七、同步代码（重要，经常做）

### 场景1：同步负责人dev到本地（每天必做）

```bash
git checkout dev
git fetch upstream
git merge upstream/dev
git push origin dev
```

### 场景2：开发中同步最新dev到功能分支（避免积累冲突）

```bash
# 在功能分支上
git checkout feature/competition-list

# 拉取最新的dev
git fetch upstream
git merge upstream/dev

# 如果有冲突，解决后继续
```

### 场景3：同步负责人main到本地

```bash
git checkout main
git fetch upstream
git merge upstream/main
git push origin main
```

---

## 八、冲突处理

### 什么时候会冲突

```
你和另一个人修改了同一个文件的同一部分
合并时Git无法自动判断用哪个版本
需要手动解决
```

### 冲突标记

```
<<<<<<< HEAD
你的代码
=======
别人的代码
>>>>>>> upstream/dev
```

### 解决步骤

```bash
# Step1：合并时提示冲突
git merge upstream/dev
# CONFLICT (content): Merge conflict in src/api/competition.ts

# Step2：打开冲突文件
# VS Code 会高亮显示冲突
# 选择：
#   Accept Current Change   保留自己的
#   Accept Incoming Change  保留别人的
#   Accept Both Changes     两者都保留
#   手动编辑删除冲突标记

# Step3：标记已解决
git add src/api/competition.ts

# Step4：完成合并
git merge --continue
git commit -m "fix: 解决合并冲突"
```

### 减少冲突的方法

```
✅ 每天同步upstream/dev
✅ 功能分支生命周期短（1-3天完成合并）
✅ 分工时避免多人修改同一文件
✅ 修改公共文件前在群里提前说一声
```

---

## 九、紧急情况处理

### 误在main或dev分支上直接开发了

```bash
# Step1：把修改暂存
git stash

# Step2：创建正确的功能分支
git checkout dev
git checkout -b feature/正确的分支名

# Step3：恢复修改
git stash pop

# Step4：正常提交
git add .
git commit -m "feat: xxx"
```

### 误提交了不该提交的文件（如 .env）

```bash
# Step1：撤销最后一次提交（保留代码）
git reset --soft HEAD~1

# Step2：把文件加入 .gitignore

# Step3：重新提交
git add .
git commit -m "chore: 移除敏感配置文件"
```

### 功能分支代码搞乱了想重来

```bash
# 放弃所有未提交的修改
git checkout .

# 或者删除分支重建
git checkout dev
git branch -D feature/搞乱的分支名
git checkout -b feature/重建的分支名
```

### 合并后发现有Bug需要紧急修复

```bash
# 从dev创建修复分支
git checkout dev
git fetch upstream
git merge upstream/dev
git checkout -b fix/紧急问题描述

# 修复后走正常PR流程
# fix/xxx → 自己Fork的dev → 负责人dev
```

---

## 十、检查提交记录

```bash
# 查看自己的提交历史
git log --oneline --author="你的用户名"

# 查看所有分支提交历史
git log --oneline --all --graph

# 查看某个文件的修改历史
git log --oneline -- 文件路径
```

```
GitHub上查看贡献：
  负责人仓库 → Insights → Contributors
  确认4人都在列表中
  每人至少 10+ 次commit

答辩前检查：
  □ 4人都有提交记录
  □ 提交信息规范（feat/fix/docs等前缀）
  □ 没有直接提交到main的记录
```

---

## 十一、完整流程图

```
每天开始工作：
  同步upstream/dev → 本地dev → 推送origin/dev

开发新功能：
  本地dev → 创建feature/xxx → 开发提交
          → push到origin/feature/xxx
          → PR到upstream/dev（等负责人合并）

阶段性发布：
  负责人：upstream/dev → upstream/main
```

---

## 十二、每日开发命令参考

```bash
# ==================== 每天开始工作 ====================
git checkout dev
git fetch upstream
git reset --hard upstream/dev        # 关键修正点
git push origin dev --force-with-lease   # 保持个人dev和上游一致

git checkout -b feature/xxx   # 在新分支开发

# ==================== 开发中 ====================
git add .
git commit -m "feat: 完成xxx功能"
git push -u origin HEAD

# ==================== 功能完成 ====================
# GitHub提交PR：feature/xxx → 负责人dev（等负责人合并）

# ==================== 合并后清理本地分支 ====================
git checkout dev
git branch -d feature/已合并的分支名
```

负责人
```bash
# ==================== 每天开始工作 ====================
git checkout dev
git pull origin dev --ff-only     # 直接拉取最新代码

git checkout -b feature/xxx

# ==================== 开发中 ====================
git add .
git commit -m "feat: 完成xxx功能"
git push -u origin HEAD

# ==================== 功能完成 ====================
# GitHub提交PR：feature/xxx → dev

# ==================== 合并其他成员 PR 之后（重要） ====================
# 同步最新代码（其他人PR合并后）
git checkout dev
git pull origin dev

# 可选：清理远程已合并的分支
git fetch --prune
git branch -r --merged | grep -v 'main\|dev' | sed 's/origin\///' | xargs -r git push origin --delete

# ==================== 合并后清理本地分支 ====================
git checkout dev
git branch -d feature/已合并的分支名

# ==================== 实用命令 ====================
# 1. 查看哪些远程分支已合并
git branch -r --merged

# 2. 一键删除所有已合并的远程 feature 分支（谨慎使用）
git branch -r --merged | grep origin/feature | sed 's/origin\///' | xargs -r git push origin --delete

# 3. 定期清理本地无用分支
git fetch --prune
git branch --merged | grep -v 'dev' | xargs -r git branch -d
```