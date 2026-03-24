# 在线编程评测系统 Redis 设计说明

## 1. 设计目标

本系统中，MySQL 负责核心业务数据的持久化存储，Redis 负责高频访问数据的缓存、排行榜维护、登录状态管理和基础统计。

Redis 的主要作用如下：

- 缓存热点数据，降低 MySQL 查询压力
- 存储用户登录态或 Token 信息
- 维护比赛/作业排行榜
- 缓存系统配置参数
- 记录判题服务心跳和短期监控状态
- 支撑基础限流与提交频率控制

## 2. 设计原则

- 核心业务数据必须以 MySQL 为准，Redis 不作为主存储
- 所有 Key 统一命名，便于管理和排查
- 缓存数据必须设置合理过期时间，避免脏数据长期存在
- 写操作以数据库落库为主，Redis 用于读优化和临时状态
- 判题任务异步处理使用 RabbitMQ，Redis 不承担任务队列职责

## 3. Key 命名规范

统一命名格式：

```text
oj:{模块}:{业务}:{标识}
```

示例：

```text
oj:auth:token:{token}
oj:user:perm:{userId}
oj:problem:detail:{problemId}
oj:contest:rank:{contestId}
oj:config:{configKey}
oj:judge:heartbeat:{serviceId}
```

## 4. Redis 核心业务设计

### 4.1 登录状态缓存

**用途**

用于保存学生、教师、管理员登录后的会话信息或 Token 信息。

**设计**

- Key：`oj:auth:token:{token}`
- 类型：`String` 或 `Hash`
- Value：`userId`、`username`、`roleCode`
- 过期时间：`2小时` 到 `12小时`

**说明**

- 用户登录成功后写入 Redis
- 用户退出登录时删除对应 Key
- 如果后续使用 JWT，也可以将 Token 黑名单或刷新信息放入 Redis

### 4.2 用户权限缓存

**用途**

用于缓存用户当前拥有的权限集合，减少频繁查询角色和权限表。

**设计**

- Key：`oj:user:perm:{userId}`
- 类型：`Set`
- Value：权限编码，如 `problem:add`、`contest:export`、`user:disable`
- 过期时间：`30分钟`

**说明**

- 用户登录时加载权限集合写入 Redis
- 管理员修改角色权限后，应删除相关用户的权限缓存

### 4.3 题目详情缓存

**用途**

用于缓存高频访问的题目详情，提高学生端题目浏览速度。

**设计**

- Key：`oj:problem:detail:{problemId}`
- 类型：`String(JSON)` 或 `Hash`
- Value：题目标题、题面、分类、标签、难度、时间限制、内存限制
- 过期时间：`30分钟`

**说明**

- 题目列表和详情页优先从 Redis 查询
- 教师修改题目后，删除对应题目缓存
- 题目测试用例不建议长期缓存到 Redis

### 4.4 比赛/作业排行榜

**用途**

用于维护比赛或作业中的实时排行榜。

**设计**

- Key：`oj:contest:rank:{contestId}`
- 类型：`ZSet`
- Member：`userId`
- Score：总分 `totalScore`

如需记录罚时或完成时间，可增加：

- Key：`oj:contest:rank:extra:{contestId}`
- 类型：`Hash`
- Field：`userId`
- Value：罚时、最后通过时间等扩展信息

**说明**

- 学生提交结果更新后，同步更新排行榜
- 比赛结束后，可将最终数据写回 MySQL 的 `contest_user`
- Redis 非常适合做实时排行榜，这也是毕业设计里最有代表性的 Redis 场景

### 4.5 系统配置缓存

**用途**

用于缓存管理员维护的系统配置，如判题时间限制、并发参数、系统开关等。

**设计**

- Key：`oj:config:{configKey}`
- 类型：`String`
- Value：配置值
- 过期时间：可不设置，或由管理员更新时主动刷新

示例：

```text
oj:config:judge.maxCpuTime
oj:config:judge.maxMemory
oj:config:judge.maxConcurrent
oj:config:system.registerEnabled
```

**说明**

- 配置源数据保存在 MySQL 的 `sys_config`
- Redis 中只作为快速读取层
- 管理员修改配置时，需要同步刷新 Redis

### 4.6 判题服务心跳缓存

**用途**

用于保存判题服务实例的短期在线状态，便于管理员端查看服务是否在线。

**设计**

- Key：`oj:judge:heartbeat:{serviceId}`
- 类型：`String` 或 `Hash`
- Value：服务状态、当前任务数、最近心跳时间
- 过期时间：`30秒`

**说明**

- 判题服务每隔几秒刷新一次心跳
- 如果 Key 过期未刷新，可判定该服务实例离线
- 长期监控信息仍可落到 MySQL 的 `judge_service`

### 4.7 提交频率限制

**用途**

防止用户短时间内重复提交代码，减少恶意请求和系统压力。

**设计**

- Key：`oj:submit:limit:{userId}:{problemId}`
- 类型：`String`
- 值：任意标记值，如 `1`
- 过期时间：`3秒` 到 `5秒`

**说明**

- 可通过 `SETNX` 或类似原子操作实现短时防重复提交
- 该功能适合作为系统稳定性优化点写入论文

### 4.8 提交量统计

**用途**

用于记录每日提交量、通过量等基础统计指标。

**设计**

- Key：`oj:stats:submit:day:{yyyyMMdd}`
- 类型：`String`
- 操作：`INCR`

可扩展：

- `oj:stats:accept:day:{yyyyMMdd}`
- `oj:stats:user:submit:{userId}:{yyyyMMdd}`

**说明**

- Redis 适合做短期高频计数
- 长期报表数据建议通过定时任务汇总到 MySQL

## 5. Redis 数据结构与业务对应关系

| 业务场景 | Key 示例 | 数据结构 | 说明 |
| --- | --- | --- | --- |
| 登录状态 | `oj:auth:token:xxx` | `String` / `Hash` | 保存登录用户会话 |
| 用户权限缓存 | `oj:user:perm:1001` | `Set` | 保存用户权限编码集合 |
| 题目详情缓存 | `oj:problem:detail:2001` | `String` / `Hash` | 缓存热点题目详情 |
| 比赛排行榜 | `oj:contest:rank:3001` | `ZSet` | 保存实时排名 |
| 配置缓存 | `oj:config:judge.maxCpuTime` | `String` | 缓存系统配置 |
| 判题服务心跳 | `oj:judge:heartbeat:1` | `String` / `Hash` | 保存服务短期状态 |
| 提交限流 | `oj:submit:limit:1001:2001` | `String` | 防止短时间重复提交 |
| 提交统计 | `oj:stats:submit:day:20260324` | `String` | 日提交计数 |

## 6. 与 MySQL 的关系

Redis 负责缓存和临时状态，MySQL 负责最终落库。

对应关系如下：

| Redis 业务 | MySQL 对应表 | 说明 |
| --- | --- | --- |
| 登录状态缓存 | `sys_user`、`login_log` | Redis 存当前登录态，MySQL 存用户和登录日志 |
| 用户权限缓存 | `sys_role`、`sys_permission`、`sys_role_permission` | Redis 存权限集合，MySQL 存权限关系 |
| 题目详情缓存 | `problem`、`problem_category`、`problem_tag` | Redis 存热点题目详情，MySQL 为源数据 |
| 比赛排行榜 | `contest_user`、`submission` | Redis 维护实时排名，MySQL 存最终结果 |
| 系统配置缓存 | `sys_config` | Redis 缓存配置值，MySQL 持久化配置 |
| 判题服务心跳 | `judge_service` | Redis 存在线状态，MySQL 存监控记录 |
| 提交统计 | `submission` | Redis 做快速计数，MySQL 存提交明细 |

## 7. 开发优先级建议

如果你按毕业设计一期实现，建议 Redis 优先做以下 4 项：

1. 登录状态缓存
2. 用户权限缓存
3. 题目详情缓存
4. 比赛排行榜

这四项最容易体现 Redis 的价值，也最适合在论文中说明性能优化点。

## 8. 论文中可使用的描述

可以在论文中这样表述：

> 系统采用 Redis 作为高性能缓存中间件，主要用于保存用户登录状态、缓存热点题目数据、维护比赛排行榜以及缓存系统配置，从而减少数据库访问压力，提高系统在高并发提交和查询场景下的响应效率。

## 9. 注意事项

- 不要把 Redis 作为唯一数据源，核心业务必须落 MySQL
- 判题任务调度应由 RabbitMQ 负责，不建议 Redis 和 RabbitMQ 职责重叠
- 缓存更新必须考虑一致性，教师修改题目或管理员修改配置后应及时删除或刷新缓存
- 排行榜属于高频读场景，适合 Redis；提交明细属于核心业务数据，必须落库

