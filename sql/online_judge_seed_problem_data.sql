-- 在线编程评测系统 - 题库初始化数据脚本
-- 用途：插入演示题目数据（分类 + 题目 + 测试用例）
-- 特点：幂等执行，重复跑不会产生重复脏数据

USE `online_judge`;

START TRANSACTION;

-- 选择题目创建人：固定使用 admin
SET @creator_id := (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1);

-- 如果 admin 不存在，本脚本不会插入题目数据（避免外键不合规）

-- =========================
-- 1) 题目分类
-- =========================
INSERT INTO problem_category (id, category_name, parent_id, sort_no, status)
VALUES
    (900000000000000101, '入门', NULL, 1, 1),
    (900000000000000102, '数组', NULL, 2, 1),
    (900000000000000103, '字符串', NULL, 3, 1),
    (900000000000000104, '动态规划', NULL, 4, 1)
ON DUPLICATE KEY UPDATE
    sort_no = VALUES(sort_no),
    status = VALUES(status),
    update_time = CURRENT_TIMESTAMP;

SET @cat_intro := (SELECT id FROM problem_category WHERE category_name = '入门' LIMIT 1);
SET @cat_array := (SELECT id FROM problem_category WHERE category_name = '数组' LIMIT 1);
SET @cat_string := (SELECT id FROM problem_category WHERE category_name = '字符串' LIMIT 1);
SET @cat_dp := (SELECT id FROM problem_category WHERE category_name = '动态规划' LIMIT 1);

-- =========================
-- 2) 题目数据
-- =========================

-- 题目1：A+B
INSERT INTO problem (
    id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
    difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id
)
SELECT
    900000000000100001,
    'A+B Problem（入门）',
    @cat_intro,
    '给定两个整数 a 和 b，输出它们的和。',
    '输入一行，包含两个整数 a, b。',
    '输出一个整数，表示 a+b。',
    '1 2',
    '3',
    '注意负数与大整数范围。',
    1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual
WHERE @creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM problem WHERE title = 'A+B Problem（入门）');

-- 题目2：数组最大值
INSERT INTO problem (
    id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
    difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id
)
SELECT
    900000000000100002,
    '数组最大值',
    @cat_array,
    '给定长度为 n 的整数数组，输出其中的最大值。',
    '第一行输入 n。第二行输入 n 个整数。',
    '输出数组中的最大值。',
    '5\n1 9 3 7 2',
    '9',
    'n 至少为 1。',
    1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual
WHERE @creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '数组最大值');

-- 题目3：判断回文串
INSERT INTO problem (
    id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
    difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id
)
SELECT
    900000000000100003,
    '判断回文串',
    @cat_string,
    '输入一个仅包含小写字母的字符串，判断它是否为回文串。是输出 YES，否则输出 NO。',
    '输入一行字符串 s。',
    '输出 YES 或 NO。',
    'level',
    'YES',
    '回文定义：正读反读都相同。',
    1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual
WHERE @creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '判断回文串');

-- 题目4：爬楼梯
INSERT INTO problem (
    id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
    difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id
)
SELECT
    900000000000100004,
    '爬楼梯',
    @cat_dp,
    '你正在爬楼梯。每次你可以爬 1 或 2 个台阶。给定 n，求到达楼顶的不同方法数。',
    '输入一个正整数 n。',
    '输出一个整数，表示方案数。',
    '4',
    '5',
    '这是经典斐波那契模型。',
    2, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual
WHERE @creator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '爬楼梯');

-- =========================
-- 3) 测试用例数据
-- =========================
SET @p1 := (SELECT id FROM problem WHERE title = 'A+B Problem（入门）' LIMIT 1);
SET @p2 := (SELECT id FROM problem WHERE title = '数组最大值' LIMIT 1);
SET @p3 := (SELECT id FROM problem WHERE title = '判断回文串' LIMIT 1);
SET @p4 := (SELECT id FROM problem WHERE title = '爬楼梯' LIMIT 1);

-- A+B
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200001, @p1, 1, '1 2', '3', 0, 1 FROM dual WHERE @p1 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200002, @p1, 2, '-5 2', '-3', 0, 0 FROM dual WHERE @p1 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);

-- 数组最大值
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200003, @p2, 1, '5\n1 9 3 7 2', '9', 0, 1 FROM dual WHERE @p2 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200004, @p2, 2, '4\n-1 -9 -3 -7', '-1', 0, 0 FROM dual WHERE @p2 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);

-- 判断回文串
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200005, @p3, 1, 'level', 'YES', 0, 1 FROM dual WHERE @p3 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200006, @p3, 2, 'hello', 'NO', 0, 0 FROM dual WHERE @p3 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);

-- 爬楼梯
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200007, @p4, 1, '4', '5', 0, 1 FROM dual WHERE @p4 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000200008, @p4, 2, '10', '89', 0, 0 FROM dual WHERE @p4 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data = VALUES(input_data), output_data = VALUES(output_data), score = VALUES(score), is_sample = VALUES(is_sample);

COMMIT;

-- 检查结果
SELECT id, title, difficulty, status, creator_id
FROM problem
ORDER BY create_time DESC
LIMIT 20;

SELECT IF(@creator_id IS NULL, 'WARNING: admin用户不存在，未插入题目数据', CONCAT('OK: admin用户ID=', @creator_id)) AS seed_status;
