package com.fc.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostQualityRuleMapper;
import com.fc.v2.model.auto.TPostQualityRule;
import com.fc.v2.model.custom.PostQualityVerdict;
import com.fc.v2.service.impl.TPostQualityRuleServiceImpl;

/**
 * 服务质量达标率判据线求值口径单测（不启 Spring 上下文，mapper 以 Mockito 顶替）。
 *
 * 线的存法对应 fixture：
 *  R1 QP-TC-01 年初版  60/80/90  [2025-01-01, 2025-10-01) 顺位10 已让位
 *  R2 QP-TC-02 入秋版  65/85/95  [2025-10-01, 2026-01-01) 顺位10 已让位
 *  R3 QP-26A           50/70/90  [2026-01-01, 至今)       顺位5  现行
 *  R4 QP-26B1          1/2/3     [2026-01-01, 至今)       顺位9  现行
 *  R5 QP-26B2          60/80/90  [2026-01-01, 至今)       顺位9  现行（与R4并列，代号靠后者说话）
 *  R6 QP-NULL3 纸质簿  60/80/空  [2026-09-01, 至今)       顺位20 现行
 *  R7 QP-TC-09 换版前  70/80/90  [2025-01-01, 2025-07-01) 顺位0  已让位
 *  R8 QP-TC-09 换版后  60/80/90  [2025-07-01, 至今)       顺位0  现行
 *  R9 QP-DEL           0/0/0     [2020-01-01, 至今)       顺位99 已删除
 */
public class PostQualityRuleEvalTest {

    private TPostQualityRuleServiceImpl service;
    private List<TPostQualityRule> fixture;

    private static Date d(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static TPostQualityRule rule(long id, String code, String name, String th1, String th2, String th3,
            String start, String end, int priority, int status, int delFlag) {
        TPostQualityRule r = new TPostQualityRule();
        r.setId(id);
        r.setRuleCode(code);
        r.setRuleName(name);
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        r.setEffStart(start == null ? null : d(start));
        r.setEffEnd(end == null ? null : d(end));
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(delFlag);
        return r;
    }

    @BeforeEach
    public void setUp() {
        fixture = new ArrayList<TPostQualityRule>(Arrays.asList(
                rule(1, "QP-TC-01", "同城时限线·年初版", "60", "80", "90", "2025-01-01 00:00:00", "2025-10-01 00:00:00", 10, 1, 0),
                rule(2, "QP-TC-02", "同城时限线·入秋版", "65", "85", "95", "2025-10-01 00:00:00", "2026-01-01 00:00:00", 10, 1, 0),
                rule(3, "QP-26A", "异地时限线", "50", "70", "90", "2026-01-01 00:00:00", null, 5, 0, 0),
                rule(4, "QP-26B1", "投递频次线", "1", "2", "3", "2026-01-01 00:00:00", null, 9, 0, 0),
                rule(5, "QP-26B2", "时限赔偿线", "60", "80", "90", "2026-01-01 00:00:00", null, 9, 0, 0),
                rule(6, "QP-NULL3", "纸质簿登记线", "60", "80", null, "2026-09-01 00:00:00", null, 20, 0, 0),
                rule(7, "QP-TC-09", "换版前", "70", "80", "90", "2025-01-01 00:00:00", "2025-07-01 00:00:00", 0, 1, 0),
                rule(8, "QP-TC-09", "换版后", "60", "80", "90", "2025-07-01 00:00:00", null, 0, 0, 0),
                rule(9, "QP-DEL", "已删除线", "0", "0", "0", "2020-01-01 00:00:00", null, 99, 0, 1)));

        TPostQualityRuleMapper mapper = mock(TPostQualityRuleMapper.class);
        // 模拟 SQL 侧 del_flag=0 与可选 rule_code 等值过滤（rule_code 是唯一会出现的字符串参数）。
        // 注意：3.4.2 的 Wrapper 条件是延迟渲染的，paramNameValuePairs 要到 getSqlSegment()
        // 被调用时才填充（真实 MyBatis 执行 selectList 必走这一步），mock 里须先触一次渲染再读参。
        when(mapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<TPostQualityRule> w = inv.getArgument(0);
            w.getSqlSegment();
            String code = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    code = (String) v;
                }
            }
            List<TPostQualityRule> out = new ArrayList<TPostQualityRule>();
            for (TPostQualityRule r : fixture) {
                if (!Integer.valueOf(0).equals(r.getDelFlag())) {
                    continue;
                }
                if (code != null && !code.equals(r.getRuleCode())) {
                    continue;
                }
                out.add(r);
            }
            return out;
        });
        when(mapper.selectById(any())).thenAnswer(inv -> {
            Object id = inv.getArgument(0);
            for (TPostQualityRule r : fixture) {
                if (String.valueOf(r.getId()).equals(String.valueOf(id))) {
                    return r;
                }
            }
            return null;
        });

        service = new TPostQualityRuleServiceImpl();
        ReflectionTestUtils.setField(service, "postQualityRuleMapper", mapper);
    }

    /** 交棒口径：旧线管到交棒前一天为止，交棒当天起新线开口（交棒之日不含） */
    @Test
    public void handoverDay_oldLineShut_newLineSpeaks() {
        PostQualityVerdict before = service.evaluate(new BigDecimal("50"), d("2025-09-30 23:59:59"));
        assertEquals("QP-TC-01", before.getRuleCode());
        assertEquals(d("2025-01-01 00:00:00"), before.getEffStart());

        PostQualityVerdict onDay = service.evaluate(new BigDecimal("50"), d("2025-10-01 00:00:00"));
        assertEquals("QP-TC-02", onDay.getRuleCode());
        assertEquals(d("2025-10-01 00:00:00"), onDay.getEffStart());
    }

    /** 取线凭单子上的考核那一天，不凭今天：去年秋天的考核今年补结论，请出当时在场那一条 */
    @Test
    public void backdatedEvaluation_usesLineInEffectThen() {
        PostQualityVerdict v = service.evaluate(new BigDecimal("90"), d("2025-11-15 10:00:00"));
        assertEquals("QP-TC-02", v.getRuleCode());
        assertEquals(d("2025-10-01 00:00:00"), v.getEffStart());
        assertEquals(3, v.getLevel()); // 入秋版 65/85/95：85<=90<95 落三档
    }

    /** 一屏定夺：顺位大的先说话；顺位拉不开认代号靠后那条，且季季同一口径 */
    @Test
    public void parallelLines_priorityThenCode_deterministic() {
        List<TPostQualityRule> avail = service.listAvailable(d("2026-06-01 00:00:00"));
        assertEquals("QP-26B2", avail.get(0).getRuleCode()); // 顺位9 并列，代号靠后者
        assertEquals("QP-26B1", avail.get(1).getRuleCode());
        assertEquals("QP-26A", avail.get(2).getRuleCode()); // 顺位5 靠后

        PostQualityVerdict first = service.evaluate(new BigDecimal("85"), d("2026-06-01 00:00:00"));
        PostQualityVerdict second = service.evaluate(new BigDecimal("85"), d("2026-06-01 00:00:00"));
        assertEquals(first.getRuleCode(), second.getRuleCode());
        assertEquals(first.getLevel(), second.getLevel());
        assertEquals("QP-26B2", first.getRuleCode());
    }

    /** 定档：与分界数一样高的那一档算高一档 */
    @Test
    public void equalBoundary_countsAsHigherLevel() {
        Date at = d("2026-06-01 00:00:00"); // 当季 QP-26B2 说话，界 60/80/90
        assertEquals(1, service.evaluateTop(new BigDecimal("0"), at));
        assertEquals(1, service.evaluateTop(new BigDecimal("59.99"), at));
        assertEquals(2, service.evaluateTop(new BigDecimal("60"), at));
        assertEquals(2, service.evaluateTop(new BigDecimal("79.99"), at));
        assertEquals(3, service.evaluateTop(new BigDecimal("80"), at));
        assertEquals(3, service.evaluateTop(new BigDecimal("89.99"), at));
        assertEquals(4, service.evaluateTop(new BigDecimal("90"), at));
        assertEquals(4, service.evaluateTop(new BigDecimal("100"), at));
    }

    /** 高界空着：不算断档也不报错，空着是不设这一格，跟着中界往下落 */
    @Test
    public void emptyHighBound_fallsDownWithMid() {
        Date at = d("2026-10-15 00:00:00"); // 当季 QP-NULL3 说话，界 60/80/空
        assertEquals(1, service.evaluateTop(new BigDecimal("59"), at));
        assertEquals(2, service.evaluateTop(new BigDecimal("60"), at));
        assertEquals(3, service.evaluateTop(new BigDecimal("80"), at));
        assertEquals(3, service.evaluateTop(new BigDecimal("95"), at)); // 高界不设格，最高到三档
        assertEquals(3, service.evaluateTop(new BigDecimal("100"), at));
    }

    /** 负数、一百二、空着：不进求值，退回考核员重录 */
    @Test
    public void outOfRangeInput_rejectedForReentry() {
        Date at = d("2026-06-01 00:00:00");
        for (BigDecimal bad : new BigDecimal[] { new BigDecimal("-1"), new BigDecimal("120") }) {
            PostQualityVerdict v = service.evaluate(bad, at);
            assertEquals(0, v.getLevel());
            assertEquals(PostQualityVerdict.NOTE_BAD_INPUT, v.getNote());
            assertNull(v.getRuleCode());
            assertEquals(0, service.evaluateTop(bad, at));
            assertEquals(0, service.evaluate("QP-26B2", bad, at));
        }
        PostQualityVerdict empty = service.evaluate(null, at);
        assertEquals(0, empty.getLevel());
        assertEquals(PostQualityVerdict.NOTE_BAD_INPUT, empty.getNote());
    }

    /** 翻不出可用线的：注明无线可依，不就近充数，不硬给结论 */
    @Test
    public void noUsableLine_markedAndNotFaked() {
        PostQualityVerdict v = service.evaluate(new BigDecimal("88"), d("2024-06-01 00:00:00"));
        assertEquals(0, v.getLevel());
        assertEquals(PostQualityVerdict.NOTE_NO_RULE, v.getNote());
        assertNull(v.getRuleCode());
        assertNull(v.getEffStart());
        assertFalse(v.isConclusive());
        assertEquals(0, service.evaluateTop(new BigDecimal("88"), d("2024-06-01 00:00:00")));
    }

    /** 参数缺失（考核那一天没给）：同样不出结论 */
    @Test
    public void missingDate_rejected() {
        PostQualityVerdict v = service.evaluate(new BigDecimal("88"), null);
        assertEquals(0, v.getLevel());
        assertEquals(PostQualityVerdict.NOTE_MISSING_ARG, v.getNote());
    }

    /** 按代号取线的老口子：同一代号换版前后，凭考核那一天各请各的版本 */
    @Test
    public void byCode_picksVersionEffectiveAtThatDay() {
        assertEquals(1, service.evaluate("QP-TC-09", new BigDecimal("65"), d("2025-03-01 00:00:00"))); // 旧版70/80/90
        assertEquals(2, service.evaluate("QP-TC-09", new BigDecimal("65"), d("2025-08-01 00:00:00"))); // 新版60/80/90
        // 该代号在考核日没有生效版本：不就近充数
        assertEquals(0, service.evaluate("QP-TC-09", new BigDecimal("65"), d("2024-06-01 00:00:00")));
        assertEquals(0, service.evaluate("QP-NO-SUCH", new BigDecimal("65"), d("2025-08-01 00:00:00")));
        assertEquals(0, service.evaluate("  ", new BigDecimal("65"), d("2025-08-01 00:00:00")));
    }

    /** 同一回求值：结论、所依代号、那条界启用哪天一并给出 */
    @Test
    public void onePass_verdictCarriesCodeAndEffStart() {
        PostQualityVerdict v = service.evaluate(new BigDecimal("85"), d("2026-06-01 00:00:00"));
        assertTrue(v.isConclusive());
        assertEquals(3, v.getLevel());
        assertEquals("QP-26B2", v.getRuleCode());
        assertEquals("时限赔偿线", v.getRuleName());
        assertEquals(d("2026-01-01 00:00:00"), v.getEffStart());
        assertNull(v.getNote());
    }

    /** 已让位的旧线：只供倒查来历（其时段内仍请得出），往现行求值里挤不得 */
    @Test
    public void retiredLine_historyOnly() {
        List<TPostQualityRule> past = service.listAvailable(d("2025-05-01 00:00:00"));
        assertTrue(past.stream().anyMatch(r -> "QP-TC-01".equals(r.getRuleCode())));
        assertTrue(service.usable(1L, d("2025-05-01 00:00:00")));

        List<TPostQualityRule> now = service.listAvailable(d("2026-06-01 00:00:00"));
        assertFalse(now.stream().anyMatch(r -> "QP-TC-01".equals(r.getRuleCode())));
        assertFalse(now.stream().anyMatch(r -> "QP-TC-02".equals(r.getRuleCode())));
        assertFalse(service.usable(1L, d("2026-06-01 00:00:00")));
    }

    /** 已删除的线不在场，顺位再高也轮不到 */
    @Test
    public void deletedLine_neverSpeaks() {
        List<TPostQualityRule> now = service.listAvailable(d("2026-06-01 00:00:00"));
        assertFalse(now.stream().anyMatch(r -> "QP-DEL".equals(r.getRuleCode())));
        assertFalse(service.usable(9L, d("2026-06-01 00:00:00")));
        assertEquals("QP-26B2", service.evaluate(new BigDecimal("50"), d("2026-06-01 00:00:00")).getRuleCode());
    }

    /** 列表/计数/单条可用判定同一口径 */
    @Test
    public void listCountUsable_sameCaliber() {
        Date at = d("2026-06-01 00:00:00");
        List<TPostQualityRule> avail = service.listAvailable(at);
        assertEquals(4, avail.size()); // R3、R4、R5、R8
        assertEquals(avail.size(), service.countAvailable(at));
        for (TPostQualityRule r : avail) {
            assertTrue(service.usable(r.getId(), at));
        }
        assertFalse(service.usable(null, at));
        assertFalse(service.usable(3L, null));
        assertEquals(0, service.countAvailable(null));
        assertTrue(service.listAvailable(null).isEmpty());
    }
}
