package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TPostQualityRule;
import com.fc.v2.model.custom.PostQualityVerdict;

/**
 * 服务质量达标率判据线 Service接口（rule-eval 形状：规则求值，无增删改）
 *
 * 全局口径（只此一套，企业自核与局里抽查同走这一路）：
 * 一、取线凭单子上的考核那一天（at），不凭今天。一条线在 [启用之日, 交棒之日) 内在场，
 *     交棒之日当天起旧线闭口、新线开口；交棒之日空着表示尚未交棒。已让位的旧线只供倒查
 *     来历，考核日落不进它的时段就轮不到它。
 * 二、同季并排可用的线有几条时，顺位（priority）数值大的先说话；顺位拉不开，认代号
 *     （ruleCode）靠后那条。此规矩季季照办，不因季度改口。
 * 三、定档：达标率与分界数一样高算高一档；分界数空着是不设这一格，跟着下一道实界往下落。
 *     达标率不在 0..100 这一段（负数、一百二、空着）不进求值，退回考核员重录。
 * 四、翻不出可用线的，注明无线可依，绝不就近挑一条充数，也不硬给结论。
 *
 * 求值一律为纯读：不出名录改动，网点挂靠企业已注销的也只出结论。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostQualityRuleService {

    /** 按主键查询规则（倒查来历用，不参与求值定夺） */
    TPostQualityRule selectTPostQualityRuleById(Long id);

    /**
     * 老口子·按代号取线：按 ruleCode 在 at 时刻生效的版本判档（1..4）。
     * 无生效版本 / 输入越界 / 参数缺失一律返回 0。签名保持不动，继续可调。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 一屏定夺（与按代号老口子同名的另一取法）：at 时刻可用线中请顺位最大者说话，
     * 取线与定档在同一回求值里走完，结论连同所依代号、线名、启用之日一并返回，
     * 钉进考核单以本返回为准。翻不出可用线 / 输入越界 / 参数缺失时 level 为 0，
     * note 注明缘由（无线可依 / 输入越界 / 参数缺失）。
     */
    PostQualityVerdict evaluate(BigDecimal input, Date at);

    /** 多规则求值：at 时刻可用规则中优先级最高者的档位；无可用规则 / 输入越界返回 0 */
    int evaluateTop(BigDecimal input, Date at);

    /** at 时刻可用规则（顺位降序、代号降序、启用日降序，全序可复现） */
    List<TPostQualityRule> listAvailable(Date at);

    /** 单条规则在 at 时刻是否可用（批量导入/报表等旁路复用，口径须与定位/列表一致） */
    boolean usable(Long id, Date at);

    /** at 时刻可用规则条数（列表页角标 / 看板） */
    int countAvailable(Date at);
}
