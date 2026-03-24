# Online Judge Backend

基于 `Spring Boot 3 + MyBatis-Plus + Spring Security + MySQL + Redis + RabbitMQ` 的在线编程评测系统后端。

## 技术栈

- `Spring Boot 3.3.6`
- `Spring Security`
- `MyBatis-Plus`
- `MySQL 8.x`
- `Redis`
- `RabbitMQ`
- `SpringDoc OpenAPI`
- `Lombok`

## 当前已完成的基础配置

- Web、校验、安全、MyBatis-Plus、Redis、RabbitMQ 依赖接入
- 统一返回结构 `BaseResponse`
- 全局异常处理 `GlobalExceptionHandler`
- 初始安全配置 `SecurityConfig`
- 健康检查接口 `/api/health`
- Swagger 文档入口 `/api/doc.html`
- 数据库设计文档 [docs/database-design.md](docs/database-design.md)
- Redis 设计文档 [docs/redis-design.md](docs/redis-design.md)
- MySQL 建表脚本 [sql/online_judge_schema.sql](sql/online_judge_schema.sql)

## 推荐目录结构

```text
src/main/java/com/dxd/oj
├── common          # 通用响应、错误码、工具类
├── config          # Spring 配置
├── controller      # 控制层
├── exception       # 异常处理
├── mapper          # MyBatis Mapper
├── model
│   ├── dto         # 请求对象
│   ├── entity      # 实体类
│   └── vo          # 返回对象
├── service         # 业务接口
└── service/impl    # 业务实现
```

## 本地启动前准备

1. 创建数据库：`online_judge`
2. 按需配置环境变量：
   - `OJ_DB_URL`
   - `OJ_DB_USERNAME`
   - `OJ_DB_PASSWORD`
   - `OJ_REDIS_HOST`
   - `OJ_REDIS_PORT`
   - `OJ_RABBITMQ_HOST`
   - `OJ_RABBITMQ_PORT`
3. 本地准备：
   - `MySQL`
   - `Redis`
   - `RabbitMQ`

## 启动方式

```bash
./mvnw spring-boot:run
```

启动后可访问：

- 健康检查：`http://localhost:8101/api/health`
- Knife4j 文档：`http://localhost:8101/api/doc.html`
- 自定义文档页：`http://localhost:8101/api/apidoc.html`
- Swagger UI：`http://localhost:8101/api/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8101/api/v3/api-docs`

前端或 AI 工具如果要根据接口文档生成页面或请求代码，优先使用 `OpenAPI JSON` 地址。

## 下一步建议

建议优先实现以下模块：

1. 用户与权限模块
2. 题目管理模块
3. 提交记录与判题模块
4. 比赛/作业模块

如果按当前毕业设计推进，可以先根据 [docs/database-design.md](docs/database-design.md) 编写建表 SQL 和实体类。
