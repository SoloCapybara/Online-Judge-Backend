-- 在线编程评测系统 MySQL 建表脚本
-- 适用版本：MySQL 8.x
-- 说明：
-- 1. 本脚本与当前项目的 MyBatis-Plus `assign_id` 主键策略保持一致，主键不使用 AUTO_INCREMENT
-- 2. 执行前请确认当前 MySQL 实例已开启 InnoDB，并使用 utf8mb4 字符集

CREATE DATABASE IF NOT EXISTS `online_judge`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `online_judge`;

CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`              BIGINT       NOT NULL COMMENT '用户ID',
    `username`        VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`        VARCHAR(100) NOT NULL COMMENT '密码',
    `real_name`       VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `student_no`      VARCHAR(30)           DEFAULT NULL COMMENT '学号/工号',
    `email`           VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `avatar`          VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    `gender`          TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0未知，1男，2女',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    `last_login_time` DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_student_no` (`student_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='用户表';

CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          BIGINT      NOT NULL COMMENT '角色ID',
    `role_code`   VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name`   VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '角色描述',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_role_code` (`role_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='角色表';

CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `id`          BIGINT   NOT NULL COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role_user_role` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_role_id` (`role_id`),
    CONSTRAINT `fk_sys_user_role_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_sys_user_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_permission`
(
    `id`              BIGINT       NOT NULL COMMENT '权限ID',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_type` VARCHAR(20)  NOT NULL COMMENT '权限类型：API、BUTTON、MENU',
    `path`            VARCHAR(255)          DEFAULT NULL COMMENT '资源路径',
    `method`          VARCHAR(20)           DEFAULT NULL COMMENT '请求方法',
    `parent_id`       BIGINT                DEFAULT NULL COMMENT '父权限ID',
    `sort_no`         INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '权限说明',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_permission_code` (`permission_code`),
    KEY `idx_sys_permission_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='权限表';

CREATE TABLE IF NOT EXISTS `sys_role_permission`
(
    `id`            BIGINT   NOT NULL COMMENT '主键ID',
    `role_id`       BIGINT   NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT   NOT NULL COMMENT '权限ID',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_permission_role_perm` (`role_id`, `permission_id`),
    KEY `idx_sys_role_permission_perm_id` (`permission_id`),
    CONSTRAINT `fk_sys_role_permission_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
    CONSTRAINT `fk_sys_role_permission_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='角色权限关联表';

CREATE TABLE IF NOT EXISTS `problem_category`
(
    `id`            BIGINT       NOT NULL COMMENT '分类ID',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `parent_id`     BIGINT                DEFAULT NULL COMMENT '父分类ID',
    `sort_no`       INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_category_name` (`category_name`),
    KEY `idx_problem_category_parent_sort` (`parent_id`, `sort_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='题目分类表';

CREATE TABLE IF NOT EXISTS `problem`
(
    `id`            BIGINT       NOT NULL COMMENT '题目ID',
    `title`         VARCHAR(200) NOT NULL COMMENT '题目标题',
    `category_id`   BIGINT                DEFAULT NULL COMMENT '分类ID',
    `content`       LONGTEXT     NOT NULL COMMENT '题目描述',
    `input_desc`    TEXT                  DEFAULT NULL COMMENT '输入描述',
    `output_desc`   TEXT                  DEFAULT NULL COMMENT '输出描述',
    `sample_input`  TEXT                  DEFAULT NULL COMMENT '样例输入',
    `sample_output` TEXT                  DEFAULT NULL COMMENT '样例输出',
    `hint`          TEXT                  DEFAULT NULL COMMENT '提示说明',
    `difficulty`    TINYINT      NOT NULL DEFAULT 1 COMMENT '难度：1简单，2中等，3困难',
    `time_limit`    INT          NOT NULL DEFAULT 1000 COMMENT '时间限制，毫秒',
    `memory_limit`  INT          NOT NULL DEFAULT 128 COMMENT '内存限制，MB',
    `judge_mode`    VARCHAR(20)  NOT NULL DEFAULT 'ACM' COMMENT '判题模式：ACM、OI',
    `submit_count`  INT          NOT NULL DEFAULT 0 COMMENT '提交次数',
    `accept_count`  INT          NOT NULL DEFAULT 0 COMMENT '通过次数',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0草稿，1发布，2下线',
    `creator_id`    BIGINT       NOT NULL COMMENT '创建人ID',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_problem_status_difficulty` (`status`, `difficulty`),
    KEY `idx_problem_category_id` (`category_id`),
    KEY `idx_problem_creator_id` (`creator_id`),
    CONSTRAINT `fk_problem_category_id` FOREIGN KEY (`category_id`) REFERENCES `problem_category` (`id`),
    CONSTRAINT `fk_problem_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='题目表';

CREATE TABLE IF NOT EXISTS `problem_case`
(
    `id`          BIGINT   NOT NULL COMMENT '测试用例ID',
    `problem_id`  BIGINT   NOT NULL COMMENT '题目ID',
    `case_no`     INT      NOT NULL COMMENT '用例序号',
    `input_data`  LONGTEXT NOT NULL COMMENT '输入数据',
    `output_data` LONGTEXT NOT NULL COMMENT '输出数据',
    `score`       INT      NOT NULL DEFAULT 0 COMMENT '分值',
    `is_sample`   TINYINT  NOT NULL DEFAULT 0 COMMENT '是否样例：1是，0否',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_case_problem_case_no` (`problem_id`, `case_no`),
    CONSTRAINT `fk_problem_case_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='题目测试用例表';

CREATE TABLE IF NOT EXISTS `problem_tag`
(
    `id`          BIGINT      NOT NULL COMMENT '标签ID',
    `tag_name`    VARCHAR(50) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_tag_tag_name` (`tag_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='题目标签表';

CREATE TABLE IF NOT EXISTS `problem_tag_rel`
(
    `id`          BIGINT   NOT NULL COMMENT '主键ID',
    `problem_id`  BIGINT   NOT NULL COMMENT '题目ID',
    `tag_id`      BIGINT   NOT NULL COMMENT '标签ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_problem_tag_rel_problem_tag` (`problem_id`, `tag_id`),
    KEY `idx_problem_tag_rel_tag_id` (`tag_id`),
    CONSTRAINT `fk_problem_tag_rel_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`),
    CONSTRAINT `fk_problem_tag_rel_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `problem_tag` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='题目标签关联表';

CREATE TABLE IF NOT EXISTS `contest`
(
    `id`                  BIGINT       NOT NULL COMMENT '比赛/作业ID',
    `title`               VARCHAR(200) NOT NULL COMMENT '标题',
    `description`         TEXT                  DEFAULT NULL COMMENT '描述',
    `type`                VARCHAR(20)  NOT NULL COMMENT '类型：CONTEST、HOMEWORK',
    `access_type`         VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC' COMMENT '参与方式：PUBLIC、PRIVATE',
    `access_password`     VARCHAR(100)          DEFAULT NULL COMMENT '访问密码',
    `submit_limit`        INT                   DEFAULT NULL COMMENT '提交次数限制',
    `result_visible_mode` VARCHAR(20)  NOT NULL DEFAULT 'REALTIME' COMMENT '结果可见规则：REALTIME、AFTER_END',
    `start_time`          DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`            DATETIME     NOT NULL COMMENT '结束时间',
    `status`              TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0未发布，1进行中，2已结束，3已关闭',
    `creator_id`          BIGINT       NOT NULL COMMENT '创建人ID',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_contest_type_status_start_time` (`type`, `status`, `start_time`),
    KEY `idx_contest_creator_id` (`creator_id`),
    CONSTRAINT `fk_contest_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='比赛/作业表';

CREATE TABLE IF NOT EXISTS `contest_problem`
(
    `id`            BIGINT   NOT NULL COMMENT '主键ID',
    `contest_id`    BIGINT   NOT NULL COMMENT '比赛/作业ID',
    `problem_id`    BIGINT   NOT NULL COMMENT '题目ID',
    `display_order` INT      NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `score`         INT      NOT NULL DEFAULT 100 COMMENT '题目分值',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_problem_contest_problem` (`contest_id`, `problem_id`),
    KEY `idx_contest_problem_contest_order` (`contest_id`, `display_order`),
    CONSTRAINT `fk_contest_problem_contest_id` FOREIGN KEY (`contest_id`) REFERENCES `contest` (`id`),
    CONSTRAINT `fk_contest_problem_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='比赛题目关联表';

CREATE TABLE IF NOT EXISTS `contest_user`
(
    `id`          BIGINT   NOT NULL COMMENT '主键ID',
    `contest_id`  BIGINT   NOT NULL COMMENT '比赛/作业ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `total_score` INT               DEFAULT 0 COMMENT '总分',
    `ac_count`    INT               DEFAULT 0 COMMENT '通过题数',
    `rank_no`     INT               DEFAULT NULL COMMENT '排名',
    `join_time`   DATETIME          DEFAULT NULL COMMENT '参与时间',
    `finish_time` DATETIME          DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contest_user_contest_user` (`contest_id`, `user_id`),
    KEY `idx_contest_user_contest_rank` (`contest_id`, `rank_no`),
    KEY `idx_contest_user_user_id` (`user_id`),
    CONSTRAINT `fk_contest_user_contest_id` FOREIGN KEY (`contest_id`) REFERENCES `contest` (`id`),
    CONSTRAINT `fk_contest_user_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='比赛参与表';

CREATE TABLE IF NOT EXISTS `submission`
(
    `id`            BIGINT        NOT NULL COMMENT '提交ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `problem_id`    BIGINT        NOT NULL COMMENT '题目ID',
    `contest_id`    BIGINT                 DEFAULT NULL COMMENT '比赛/作业ID',
    `language`      VARCHAR(20)   NOT NULL COMMENT '编程语言',
    `source_code`   MEDIUMTEXT    NOT NULL COMMENT '源代码',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'WAITING' COMMENT '判题状态',
    `time_used`     INT                    DEFAULT NULL COMMENT '运行耗时',
    `memory_used`   INT                    DEFAULT NULL COMMENT '内存消耗',
    `score`         INT                    DEFAULT NULL COMMENT '得分',
    `error_message` TEXT                   DEFAULT NULL COMMENT '错误信息',
    `submit_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `judge_time`    DATETIME               DEFAULT NULL COMMENT '判题完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_submission_user_submit_time` (`user_id`, `submit_time`),
    KEY `idx_submission_problem_submit_time` (`problem_id`, `submit_time`),
    KEY `idx_submission_contest_user` (`contest_id`, `user_id`),
    CONSTRAINT `fk_submission_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
    CONSTRAINT `fk_submission_problem_id` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`),
    CONSTRAINT `fk_submission_contest_id` FOREIGN KEY (`contest_id`) REFERENCES `contest` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='提交记录表';

CREATE TABLE IF NOT EXISTS `submission_case`
(
    `id`            BIGINT      NOT NULL COMMENT '主键ID',
    `submission_id` BIGINT      NOT NULL COMMENT '提交ID',
    `case_no`       INT         NOT NULL COMMENT '测试点序号',
    `status`        VARCHAR(20) NOT NULL COMMENT '测试点状态',
    `time_used`     INT                  DEFAULT NULL COMMENT '运行耗时',
    `memory_used`   INT                  DEFAULT NULL COMMENT '内存消耗',
    `message`       VARCHAR(255)         DEFAULT NULL COMMENT '结果说明',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_submission_case_submission_case_no` (`submission_id`, `case_no`),
    CONSTRAINT `fk_submission_case_submission_id` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='提交测试点结果表';

CREATE TABLE IF NOT EXISTS `sys_config`
(
    `id`           BIGINT       NOT NULL COMMENT '配置ID',
    `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_name`  VARCHAR(100) NOT NULL COMMENT '配置名称',
    `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
    `value_type`   VARCHAR(20)  NOT NULL COMMENT '值类型',
    `category`     VARCHAR(50)  NOT NULL COMMENT '配置分类',
    `description`  VARCHAR(255)          DEFAULT NULL COMMENT '配置说明',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    `update_by`    BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_config_key` (`config_key`),
    KEY `idx_sys_config_category_status` (`category`, `status`),
    KEY `idx_sys_config_update_by` (`update_by`),
    CONSTRAINT `fk_sys_config_update_by` FOREIGN KEY (`update_by`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='系统配置表';

CREATE TABLE IF NOT EXISTS `judge_service`
(
    `id`                  BIGINT       NOT NULL COMMENT '服务ID',
    `service_name`        VARCHAR(100) NOT NULL COMMENT '服务名称',
    `service_host`        VARCHAR(100) NOT NULL COMMENT '服务主机',
    `service_port`        INT          NOT NULL COMMENT '服务端口',
    `service_status`      VARCHAR(20)  NOT NULL COMMENT '服务状态',
    `current_task_count`  INT          NOT NULL DEFAULT 0 COMMENT '当前任务数',
    `max_task_count`      INT          NOT NULL DEFAULT 1 COMMENT '最大并发数',
    `last_heartbeat_time` DATETIME     NOT NULL COMMENT '最后心跳时间',
    `version`             VARCHAR(50)           DEFAULT NULL COMMENT '服务版本',
    `remark`              VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_judge_service_status_heartbeat` (`service_status`, `last_heartbeat_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='判题服务监控表';

CREATE TABLE IF NOT EXISTS `login_log`
(
    `id`           BIGINT       NOT NULL COMMENT '日志ID',
    `user_id`      BIGINT                DEFAULT NULL COMMENT '用户ID',
    `username`     VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `login_type`   VARCHAR(20)  NOT NULL COMMENT '登录类型',
    `login_status` TINYINT      NOT NULL COMMENT '登录结果：1成功，0失败',
    `ip`           VARCHAR(64)           DEFAULT NULL COMMENT '登录IP',
    `user_agent`   VARCHAR(500)          DEFAULT NULL COMMENT '客户端信息',
    `fail_reason`  VARCHAR(255)          DEFAULT NULL COMMENT '失败原因',
    `login_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_login_log_username_time` (`username`, `login_time`),
    KEY `idx_login_log_user_id_time` (`user_id`, `login_time`),
    CONSTRAINT `fk_login_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='登录日志表';

-- 基础角色初始化
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`)
VALUES (1, 'STUDENT', '学生', '学生角色'),
       (2, 'TEACHER', '教师', '教师角色'),
       (3, 'ADMIN', '管理员', '管理员角色')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`),
                        `description` = VALUES(`description`);
