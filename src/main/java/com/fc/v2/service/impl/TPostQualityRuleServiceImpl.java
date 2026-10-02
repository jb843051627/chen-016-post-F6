package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostQualityRuleMapper;
import com.fc.v2.model.auto.TPostQualityRule;
import com.fc.v2.model.custom.PostQualityVerdict;
import com.fc.v2.service.ITPostQualityRuleService;

/**
 * 服务质量达标率判据线 Service业务层处理（rule-eval 形状）
 *
 * 从当季达标率到落第几档、依哪一条线，只在本类这一回求值里走完：
 * 界取哪一版、并线取哪一条、缺位怎么兜，都现算现给；页面不得自行请线再摆结论。
 * 全程纯读，不改动任何名录。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostQualityRuleServiceImpl implements ITPostQualityRuleService {

    /** 达标率合法区间 [0, 100]；区间外（负数、一百二、空着）不进求值 */
    private static final BigDecimal RATE_MIN = BigDecimal.ZERO;
    private static final BigDecimal RATE_MAX = new BigDecimal("100");

    /**
     * 定夺规矩（季季同一套，不许这季认前一条下季改认后一条）：
     * 顺位数值大的先说话；顺位拉不开认代号靠后那条；再并列按启用日降序兜底，凑成全序。
     */
    private static final Comparator<TPostQualityRule> PRIORITY_THEN_CODE_DESC = new Comparator<TPostQualityRule>() {
        @Override
        public int compare(TPostQualityRule a, TPostQualityRule b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            int byCode = cb.compareTo(ca);
            if (byCode != 0) {
                return byCode;
            }
            Date sa = a.getEffStart();
            Date sb = b.getEffStart();
            if (sa == null || sb == null) {
                return sa == null ? (sb == null ? 0 : 1) : -1;
            }
            return sb.compareTo(sa);
        }
    };

    @javax.annotation.Resource
    private TPostQualityRuleMapper postQualityRuleMapper;

    /**
     * 交棒口径全局只此一套：线在 [启用之日, 交棒之日) 内在场——旧线管到交棒前一天为止，
     * 交棒当天起新线开口；交棒之日空着表示尚未交棒。启用之日空着视为未启用，不在场。
     * 比较在内存中按瞬时走（不拿 java.util.Date 绑 SQL 参数），从根上避开
     * JDBC serverTimezone 与本机时区不对称导致的边界错档。
     */
    private static boolean inWindow(TPostQualityRule r, Date at) {
        if (r.getEffStart() == null || r.getEffStart().after(at)) {
            return false;
        }
        return r.getEffEnd() == null || r.getEffEnd().after(at);
    }

    /** 填进来的数不在零到一百这一段里的（负数、一百二、空着），不进求值 */
    private static boolean outOfRange(BigDecimal input) {
        return input == null || input.compareTo(RATE_MIN) < 0 || input.compareTo(RATE_MAX) > 0;
    }

    @Override
    public TPostQualityRule selectTPostQualityRuleById(Long id) {
        return this.postQualityRuleMapper.selectById(id);
    }

    @Override
    public List<TPostQualityRule> listAvailable(Date at) {
        List<TPostQualityRule> avail = new java.util.ArrayList<TPostQualityRule>();
        if (at == null) {
            return avail;
        }
        List<TPostQualityRule> all = this.postQualityRuleMapper
                .selectList(new QueryWrapper<TPostQualityRule>().eq("del_flag", 0));
        for (TPostQualityRule r : all) {
            if (inWindow(r, at)) {
                avail.add(r);
            }
        }
        avail.sort(PRIORITY_THEN_CODE_DESC);
        return avail;
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || at == null || outOfRange(input)) {
            return 0;
        }
        List<TPostQualityRule> hit = this.postQualityRuleMapper.selectList(new QueryWrapper<TPostQualityRule>()
                .eq("del_flag", 0).eq("rule_code", ruleCode));
        TPostQualityRule best = null;
        for (TPostQualityRule r : hit) {
            if (inWindow(r, at) && (best == null || PRIORITY_THEN_CODE_DESC.compare(r, best) < 0)) {
                best = r;
            }
        }
        if (best == null) {
            return 0;
        }
        return levelOf(best, input);
    }

    @Override
    public PostQualityVerdict evaluate(BigDecimal input, Date at) {
        if (at == null) {
            return PostQualityVerdict.reject(PostQualityVerdict.NOTE_MISSING_ARG);
        }
        if (outOfRange(input)) {
            return PostQualityVerdict.reject(PostQualityVerdict.NOTE_BAD_INPUT);
        }
        List<TPostQualityRule> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return PostQualityVerdict.reject(PostQualityVerdict.NOTE_NO_RULE);
        }
        TPostQualityRule line = avail.get(0);
        return PostQualityVerdict.of(levelOf(line, input), line.getRuleCode(), line.getRuleName(),
                line.getEffStart());
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        return evaluate(input, at).getLevel();
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        TPostQualityRule r = this.postQualityRuleMapper.selectById(id);
        return r != null && Integer.valueOf(0).equals(r.getDelFlag()) && inWindow(r, at);
    }

    @Override
    public int countAvailable(Date at) {
        return listAvailable(at).size();
    }

    /**
     * 分档：与分界数一样高的那一档算高一档；分界数空着是不设这一格的意思，
     * 不算断档也不报错，跟着下一道实界往下落（如高界空着，中界之上最高只到三档）。
     */
    private int levelOf(TPostQualityRule rule, BigDecimal input) {
        int level = 1;
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) >= 0) {
            level = 2;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) >= 0) {
            level = 3;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) >= 0) {
            level = 4;
        }
        return level;
    }
}
