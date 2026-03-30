-- 在线编程评测系统 - 经典题目扩展数据
-- 说明：在已有基础题目上补充一批经典题，幂等执行

USE `online_judge`;

START TRANSACTION;

SET @creator_id := (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1);

-- 补充分类（不存在则插入）
INSERT INTO problem_category (id, category_name, parent_id, sort_no, status)
VALUES
    (900000000000000105, '双指针', NULL, 5, 1),
    (900000000000000106, '二分查找', NULL, 6, 1),
    (900000000000000107, '图论', NULL, 7, 1),
    (900000000000000108, '贪心', NULL, 8, 1),
    (900000000000000109, '回溯', NULL, 9, 1)
ON DUPLICATE KEY UPDATE
    sort_no = VALUES(sort_no),
    status = VALUES(status),
    update_time = CURRENT_TIMESTAMP;

SET @cat_array := (SELECT id FROM problem_category WHERE category_name = '数组' LIMIT 1);
SET @cat_string := (SELECT id FROM problem_category WHERE category_name = '字符串' LIMIT 1);
SET @cat_dp := (SELECT id FROM problem_category WHERE category_name = '动态规划' LIMIT 1);
SET @cat_tp := (SELECT id FROM problem_category WHERE category_name = '双指针' LIMIT 1);
SET @cat_bs := (SELECT id FROM problem_category WHERE category_name = '二分查找' LIMIT 1);
SET @cat_graph := (SELECT id FROM problem_category WHERE category_name = '图论' LIMIT 1);
SET @cat_greedy := (SELECT id FROM problem_category WHERE category_name = '贪心' LIMIT 1);
SET @cat_backtrack := (SELECT id FROM problem_category WHERE category_name = '回溯' LIMIT 1);

-- 1. 两数之和
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100101, '两数之和',
       @cat_array,
       '给定数组和目标值 target，输出两个下标 i,j（i<j）使 nums[i]+nums[j]=target。保证有解。',
       '第一行 n target；第二行 n 个整数。',
       '输出两个下标 i j（空格分隔）。',
       '4 9\n2 7 11 15',
       '0 1',
       '返回任意一组可行解即可。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '两数之和');

-- 2. 有效括号
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100102, '有效括号',
       @cat_string,
       '给定仅含 ()[]{} 的字符串，判断括号是否有效匹配。',
       '输入一行字符串 s。',
       '有效输出 YES，否则输出 NO。',
       '()[]{}',
       'YES',
       '建议使用栈。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '有效括号');

-- 3. 二分查找
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100103, '二分查找',
       @cat_bs,
       '在升序数组中查找 target，存在返回下标，不存在返回 -1。',
       '第一行 n target；第二行 n 个升序整数。',
       '输出下标或 -1。',
       '6 9\n1 3 5 7 9 11',
       '4',
       '时间复杂度 O(log n)。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '二分查找');

-- 4. 合并两个有序数组
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100104, '合并两个有序数组',
       @cat_tp,
       '给定两个升序数组，输出合并后的升序数组。',
       '第一行 n m；第二行 n 个数；第三行 m 个数。',
       '输出 n+m 个升序整数。',
       '3 3\n1 2 4\n1 3 4',
       '1 1 2 3 4 4',
       '双指针线性合并。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '合并两个有序数组');

-- 5. 删除有序数组中的重复项
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100105, '删除有序数组中的重复项',
       @cat_tp,
       '给定升序数组，原地删除重复元素后返回新长度。',
       '第一行 n；第二行 n 个升序整数。',
       '输出去重后的长度。',
       '10\n0 0 1 1 1 2 2 3 3 4',
       '5',
       '经典快慢指针。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '删除有序数组中的重复项');

-- 6. 移动零
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100106, '移动零',
       @cat_tp,
       '将数组中所有 0 移动到末尾，保持非零元素相对顺序。',
       '第一行 n；第二行 n 个整数。',
       '输出变换后的数组。',
       '5\n0 1 0 3 12',
       '1 3 12 0 0',
       '双指针稳定移动。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '移动零');

-- 7. 最长递增子序列
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100107, '最长递增子序列',
       @cat_dp,
       '给定数组，求最长严格递增子序列长度。',
       '第一行 n；第二行 n 个整数。',
       '输出LIS长度。',
       '8\n10 9 2 5 3 7 101 18',
       '4',
       '可用 O(n log n) 解法。',
       2, 1200, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '最长递增子序列');

-- 8. 零钱兑换
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100108, '零钱兑换',
       @cat_dp,
       '给定硬币面值数组和金额 amount，求凑出金额的最少硬币数，不可达输出 -1。',
       '第一行 n amount；第二行 n 个面值。',
       '输出最少硬币数或 -1。',
       '3 11\n1 2 5',
       '3',
       '完全背包模型。',
       2, 1500, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '零钱兑换');

-- 9. 岛屿数量
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100109, '岛屿数量',
       @cat_graph,
       '给定由 0/1 组成的网格，统计岛屿数量（上下左右连通）。',
       '第一行 r c；接下来 r 行，每行 c 个字符（0或1）。',
       '输出岛屿数量。',
       '4 5\n11000\n11000\n00100\n00011',
       '3',
       'DFS/BFS/并查集均可。',
       2, 1500, 256, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '岛屿数量');

-- 10. 课程表
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100110, '课程表',
       @cat_graph,
       '给定课程总数和先修关系，判断是否可以完成所有课程（判断有向图是否有环）。',
       '第一行 n m；接下来 m 行 a b，表示学 a 前要先学 b。',
       '可以完成输出 YES，否则输出 NO。',
       '2 1\n1 0',
       'YES',
       '拓扑排序判环。',
       2, 1500, 256, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '课程表');

-- 11. 全排列
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100111, '全排列',
       @cat_backtrack,
       '给定 n 个互不相同的整数，输出所有排列（按字典序即可）。',
       '第一行 n；第二行 n 个整数。',
       '第一行输出排列总数，后续每行一个排列。',
       '3\n1 2 3',
       '6',
       '可用回溯。',
       2, 2000, 256, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '全排列');

-- 12. 买卖股票的最佳时机
INSERT INTO problem (id, title, category_id, content, input_desc, output_desc, sample_input, sample_output, hint,
                     difficulty, time_limit, memory_limit, judge_mode, submit_count, accept_count, status, creator_id)
SELECT 900000000000100112, '买卖股票的最佳时机',
       @cat_greedy,
       '给定每天股价，只允许买入一次卖出一次，求最大利润。',
       '第一行 n；第二行 n 个整数表示价格。',
       '输出最大利润。',
       '6\n7 1 5 3 6 4',
       '5',
       '维护历史最小值。',
       1, 1000, 128, 'ACM', 0, 0, 1, @creator_id
FROM dual WHERE @creator_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM problem WHERE title = '买卖股票的最佳时机');

-- -------------------------
-- 测试用例（每题2条）
-- -------------------------
SET @p101 := (SELECT id FROM problem WHERE title = '两数之和' LIMIT 1);
SET @p102 := (SELECT id FROM problem WHERE title = '有效括号' LIMIT 1);
SET @p103 := (SELECT id FROM problem WHERE title = '二分查找' LIMIT 1);
SET @p104 := (SELECT id FROM problem WHERE title = '合并两个有序数组' LIMIT 1);
SET @p105 := (SELECT id FROM problem WHERE title = '删除有序数组中的重复项' LIMIT 1);
SET @p106 := (SELECT id FROM problem WHERE title = '移动零' LIMIT 1);
SET @p107 := (SELECT id FROM problem WHERE title = '最长递增子序列' LIMIT 1);
SET @p108 := (SELECT id FROM problem WHERE title = '零钱兑换' LIMIT 1);
SET @p109 := (SELECT id FROM problem WHERE title = '岛屿数量' LIMIT 1);
SET @p110 := (SELECT id FROM problem WHERE title = '课程表' LIMIT 1);
SET @p111 := (SELECT id FROM problem WHERE title = '全排列' LIMIT 1);
SET @p112 := (SELECT id FROM problem WHERE title = '买卖股票的最佳时机' LIMIT 1);

-- 1
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000210001, @p101, 1, '4 9\n2 7 11 15', '0 1', 0, 1 FROM dual WHERE @p101 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case (id, problem_id, case_no, input_data, output_data, score, is_sample)
SELECT 900000000000210002, @p101, 2, '3 6\n3 2 4', '1 2', 0, 0 FROM dual WHERE @p101 IS NOT NULL
ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 2
INSERT INTO problem_case VALUES (900000000000210003, @p102, 1, '()[]{}', 'YES', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210004, @p102, 2, '([)]', 'NO', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 3
INSERT INTO problem_case VALUES (900000000000210005, @p103, 1, '6 9\n1 3 5 7 9 11', '4', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210006, @p103, 2, '5 2\n1 3 5 7 9', '-1', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 4
INSERT INTO problem_case VALUES (900000000000210007, @p104, 1, '3 3\n1 2 4\n1 3 4', '1 1 2 3 4 4', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210008, @p104, 2, '2 2\n2 6\n1 3', '1 2 3 6', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 5
INSERT INTO problem_case VALUES (900000000000210009, @p105, 1, '10\n0 0 1 1 1 2 2 3 3 4', '5', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210010, @p105, 2, '5\n1 1 1 1 1', '1', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 6
INSERT INTO problem_case VALUES (900000000000210011, @p106, 1, '5\n0 1 0 3 12', '1 3 12 0 0', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210012, @p106, 2, '3\n0 0 1', '1 0 0', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 7
INSERT INTO problem_case VALUES (900000000000210013, @p107, 1, '8\n10 9 2 5 3 7 101 18', '4', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210014, @p107, 2, '6\n0 1 0 3 2 3', '4', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 8
INSERT INTO problem_case VALUES (900000000000210015, @p108, 1, '3 11\n1 2 5', '3', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210016, @p108, 2, '1 3\n2', '-1', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 9
INSERT INTO problem_case VALUES (900000000000210017, @p109, 1, '4 5\n11000\n11000\n00100\n00011', '3', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210018, @p109, 2, '1 1\n0', '0', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 10
INSERT INTO problem_case VALUES (900000000000210019, @p110, 1, '2 1\n1 0', 'YES', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210020, @p110, 2, '2 2\n1 0\n0 1', 'NO', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 11
INSERT INTO problem_case VALUES (900000000000210021, @p111, 1, '3\n1 2 3', '6', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210022, @p111, 2, '1\n9', '1', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

-- 12
INSERT INTO problem_case VALUES (900000000000210023, @p112, 1, '6\n7 1 5 3 6 4', '5', 0, 1, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);
INSERT INTO problem_case VALUES (900000000000210024, @p112, 2, '5\n7 6 4 3 1', '0', 0, 0, NOW()) ON DUPLICATE KEY UPDATE input_data=VALUES(input_data), output_data=VALUES(output_data), is_sample=VALUES(is_sample);

COMMIT;

SELECT COUNT(*) AS problem_count FROM problem;
SELECT COUNT(*) AS case_count FROM problem_case;
SELECT IF(@creator_id IS NULL, 'WARNING: admin用户不存在，未插入扩展题目数据', CONCAT('OK: admin用户ID=', @creator_id)) AS seed_status;
