package com.fc.v2.model.custom;

import java.util.Date;

import com.fc.v2.model.auto.TPostQualityRule;

/**
 * 达标率定档求值结果——取线与定档由同一回求值给出，中间不许再夹第二回计算。
 *
 * <p>三种落点：
 * <ul>
 *   <li>{@link Outcome#GRADED}：已定档，{@link #gradeLevel} 为 1..4，
 *       所依代号 {@link #ruleCode} 与该界启用之日 {@link #effStart} 一并给出，
 *       钉进考核单后不再改口；</li>
 *   <li>{@link Outcome#NO_RULE}：该考核日翻不出可用线，注明无线可依，
 *       不就近挑一条充数，更不许硬给结论；</li>
 *   <li>{@link Outcome#INVALID}：填入的达标率不在 0..100（负数 / 大于 100 / 空着），
 *       不进求值，退回考核员重录。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-10-02
 */
public class QualityGradeResult {

    /** 求值落点 */
    public enum Outcome {
        /** 已定档（gradeLevel 1..4） */
        GRADED,
        /** 无线可依：该考核日没有在场的现行线 */
        NO_RULE,
        /** 输入越界或缺失：退回考核员重录 */
        INVALID
    }

    private final Outcome outcome;
    private final int gradeLevel;
    private final String ruleCode;
    private final String ruleName;
    private final String serviceType;
    private final Date effStart;

    private QualityGradeResult(Outcome outcome, int gradeLevel, TPostQualityRule rule) {
        this.outcome = outcome;
        this.gradeLevel = gradeLevel;
        if (rule == null) {
            this.ruleCode = null;
            this.ruleName = null;
            this.serviceType = null;
            this.effStart = null;
        } else {
            this.ruleCode = rule.getRuleCode();
            this.ruleName = rule.getRuleName();
            this.serviceType = rule.getServiceType();
            this.effStart = rule.getEffStart();
        }
    }

    /** 已定档：结论连同所依代号、启用之日一并给出 */
    public static QualityGradeResult graded(int gradeLevel, TPostQualityRule rule) {
        return new QualityGradeResult(Outcome.GRADED, gradeLevel, rule);
    }

    /** 无线可依：不硬给结论 */
    public static QualityGradeResult noRule() {
        return new QualityGradeResult(Outcome.NO_RULE, 0, null);
    }

    /** 输入越界/缺失：退回重录 */
    public static QualityGradeResult invalid() {
        return new QualityGradeResult(Outcome.INVALID, 0, null);
    }

    public boolean isGraded() {
        return outcome == Outcome.GRADED;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public int getGradeLevel() {
        return gradeLevel;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public Date getEffStart() {
        return effStart;
    }
}
