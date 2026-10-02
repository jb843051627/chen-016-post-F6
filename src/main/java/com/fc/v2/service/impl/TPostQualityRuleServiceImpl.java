package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostQualityRuleMapper;
import com.fc.v2.model.auto.TPostQualityRule;
import com.fc.v2.model.custom.QualityGradeQuery;
import com.fc.v2.model.custom.QualityGradeResult;
import com.fc.v2.service.ITPostQualityRuleService;

/**
 * 服务质量达标率判据线 Service业务层处理（rule-eval 形状）。
 *
 * <p>全局一册线，取线只走这一处。交棒口径全局一套：旧线管到交棒日前一刻、
 * 新线自交棒日当日开口（启用之日含、交棒之日不含），企业自核与局里抽查同此口径。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostQualityRuleServiceImpl implements ITPostQualityRuleService {

    /** 现行旗 */
    private static final int STATUS_CURRENT = 0;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 顺位降序；并列按代号降序（代号靠后者说话）。确定性规矩，季季照办，不许这季认前下季认后 */
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
            return cb.compareTo(ca);
        }
    };

    @javax.annotation.Resource
    private TPostQualityRuleMapper postQualityRuleMapper;

    /**
     * 时段比较统一走<b>墙钟字符串</b>（yyyy-MM-dd HH:mm:ss）：
     * JDBC 的 serverTimezone(GMT) 与本机时区不对称，直接把 java.util.Date 作参数会整体偏移，
     * 使边界判定在"交棒当日"这类用例上随机错档（本库既有实测，见旧版注释）。
     * 入参 at 按本机墙钟取面（考核日就是考核员落笔记的那一天）；
     * 库里读出的启用/交棒日按会话时区 GMT 还原成入库时写下的墙钟。两边都在墙钟面上比。
     */
    private static String queryWall(Date at) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(at);
    }

    /** 库里 datetime 由驱动按 serverTimezone=GMT 编成瞬时；挂 GMT 日历即还原入库墙钟 */
    private static String storedWall(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.format(d);
    }

    @Override
    public TPostQualityRule selectTPostQualityRuleById(Long id) {
        return this.postQualityRuleMapper.selectById(id);
    }

    @Override
    public QualityGradeResult evaluate(QualityGradeQuery query) {
        // 取线与定档在这一回里走完，中间不夹第二回计算。
        if (query == null
                || query.getAssessedAt() == null
                || query.getServiceType() == null || query.getServiceType().trim().isEmpty()
                || !inRange(query.getRate())) {
            // 负数 / 一百二 / 空着：不进求值，退回考核员重录
            return QualityGradeResult.invalid();
        }
        List<TPostQualityRule> avail = listAvailable(query.getServiceType(), query.getAssessedAt());
        if (avail.isEmpty()) {
            // 翻不出可用线：注明无线可依，不就近挑一条，更不许硬给结论
            return QualityGradeResult.noRule();
        }
        TPostQualityRule speaker = avail.get(0);
        return QualityGradeResult.graded(levelOf(speaker, query.getRate()), speaker);
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || at == null || !inRange(input)) {
            return 0;
        }
        TPostQualityRule rule = this.postQualityRuleMapper.selectOne(new QueryWrapper<TPostQualityRule>()
                .eq("del_flag", 0)
                .eq("rule_code", ruleCode));
        if (!onDuty(rule, at, null)) {
            return 0;
        }
        return levelOf(rule, input);
    }

    @Override
    public int evaluateTop(String serviceType, BigDecimal input, Date at) {
        if (serviceType == null || serviceType.trim().isEmpty() || at == null || !inRange(input)) {
            return 0;
        }
        List<TPostQualityRule> avail = listAvailable(serviceType, at);
        return avail.isEmpty() ? 0 : levelOf(avail.get(0), input);
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (at == null || !inRange(input)) {
            return 0;
        }
        List<TPostQualityRule> avail = listAvailable(at);
        return avail.isEmpty() ? 0 : levelOf(avail.get(0), input);
    }

    @Override
    public List<TPostQualityRule> listAvailable(Date at) {
        if (at == null) {
            return new ArrayList<TPostQualityRule>();
        }
        // 不限类别：serviceType 传 null
        return onDutyRows(baseWindowQuery(at), at, null);
    }

    @Override
    public List<TPostQualityRule> listAvailable(String serviceType, Date at) {
        if (serviceType == null || serviceType.trim().isEmpty() || at == null) {
            return new ArrayList<TPostQualityRule>();
        }
        // SQL 里 .eq service_type 是生产预筛；类别资格在 onDutyRows 里同一谓词再兜一遍，口径只有一处
        return onDutyRows(baseWindowQuery(at).eq("service_type", serviceType), at, serviceType);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        return onDuty(this.postQualityRuleMapper.selectById(id), at, null);
    }

    @Override
    public int countAvailable(Date at) {
        return listAvailable(at).size();
    }

    @Override
    public int countAvailable(String serviceType, Date at) {
        return listAvailable(serviceType, at).size();
    }

    /**
     * 在场窗口的库面条件（墙钟字符串绑定）：启用之日含起、交棒之日不含止，交棒日空着＝一直开口。
     * 旗不在库里筛——已封口的让位线在它（过往）窗口内仍须能被倒查请出，开口线的现行旗在
     * {@link #onDutyRows} 兜底。
     */
    private QueryWrapper<TPostQualityRule> baseWindowQuery(Date at) {
        String atWall = queryWall(at);
        return new QueryWrapper<TPostQualityRule>()
                .eq("del_flag", 0)
                .le("eff_start", atWall)
                .and(w -> w.isNull("eff_end").or().gt("eff_end", atWall));
    }

    /**
     * 开口线（无交棒日）兜底：必须仍挂现行旗。已让位又没记交棒日的，闭口不言，
     * 不许往现行求值里挤。serviceType 非空时类别不符的也剔除（与库面 .eq 同口径，再兜一遍）。
     * 随后按"顺位降序、代号降序"排定说话次序。
     */
    private List<TPostQualityRule> onDutyRows(QueryWrapper<TPostQualityRule> q, Date at, String serviceType) {
        List<TPostQualityRule> rows = this.postQualityRuleMapper.selectList(q);
        List<TPostQualityRule> hit = new ArrayList<TPostQualityRule>();
        for (TPostQualityRule r : rows) {
            if (onDuty(r, at, serviceType)) {
                hit.add(r);
            }
        }
        hit.sort(PRIORITY_THEN_CODE_DESC);
        return hit;
    }

    /**
     * 唯一在场口径，取线只凭考核日 at，不凭今天；所管类别各管各的。
     *
     * <p>资格：未删除；类别相符（serviceType 为 null 表示不限类别）；
     * 启用之日 ≤ 考核日 &lt; 交棒之日（墙钟面，交棒之日不含）；
     * 开口线（交棒日空着）须仍挂现行旗。
     *
     * <p>已封口（交棒日已记）的让位线，其窗口落在过去、挤不进今天的求值，
     * 却正好在过往那个考核日上在场——去年秋天的考核今年开春补结论，请出的就是它。
     */
    private boolean onDuty(TPostQualityRule r, Date at, String serviceType) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getDelFlag() != null && r.getDelFlag() != 0) {
            return false;
        }
        if (serviceType != null && !serviceType.equals(r.getServiceType())) {
            return false;
        }
        Date start = r.getEffStart();
        if (start == null || storedWall(start).compareTo(queryWall(at)) > 0) {
            return false;
        }
        Date end = r.getEffEnd();
        if (end != null) {
            // 交棒之日不含：墙钟相等这一刻旧线已不说话
            return storedWall(end).compareTo(queryWall(at)) > 0;
        }
        // 开口线（无交棒日）须仍挂现行旗；已让位又开口的，闭口不言
        return r.getStatus() == null || r.getStatus() == STATUS_CURRENT;
    }

    /** 达标率只收 0..100 的；空着、负数、一百二都退回重录 */
    private boolean inRange(BigDecimal input) {
        return input != null
                && input.compareTo(BigDecimal.ZERO) >= 0
                && input.compareTo(HUNDRED) <= 0;
    }

    /**
     * 定档（达标率越高档越高）：从高界往下靠，与分界数一样高算高一档，故用 &gt;= 向上取。
     *
     * <p>某一界空着＝不设这一格，不报错、不断档，跟着下一界往下落
     * （早年纸质簿登记的高界留空，越过中界的值也只落到中界那一格）。
     */
    private int levelOf(TPostQualityRule rule, BigDecimal rate) {
        if (rule.getTh3Max() != null && rate.compareTo(rule.getTh3Max()) >= 0) {
            return 4;
        }
        if (rule.getTh2Max() != null && rate.compareTo(rule.getTh2Max()) >= 0) {
            return 3;
        }
        if (rule.getTh1Max() != null && rate.compareTo(rule.getTh1Max()) >= 0) {
            return 2;
        }
        return 1;
    }
}
