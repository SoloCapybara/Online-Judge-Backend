# 在线编程评测系统数据库设计说明

## 1. 设计目标

本设计面向高校教学场景下的在线编程评测系统，覆盖以下核心业务：

- 用户与权限管理
- 题目与测试用例管理
- 在线提交与判题结果管理
- 比赛/作业发布与参与管理
- 标签分类与基础统计支撑

数据库设计遵循以下原则：

- 学生、教师、管理员统一使用一张用户表管理
- 比赛和作业统一使用一张活动表，通过类型字段区分
- 判题主记录和测试点明细分表存储，便于扩展
- 优先保证一期开发可落地，后续可平滑扩展

## 2. 表设计总览

本方案共设计 12 张核心业务表：

| 序号 | 表名 | 中文名 | 说明 |
| --- | --- | --- | --- |
| 1 | `sys_user` | 用户表 | 存储学生、教师、管理员基础信息 |
| 2 | `sys_role` | 角色表 | 存储系统角色定义 |
| 3 | `sys_user_role` | 用户角色关联表 | 存储用户与角色的多对多关系 |
| 4 | `problem` | 题目表 | 存储题目主体信息 |
| 5 | `problem_case` | 题目测试用例表 | 存储题目的输入输出测试数据 |
| 6 | `problem_tag` | 题目标签表 | 存储题目标签，如数组、动态规划 |
| 7 | `problem_tag_rel` | 题目标签关联表 | 存储题目与标签的多对多关系 |
| 8 | `submission` | 提交记录表 | 存储用户每次代码提交记录 |
| 9 | `submission_case` | 提交测试点结果表 | 存储一次提交在每个测试点上的判题结果 |
| 10 | `contest` | 比赛/作业表 | 存储教师发布的比赛或作业 |
| 11 | `contest_problem` | 比赛题目关联表 | 存储比赛/作业包含的题目 |
| 12 | `contest_user` | 比赛参与表 | 存储用户参与比赛/作业的情况和统计结果 |

## 3. 详细表设计

### 3.1 `sys_user` 用户表

**表说明**

统一维护系统所有用户信息，包括学生、教师、管理员。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 用户ID | bigint | PK | 主键 |
| `username` | 用户名 | varchar(50) | NOT NULL, UNIQUE | 登录账号 |
| `password` | 密码 | varchar(100) | NOT NULL | 建议存加密后的密码 |
| `real_name` | 真实姓名 | varchar(50) | NOT NULL | 用户姓名 |
| `student_no` | 学号/工号 | varchar(30) | NULL | 学生填学号，教师可填工号 |
| `email` | 邮箱 | varchar(100) | NULL | 联系邮箱 |
| `phone` | 手机号 | varchar(20) | NULL | 联系电话 |
| `avatar` | 头像地址 | varchar(255) | NULL | 头像图片路径 |
| `gender` | 性别 | tinyint | NULL | `0`未知，`1`男，`2`女 |
| `status` | 状态 | tinyint | NOT NULL | `1`启用，`0`禁用 |
| `last_login_time` | 最后登录时间 | datetime | NULL | 最近一次登录时间 |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |
| `update_time` | 更新时间 | datetime | NOT NULL | 记录更新时间 |

**说明**

- 一期推荐所有身份统一放在该表，不拆分学生表、教师表、管理员表。
- 角色权限通过 `sys_user_role` 关联，不直接写死在用户表中。

### 3.2 `sys_role` 角色表

**表说明**

定义系统角色，用于权限控制。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 角色ID | bigint | PK | 主键 |
| `role_code` | 角色编码 | varchar(50) | NOT NULL, UNIQUE | 如 `STUDENT`、`TEACHER`、`ADMIN` |
| `role_name` | 角色名称 | varchar(50) | NOT NULL | 如学生、教师、管理员 |
| `description` | 角色描述 | varchar(255) | NULL | 角色说明 |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |
| `update_time` | 更新时间 | datetime | NOT NULL | 记录更新时间 |

### 3.3 `sys_user_role` 用户角色关联表

**表说明**

实现用户与角色的多对多关联。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 主键ID | bigint | PK | 主键 |
| `user_id` | 用户ID | bigint | NOT NULL, FK | 关联 `sys_user.id` |
| `role_id` | 角色ID | bigint | NOT NULL, FK | 关联 `sys_role.id` |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

**唯一约束建议**

- `unique(user_id, role_id)`

### 3.4 `problem` 题目表

**表说明**

存储题目题面、资源限制、难度、出题人等信息。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 题目ID | bigint | PK | 主键 |
| `title` | 题目标题 | varchar(200) | NOT NULL | 题目名称 |
| `content` | 题目描述 | longtext | NOT NULL | 题目正文 |
| `input_desc` | 输入描述 | text | NULL | 输入格式说明 |
| `output_desc` | 输出描述 | text | NULL | 输出格式说明 |
| `sample_input` | 样例输入 | text | NULL | 页面展示的样例输入 |
| `sample_output` | 样例输出 | text | NULL | 页面展示的样例输出 |
| `hint` | 提示说明 | text | NULL | 补充提示 |
| `difficulty` | 难度 | tinyint | NOT NULL | `1`简单，`2`中等，`3`困难 |
| `time_limit` | 时间限制 | int | NOT NULL | 单位毫秒 |
| `memory_limit` | 内存限制 | int | NOT NULL | 单位MB |
| `judge_mode` | 判题模式 | varchar(20) | NOT NULL | 如 `ACM`、`OI` |
| `status` | 题目状态 | tinyint | NOT NULL | `0`草稿，`1`发布，`2`下线 |
| `creator_id` | 创建人ID | bigint | NOT NULL, FK | 关联 `sys_user.id` |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |
| `update_time` | 更新时间 | datetime | NOT NULL | 记录更新时间 |

### 3.5 `problem_case` 题目测试用例表

**表说明**

存储题目的测试输入输出数据，用于判题。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 测试用例ID | bigint | PK | 主键 |
| `problem_id` | 题目ID | bigint | NOT NULL, FK | 关联 `problem.id` |
| `case_no` | 用例序号 | int | NOT NULL | 第几个测试点 |
| `input_data` | 输入数据 | longtext | NOT NULL | 测试输入内容 |
| `output_data` | 输出数据 | longtext | NOT NULL | 标准输出内容 |
| `score` | 分值 | int | NOT NULL | OI 模式下用于按点计分 |
| `is_sample` | 是否样例 | tinyint | NOT NULL | `1`是，`0`否 |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

**唯一约束建议**

- `unique(problem_id, case_no)`

### 3.6 `problem_tag` 题目标签表

**表说明**

存储题目标签，用于筛选和分类。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 标签ID | bigint | PK | 主键 |
| `tag_name` | 标签名称 | varchar(50) | NOT NULL, UNIQUE | 如数组、二分、DFS |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

### 3.7 `problem_tag_rel` 题目标签关联表

**表说明**

建立题目与标签的多对多关系。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 主键ID | bigint | PK | 主键 |
| `problem_id` | 题目ID | bigint | NOT NULL, FK | 关联 `problem.id` |
| `tag_id` | 标签ID | bigint | NOT NULL, FK | 关联 `problem_tag.id` |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

**唯一约束建议**

- `unique(problem_id, tag_id)`

### 3.8 `submission` 提交记录表

**表说明**

记录用户每一次代码提交及其判题总结果，是系统核心业务表。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 提交ID | bigint | PK | 主键 |
| `user_id` | 用户ID | bigint | NOT NULL, FK | 关联 `sys_user.id` |
| `problem_id` | 题目ID | bigint | NOT NULL, FK | 关联 `problem.id` |
| `contest_id` | 比赛/作业ID | bigint | NULL, FK | 关联 `contest.id`，平时练习可为空 |
| `language` | 编程语言 | varchar(20) | NOT NULL | 如 `java`、`cpp`、`python` |
| `source_code` | 源代码 | mediumtext | NOT NULL | 用户提交代码 |
| `status` | 判题状态 | varchar(20) | NOT NULL | 如 `WAITING`、`JUDGING`、`AC`、`WA`、`TLE`、`MLE`、`RE`、`CE` |
| `time_used` | 运行耗时 | int | NULL | 单位毫秒 |
| `memory_used` | 内存消耗 | int | NULL | 单位KB或MB，需统一 |
| `score` | 得分 | int | NULL | OI 模式或作业评分可使用 |
| `error_message` | 错误信息 | text | NULL | 编译错误或运行错误信息 |
| `submit_time` | 提交时间 | datetime | NOT NULL | 用户提交时间 |
| `judge_time` | 判题完成时间 | datetime | NULL | 判题完成时间 |

### 3.9 `submission_case` 提交测试点结果表

**表说明**

记录某次提交在每一个测试点上的运行结果。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 主键ID | bigint | PK | 主键 |
| `submission_id` | 提交ID | bigint | NOT NULL, FK | 关联 `submission.id` |
| `case_no` | 测试点序号 | int | NOT NULL | 第几个测试点 |
| `status` | 测试点状态 | varchar(20) | NOT NULL | 如 `AC`、`WA`、`TLE`、`RE` |
| `time_used` | 运行耗时 | int | NULL | 单位毫秒 |
| `memory_used` | 内存消耗 | int | NULL | 单位KB或MB，需统一 |
| `message` | 结果说明 | varchar(255) | NULL | 测试点提示信息 |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

**唯一约束建议**

- `unique(submission_id, case_no)`

### 3.10 `contest` 比赛/作业表

**表说明**

统一管理比赛和作业，通过 `type` 区分业务类型。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 比赛/作业ID | bigint | PK | 主键 |
| `title` | 标题 | varchar(200) | NOT NULL | 比赛或作业名称 |
| `description` | 描述 | text | NULL | 页面展示说明 |
| `type` | 类型 | varchar(20) | NOT NULL | `CONTEST` 或 `HOMEWORK` |
| `start_time` | 开始时间 | datetime | NOT NULL | 开始时间 |
| `end_time` | 结束时间 | datetime | NOT NULL | 截止时间 |
| `status` | 状态 | tinyint | NOT NULL | `0`未发布，`1`进行中，`2`已结束，`3`已关闭 |
| `creator_id` | 创建人ID | bigint | NOT NULL, FK | 关联 `sys_user.id` |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |
| `update_time` | 更新时间 | datetime | NOT NULL | 记录更新时间 |

### 3.11 `contest_problem` 比赛题目关联表

**表说明**

建立比赛/作业与题目之间的关系，并控制显示顺序和分值。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 主键ID | bigint | PK | 主键 |
| `contest_id` | 比赛/作业ID | bigint | NOT NULL, FK | 关联 `contest.id` |
| `problem_id` | 题目ID | bigint | NOT NULL, FK | 关联 `problem.id` |
| `display_order` | 显示顺序 | int | NOT NULL | 题目排序 |
| `score` | 题目分值 | int | NOT NULL | 比赛或作业中的题目分数 |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

**唯一约束建议**

- `unique(contest_id, problem_id)`

### 3.12 `contest_user` 比赛参与表

**表说明**

记录用户参与比赛/作业的情况，也可用于存储成绩统计和排名结果。

| 字段名 | 中文名 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 主键ID | bigint | PK | 主键 |
| `contest_id` | 比赛/作业ID | bigint | NOT NULL, FK | 关联 `contest.id` |
| `user_id` | 用户ID | bigint | NOT NULL, FK | 关联 `sys_user.id` |
| `total_score` | 总分 | int | NULL | 用户在该活动中的总得分 |
| `ac_count` | 通过题数 | int | NULL | 通过题目数量 |
| `rank_no` | 排名 | int | NULL | 排名结果 |
| `join_time` | 参与时间 | datetime | NULL | 首次进入或报名时间 |
| `finish_time` | 完成时间 | datetime | NULL | 完成时间 |
| `create_time` | 创建时间 | datetime | NOT NULL | 记录创建时间 |

**唯一约束建议**

- `unique(contest_id, user_id)`

## 4. 表之间的关系说明

### 4.1 用户与角色关系

- `sys_user` 和 `sys_role` 是多对多关系
- 中间通过 `sys_user_role` 进行关联
- 一个用户可以拥有多个角色
- 一个角色也可以分配给多个用户

### 4.2 用户与题目关系

- `sys_user` 和 `problem` 是一对多关系
- 一个用户可以创建多道题目
- 一道题目只能有一个创建人
- 关联字段为 `problem.creator_id -> sys_user.id`

### 4.3 题目与测试用例关系

- `problem` 和 `problem_case` 是一对多关系
- 一道题目可以有多个测试用例
- 一个测试用例只属于一题
- 关联字段为 `problem_case.problem_id -> problem.id`

### 4.4 题目与标签关系

- `problem` 和 `problem_tag` 是多对多关系
- 中间通过 `problem_tag_rel` 关联
- 一道题目可以有多个标签
- 一个标签也可以标记多道题目

### 4.5 用户与提交记录关系

- `sys_user` 和 `submission` 是一对多关系
- 一个用户可以提交多次代码
- 每条提交记录只属于一个用户
- 关联字段为 `submission.user_id -> sys_user.id`

### 4.6 题目与提交记录关系

- `problem` 和 `submission` 是一对多关系
- 一道题目可以收到多次提交
- 每条提交记录只对应一道题目
- 关联字段为 `submission.problem_id -> problem.id`

### 4.7 提交记录与测试点结果关系

- `submission` 和 `submission_case` 是一对多关系
- 一次提交可以对应多个测试点判题结果
- 每条测试点结果只属于一条提交记录
- 关联字段为 `submission_case.submission_id -> submission.id`

### 4.8 比赛/作业与题目关系

- `contest` 和 `problem` 是多对多关系
- 中间通过 `contest_problem` 关联
- 一个比赛/作业可包含多道题目
- 一道题目也可以被多个比赛/作业复用

### 4.9 比赛/作业与用户关系

- `contest` 和 `sys_user` 是多对多关系
- 中间通过 `contest_user` 关联
- 一个比赛/作业可对应多个参与者
- 一个用户也可参加多个比赛或作业

### 4.10 提交记录与比赛/作业关系

- `contest` 和 `submission` 是一对多关系
- 比赛/作业中的提交可通过 `submission.contest_id` 关联
- 普通练习提交时 `contest_id` 可为空

## 5. 业务流程与表的对应关系

### 5.1 用户登录与权限控制

- 用户基础信息保存在 `sys_user`
- 用户角色保存在 `sys_role`
- 用户与角色关系保存在 `sys_user_role`
- 登录后由 Spring Security 根据角色控制访问权限

### 5.2 教师创建题目

- 教师在 `problem` 中创建题目主信息
- 题目测试数据写入 `problem_case`
- 题目标签写入 `problem_tag_rel`

### 5.3 学生在线提交代码

- 学生提交代码后，生成一条 `submission`
- 状态初始可设为 `WAITING`
- 判题服务处理后更新 `submission.status`
- 每个测试点结果写入 `submission_case`

### 5.4 教师发布比赛或作业

- 发布活动时先写入 `contest`
- 选择题目后写入 `contest_problem`
- 学生参与后可在 `contest_user` 中记录统计信息

### 5.5 成绩统计与排名

- 个人提交记录来源于 `submission`
- 某个活动下的用户总成绩可统计到 `contest_user`
- 排行榜可基于 `contest_user` 或从 `submission` 实时计算

## 6. 推荐索引设计

为了提高查询性能，建议增加以下索引：

| 表名 | 索引字段 | 用途 |
| --- | --- | --- |
| `sys_user` | `username` | 登录查询 |
| `sys_user` | `student_no` | 学号检索 |
| `problem` | `(status, difficulty)` | 题目列表筛选 |
| `problem_case` | `(problem_id, case_no)` | 测试点查询 |
| `problem_tag_rel` | `(problem_id, tag_id)` | 标签关联查询 |
| `submission` | `(user_id, submit_time)` | 个人提交记录 |
| `submission` | `(problem_id, submit_time)` | 题目提交记录 |
| `submission` | `(contest_id, user_id)` | 比赛内提交查询 |
| `submission_case` | `(submission_id, case_no)` | 测试点结果查询 |
| `contest` | `(type, status, start_time)` | 比赛/作业列表查询 |
| `contest_problem` | `(contest_id, display_order)` | 活动题目列表 |
| `contest_user` | `(contest_id, rank_no)` | 排行榜查询 |

## 7. 一期开发建议

如果你想尽快开始编码，建议按照下面顺序实现：

1. 先建用户与角色表：`sys_user`、`sys_role`、`sys_user_role`
2. 再建题目相关表：`problem`、`problem_case`
3. 然后建提交判题表：`submission`、`submission_case`
4. 最后建比赛相关表：`contest`、`contest_problem`、`contest_user`
5. 标签表 `problem_tag`、`problem_tag_rel` 可与题目模块并行开发

## 8. 后续可扩展方向

如果毕业设计后续要继续完善，可以考虑再扩展以下内容：

- `judge_queue_log`：判题队列日志表
- `login_log`：登录日志表
- `sys_config`：系统参数配置表
- `announcement`：公告表
- `file_record`：测试数据文件或头像文件记录表

以上扩展表不是一期必须内容，建议先完成核心 12 张表。

