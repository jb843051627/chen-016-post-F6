package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TPostQualityRule;
import com.fc.v2.model.custom.QualityGradeQuery;
import com.fc.v2.model.custom.QualityGradeResult;

/**
 * 服务质量达标率判据线 Service接口（rule-eval 形状：规则求值，无增删改）。
 *
 * <p>并线一册，取线只走这一处：从一项当季达标率到"落第几档、依哪条线"，
 * 只在 {@link #evaluate(QualityGradeQuery)} 一条求值里走完，
 * 钉进考核单的代号与启用之日以它的返回为准，页面不得自行取线再摆结论。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostQualityRuleService {

    /** 所管服务类别：同城时限 */
    String TYPE_CITY = "1";
    /** 所管服务类别：异地时限 */
    String TYPE_REMOTE = "2";
    /** 所管服务类别：投递频次 */
    String TYPE_FREQUENCY = "3";
    /** 所管服务类别：时限赔偿 */
    String TYPE_COMPENSATION = "4";

    /** 按主键查询规则 */
    TPostQualityRule selectTPostQualityRuleById(Long id);

    /**
     * 一屏定夺的唯一求值：取线与定档同一回给出，中间不许夹第二回计算。
     *
     * <p>取线凭 query.assessedAt（考核那一天，不凭今天）：在该日在场
     * （启用之日 ≤ 考核日 &lt; 交棒之日）的本类别现行线里，
     * 请顺位最大的说话；顺位拉不开认代号靠后的。结论连同所依代号、启用之日一并返回。
     * <ul>
     *   <li>达标率为空 / 负数 / 大于 100：不进求值，{@link QualityGradeResult.Outcome#INVALID} 退回重录；</li>
     *   <li>该考核日翻不出可用线：{@link QualityGradeResult.Outcome#NO_RULE}，注明无线可依，不硬给结论；</li>
     *   <li>否则 {@link QualityGradeResult.Outcome#GRADED}，档位 1..4，等于分界算高一档。</li>
     * </ul>
     */
    QualityGradeResult evaluate(QualityGradeQuery query);

    /**
     * 老口子（签名不变）：按 ruleCode 请在 at 时刻在场的那条版本判档（1..4）。
     * 无在场版本 / 输入越界或缺失（不在 0..100）一律返回 0。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 另一种取法（同名另立，不改老签名）：在本类别 at 时刻可用的线里取顺位最大者判档。
     * 无可用线 / 输入越界一律返回 0。
     */
    int evaluateTop(String serviceType, BigDecimal input, Date at);

    /** 遗留（不分类别）：at 时刻全部可用线中顺位最大者的档位；新用处请走 {@link #evaluateTop(String, BigDecimal, Date)} */
    int evaluateTop(BigDecimal input, Date at);

    /** at 时刻可用规则（顺位降序、代号降序） */
    List<TPostQualityRule> listAvailable(Date at);

    /** 本类别 at 时刻可用规则（顺位降序、代号降序） */
    List<TPostQualityRule> listAvailable(String serviceType, Date at);

    /** 单条线在 at 时刻是否可用（批量导入/报表等旁路复用，口径须与定位/列表一致） */
    boolean usable(Long id, Date at);

    /** at 时刻可用规则条数（列表页角标 / 看板） */
    int countAvailable(Date at);

    /** 本类别 at 时刻可用规则条数 */
    int countAvailable(String serviceType, Date at);
}
