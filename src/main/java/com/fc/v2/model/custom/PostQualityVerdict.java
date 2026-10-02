package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Date;

/**
 * 服务质量达标率求值结论（考核单钉案载体）。
 *
 * 取线（依哪一条线）与定档（落第几档）由业务服务层同一回求值给出，
 * 本对象把两头的数一并带回：结论档位、所依代号、那条界启用哪天。
 * 钉进考核单以后不再改口；整改复验重算另起一份结论，前后各留各的代号与日期。
 *
 * level 取值：1..4 为档位；0 为无结论（note 注明缘由：无线可依 / 输入越界 / 参数缺失），
 * 无结论时 ruleCode、effStart 一律为空，绝不就近挑一条充数。
 *
 * @author fuce
 * @date 2026-10-02
 */
public class PostQualityVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 附注：翻不出可用线 */
    public static final String NOTE_NO_RULE = "无线可依";
    /** 附注：达标率不在 0..100 这一段，退回考核员重录 */
    public static final String NOTE_BAD_INPUT = "输入越界";
    /** 附注：考核那一天等参数缺失 */
    public static final String NOTE_MISSING_ARG = "参数缺失";

    /** 结论档位 1..4；0=无结论 */
    private int level;

    /** 所依判据线代号（无结论时为空） */
    private String ruleCode;

    /** 所依判据线线名（无结论时为空） */
    private String ruleName;

    /** 所依那条界启用哪天（无结论时为空） */
    private Date effStart;

    /** 无结论缘由（NOTE_NO_RULE / NOTE_BAD_INPUT / NOTE_MISSING_ARG）；有结论时为空 */
    private String note;

    public PostQualityVerdict() {
        super();
    }

    public PostQualityVerdict(int level, String ruleCode, String ruleName, Date effStart, String note) {
        super();
        this.level = level;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.effStart = effStart;
        this.note = note;
    }

    /** 有结论的一档（level 1..4，代号与启用日齐备） */
    public static PostQualityVerdict of(int level, String ruleCode, String ruleName, Date effStart) {
        return new PostQualityVerdict(level, ruleCode, ruleName, effStart, null);
    }

    /** 无结论的一趟（level 0，只带缘由，不带线） */
    public static PostQualityVerdict reject(String note) {
        return new PostQualityVerdict(0, null, null, null, note);
    }

    /** 是否已有结论（level 1..4） */
    public boolean isConclusive() {
        return this.level > 0;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
