package com.fc.v2.model.custom;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 达标率定档求值入参（一条走业务服务层，页面不得自行取线）。
 *
 * <p>取哪条线凭 {@link #assessedAt}（考核那一天），不凭今天；
 * 去年秋天的考核今年开春补结论，也要把当时在场的那条线请出来。
 *
 * @author fuce
 * @date 2026-10-02
 */
public class QualityGradeQuery {

    /** 所管服务类别 1同城时限 2异地时限 3投递频次 4时限赔偿（各管各的） */
    private String serviceType;

    /** 当季达标率（0..100 的百分数；负数 / 大于 100 / 空着 退回考核员重录） */
    private BigDecimal rate;

    /** 考核那一天（取线凭它，不凭今天） */
    private Date assessedAt;

    public QualityGradeQuery() {
    }

    public QualityGradeQuery(String serviceType, BigDecimal rate, Date assessedAt) {
        this.serviceType = serviceType;
        this.rate = rate;
        this.assessedAt = assessedAt;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Date getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Date assessedAt) {
        this.assessedAt = assessedAt;
    }
}
