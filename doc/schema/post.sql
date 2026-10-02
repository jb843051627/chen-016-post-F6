-- post 邮政业市场监管与快递末端服务监督 -- schema (chen-016)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_016

CREATE TABLE IF NOT EXISTS t_post_company_arch (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '经营企业在册底档字号',
  site_name varchar(128) DEFAULT NULL COMMENT '企业全称',
  site_type varchar(32) DEFAULT NULL COMMENT '许可类别',
  road_name varchar(128) DEFAULT NULL COMMENT '管辖邮政管理局(省—市—县)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单卡日均件量低界(件/日)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界(件/日)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界(件/日)',
  status int DEFAULT NULL COMMENT '底档情形 0在册 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='经营企业在册底档';

CREATE TABLE IF NOT EXISTS t_post_due_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '告知事项号',
  due_at datetime DEFAULT NULL COMMENT '该动手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '提前几日开口告知',
  content varchar(255) DEFAULT NULL COMMENT '事由与被告知企业记要',
  status int DEFAULT NULL COMMENT '条目情形 0未开口 1已送达 2送不下去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='经营许可效期与备案年度核验到期告知事项';

CREATE TABLE IF NOT EXISTS t_post_outlet_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '末端网点备案建档卡号',
  node_no int DEFAULT NULL COMMENT '本卡版本序',
  site_id int DEFAULT NULL COMMENT '所属企业在册底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属企业底档字号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡网点日均处理件数(件/日)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本卡报备营业面积(平方米)',
  content varchar(255) DEFAULT NULL COMMENT '核验串（随版本序走）',
  grade_level int DEFAULT NULL COMMENT '日均件量折到的服务序位',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已冻住',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快递末端网点备案建档卡';

CREATE TABLE IF NOT EXISTS t_post_punish_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '查处核签单号',
  node_no int DEFAULT NULL COMMENT '当前所在开次 0..3',
  sign_mode int DEFAULT NULL COMMENT '同关并签办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本开应签人数',
  sign_count int DEFAULT NULL COMMENT '本开已签人数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违法行为查处逐级核签单';

CREATE TABLE IF NOT EXISTS t_post_quality_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '服务质量判据代号（省里给的或局里加的，全局独占一个）',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  service_type varchar(32) DEFAULT NULL COMMENT '所管服务类别 1同城时限 2异地时限 3投递频次 4时限赔偿',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '低界达标率(百分比)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界达标率(百分比)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界达标率(空着=不设这一格，随中界往下落)',
  eff_start datetime DEFAULT NULL COMMENT '启用之日(含)',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含，空着=尚无接棒线)',
  priority int DEFAULT NULL COMMENT '让线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0现行 1已让位(只供倒查来历，不进现行求值)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quality_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务质量达标率判据线（一册并线：一条线一行，代号独占；换版不删旧）';

CREATE TABLE IF NOT EXISTS t_post_rectify_card (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '责令整改事务卡号',
  stage int DEFAULT NULL COMMENT '当前段次 0..3（下达/整改中/复查/销案）',
  status int DEFAULT NULL COMMENT '卡的落点 0未起 1在办 2已销案',
  content varchar(255) DEFAULT NULL COMMENT '整改记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='责令整改事务卡';

CREATE TABLE IF NOT EXISTS t_post_report_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '月度业务报表报送批次号',
  row_no int DEFAULT NULL COMMENT '原报表行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被挑出的前置项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行报送业务量(万件)',
  status int DEFAULT NULL COMMENT '行落地情形 0待核 1已收 2挂起',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度业务报表核收行';

-- 初始底档数据（在册、已注销两类各一条）
-- t_post_company_arch 两条种子底档：id=0 在册、id=1 已注销。
-- 单卡日均件数三线上限取 30/90/240 件（等于上限算高一序位）。
INSERT IGNORE INTO t_post_company_arch (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'YZ00', '青屏市在册企业底档（青屏速递有限公司，在册）', '省内经营', '青屏省—青屏市—青屏县', 30.00, 90.00, 240.00, 0, 0, 'seed', NOW()),
       (1, 'YZ01', '青屏市在册企业底档（云驿快递服务有限公司，已注销）', '省内经营', '青屏省—青屏市—青屏县', 30.00, 90.00, 240.00, 1, 0, 'seed', NOW());

-- =====================================================================
-- 迁移段（对已建库幂等）：并线一册所需列与代号唯一约束
-- 列名与 TPostQualityRule 实体 @TableField 逐列对齐。
-- =====================================================================

SET @db := DATABASE();

SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_post_quality_rule' AND COLUMN_NAME = 'service_type') = 0,
  'ALTER TABLE t_post_quality_rule ADD COLUMN service_type varchar(32) DEFAULT NULL COMMENT ''所管服务类别 1同城时限 2异地时限 3投递频次 4时限赔偿'' AFTER rule_name',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 交棒日补义：空着表示尚无接棒线；旧线交棒日与新线启用之日为同一日（含起不含止，全局一套口径）
SET @sql := (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_post_quality_rule' AND INDEX_NAME = 'uk_quality_rule_code') = 0,
  'ALTER TABLE t_post_quality_rule ADD UNIQUE KEY uk_quality_rule_code (rule_code)',
  'SELECT 1'));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =====================================================================
-- 种子线（仅空册时落）：演示同城时限的年初一版、入秋换版，以及同季并排两条的并线取舍。
-- 达标率越高越好；等于分界算高一档。高界空着=不设该格，随中界往下落。
--   TC-TC-OLD 年初线（已让位，交棒日 2025-10-01 不含）
--   TC-TC-NEW 入秋换版线（现行，2025-10-01 起，无交棒日）
--   TC-TC-AID 同季并排的另一条（现行，顺位低于 NEW，顺位决胜时不说话）
--   TC-PR-01  早年纸质簿登记：高界留空
-- =====================================================================
INSERT IGNORE INTO t_post_quality_rule
  (id, rule_code, rule_name, service_type, th1_max, th2_max, th3_max, eff_start, eff_end, priority, status, del_flag, create_by, create_time)
VALUES
  (1001, 'TC-TC-OLD', '同城投递时限达标率判据（年初版）', '1', 60.00, 80.00, 90.00,
        '2025-01-01 00:00:00', '2025-10-01 00:00:00', 10, 1, 0, 'seed', NOW()),
  (1002, 'TC-TC-NEW', '同城投递时限达标率判据（入秋版）', '1', 65.00, 85.00, 95.00,
        '2025-10-01 00:00:00', NULL, 20, 0, 0, 'seed', NOW()),
  (1003, 'TC-TC-AID', '同城投递时限达标率辅助线（同季并排）', '1', 70.00, 88.00, 96.00,
        '2025-10-01 00:00:00', NULL, 15, 0, 0, 'seed', NOW()),
  (1004, 'TC-PR-01', '纸质簿登记线（高界留空）', '4', 70.00, 90.00, NULL,
        '2025-01-01 00:00:00', NULL, 10, 0, 0, 'seed', NOW());

