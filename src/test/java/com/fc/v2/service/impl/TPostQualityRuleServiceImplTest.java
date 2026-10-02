package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.mapper.auto.TPostQualityRuleMapper;
import com.fc.v2.model.auto.TPostQualityRule;
import com.fc.v2.model.custom.QualityGradeQuery;
import com.fc.v2.model.custom.QualityGradeResult;

/**
 * 并线一册、一回求值的口径测试。
 *
 * <p>时区刻意设成 Asia/Shanghai：库列 datetime 由驱动按 serverTimezone=GMT 还原，
 * 考核日按考核员本地墙钟落账，两者墙钟面比较——交棒当日不能错档。
 */
public class TPostQualityRuleServiceImplTest {

    private static TimeZone savedTz;

    private TPostQualityRuleMapper mapper;
    private TPostQualityRuleServiceImpl service;
    private final List<TPostQualityRule> rows = new ArrayList<TPostQualityRule>();

    @BeforeAll
    static void pinZone() {
        savedTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    @AfterAll
    static void restoreZone() {
        TimeZone.setDefault(savedTz);
    }

    @BeforeEach
    void setUp() {
        rows.clear();
        mapper = Mockito.mock(TPostQualityRuleMapper.class);
        // 库面窗口/.eq 预筛与 Java 谓词同口径：直接把全册交给 onDuty 复核
        when(mapper.selectList(any(Wrapper.class))).thenAnswer(inv -> new ArrayList<TPostQualityRule>(rows));
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(inv -> {
            // 仅模拟 selectOne 按 rule_code 取那一条（del_flag 仍由谓词把口）
            return null;
        });
        service = new TPostQualityRuleServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "postQualityRuleMapper", mapper);
    }

    /** 库读出的 datetime：按 GMT 编成瞬时（模拟 connector/J，serverTimezone=GMT） */
    private static Date gmt(String wall) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            return f.parse(wall);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /** 考核日：考核员本地墙钟 */
    private static Date at(String wall) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(wall);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private TPostQualityRule rule(long id, String code, String type,
                                  String th1, String th2, String th3,
                                  String start, String end, int prio, int status) {
        TPostQualityRule r = new TPostQualityRule();
        r.setId(id);
        r.setRuleCode(code);
        r.setRuleName(code + "-名");
        r.setServiceType(type);
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        r.setEffStart(gmt(start));
        r.setEffEnd(end == null ? null : gmt(end));
        r.setPriority(prio);
        r.setStatus(status);
        r.setDelFlag(0);
        rows.add(r);
        return r;
    }

    private QualityGradeResult eval(String type, String rate, String when) {
        return service.evaluate(new QualityGradeQuery(
                type, rate == null ? null : new BigDecimal(rate), at(when)));
    }

    private void seedHandoverPair() {
        // 年初线：管到 2025-10-01（不含），已让位
        rule(1, "TC-OLD", "1", "60", "80", "90", "2025-01-01 00:00:00", "2025-10-01 00:00:00", 10, 1);
        // 入秋版：2025-10-01 起，无交棒日，现行，顺位 20
        rule(2, "TC-NEW", "1", "65", "85", "95", "2025-10-01 00:00:00", null, 20, 0);
        // 同季并排辅助线：顺位 15，争不过 NEW
        rule(3, "TC-AID", "1", "70", "88", "96", "2025-10-01 00:00:00", null, 15, 0);
    }

    // ---- 定档：等于分界算高一档（达标率越高档越高） ----

    @Test
    void equalThresholdTakesHigherGrade() {
        seedHandoverPair();
        assertEquals(1, eval("1", "64.99", "2025-11-01 09:00:00").getGradeLevel());
        assertEquals(2, eval("1", "65", "2025-11-01 09:00:00").getGradeLevel());   // 等于低界→高档
        assertEquals(3, eval("1", "85", "2025-11-01 09:00:00").getGradeLevel());   // 等于中界→高档
        assertEquals(4, eval("1", "95", "2025-11-01 09:00:00").getGradeLevel());   // 等于高界→高档
        assertEquals(4, eval("1", "100", "2025-11-01 09:00:00").getGradeLevel());
    }

    @Test
    void pinnedCodeAndEffStartComeFromSpeaker() {
        seedHandoverPair();
        QualityGradeResult r = eval("1", "86", "2025-11-01 09:00:00");
        assertTrue(r.isGraded());
        assertEquals(3, r.getGradeLevel());
        assertEquals("TC-NEW", r.getRuleCode());
        assertEquals(gmt("2025-10-01 00:00:00"), r.getEffStart());
    }

    // ---- 取线凭考核日，不凭今天：补结论请当时在场的线 ----

    @Test
    void historicalAssessmentPicksOldVersion() {
        seedHandoverPair();
        // 去年九月（交棒前）的考核，今年补结论：请 OLD，不请 NEW
        QualityGradeResult r = eval("1", "80", "2025-09-30 23:59:59");
        assertEquals(QualityGradeResult.Outcome.GRADED, r.getOutcome());
        assertEquals("TC-OLD", r.getRuleCode());
        assertEquals(3, r.getGradeLevel()); // 等于中界 80→3
    }

    @Test
    void handoverDayIsExclusive() {
        seedHandoverPair();
        // 交棒当日 00:00:00：OLD 已封口，NEW 当日开口
        QualityGradeResult r = eval("1", "80", "2025-10-01 00:00:00");
        assertEquals("TC-NEW", r.getRuleCode());
        assertEquals(2, r.getGradeLevel()); // 80 在新版 65..85 之间→2 档
    }

    @Test
    void effStartIsInclusive() {
        seedHandoverPair();
        // 启用当日即在场
        assertEquals("TC-NEW", eval("1", "90", "2025-10-01 08:00:00").getRuleCode());
    }

    // ---- 一屏定夺：顺位最大者说话；顺位拉不开认代号靠后者 ----

    @Test
    void priorityWinsAcrossConcurrentLines() {
        seedHandoverPair();
        // AID 界更松但顺位低，不许说话
        QualityGradeResult r = eval("1", "89", "2025-11-01 09:00:00");
        assertEquals("TC-NEW", r.getRuleCode());
        assertEquals(3, r.getGradeLevel());
    }

    @Test
    void tiePrioritySettledByLaterCode() {
        rule(10, "AA", "2", "60", "80", "90", "2025-01-01 00:00:00", null, 10, 0);
        rule(11, "BB", "2", "50", "70", "88", "2025-01-01 00:00:00", null, 10, 0);
        QualityGradeResult r = eval("2", "75", "2025-11-01 09:00:00");
        assertEquals("BB", r.getRuleCode()); // 同顺位，代号靠后者说话
        // 规矩季季照办：再来一回仍是 BB
        assertEquals("BB", eval("2", "75", "2025-11-02 09:00:00").getRuleCode());
    }

    // ---- 旗：让位线不得挤进现行求值 ----

    @Test
    void retiredOpenEndedLineNeverSpeaks() {
        // 已让位、又没记交棒日：闭口不言
        rule(20, "OLD-OPEN", "1", "60", "80", "90", "2025-01-01 00:00:00", null, 99, 1);
        rule(21, "CUR", "1", "65", "85", "95", "2025-01-01 00:00:00", null, 10, 0);
        assertEquals("CUR", eval("1", "90", "2025-11-01 09:00:00").getRuleCode());
        // 只剩让位开口线时：无线可依，绝不拿它充数
        rows.removeIf(r -> "CUR".equals(r.getRuleCode()));
        assertEquals(QualityGradeResult.Outcome.NO_RULE, eval("1", "90", "2025-11-01 09:00:00").getOutcome());
    }

    @Test
    void deletedLineExcluded() {
        TPostQualityRule d = rule(30, "GONE", "1", "60", "80", "90", "2025-01-01 00:00:00", null, 10, 0);
        d.setDelFlag(1);
        assertEquals(QualityGradeResult.Outcome.NO_RULE, eval("1", "90", "2025-11-01 09:00:00").getOutcome());
    }

    // ---- 越界退回重录 ----

    @Test
    void outOfRangeInputsAreRejectedNotGraded() {
        seedHandoverPair();
        assertEquals(QualityGradeResult.Outcome.INVALID, eval("1", "-1", "2025-11-01 09:00:00").getOutcome());
        assertEquals(QualityGradeResult.Outcome.INVALID, eval("1", "120", "2025-11-01 09:00:00").getOutcome());
        assertEquals(QualityGradeResult.Outcome.INVALID, eval("1", null, "2025-11-01 09:00:00").getOutcome());
        assertEquals(QualityGradeResult.Outcome.INVALID,
                service.evaluate(new QualityGradeQuery("1", new BigDecimal("80"), null)).getOutcome());
        assertEquals(QualityGradeResult.Outcome.INVALID,
                service.evaluate(new QualityGradeQuery(" ", new BigDecimal("80"), at("2025-11-01 09:00:00"))).getOutcome());
        assertNull(eval("1", "-1", "2025-11-01 09:00:00").getRuleCode());
    }

    @Test
    void zeroAndHundredAreAccepted() {
        seedHandoverPair();
        assertEquals(1, eval("1", "0", "2025-11-01 09:00:00").getGradeLevel());
        assertEquals(4, eval("1", "100", "2025-11-01 09:00:00").getGradeLevel());
    }

    // ---- 无线可依：不硬给结论 ----

    @Test
    void noLineForCategoryOrDate() {
        seedHandoverPair();
        // 没有异地时限（类别 2）的线
        assertEquals(QualityGradeResult.Outcome.NO_RULE, eval("2", "80", "2025-11-01 09:00:00").getOutcome());
        // 早于任何线启用
        assertEquals(QualityGradeResult.Outcome.NO_RULE, eval("1", "80", "2024-12-31 23:59:59").getOutcome());
    }

    // ---- 空高界：不设该格，不断档，跟着中界往下落 ----

    @Test
    void blankHighThresholdMeansNoTopCell() {
        // 纸质簿登记：高界空着
        rule(40, "PR-01", "4", "70", "90", null, "2025-01-01 00:00:00", null, 10, 0);
        assertEquals(1, eval("4", "69", "2025-11-01 09:00:00").getGradeLevel());
        assertEquals(2, eval("4", "70", "2025-11-01 09:00:00").getGradeLevel());
        assertEquals(3, eval("4", "90", "2025-11-01 09:00:00").getGradeLevel());
        assertEquals(3, eval("4", "99.99", "2025-11-01 09:00:00").getGradeLevel()); // 越过中界也只落中界格
    }

    // ---- 类别各管各的 ----

    @Test
    void categoriesAreIsolated() {
        seedHandoverPair();
        rule(50, "REMOTE", "2", "60", "75", "90", "2025-10-01 00:00:00", null, 5, 0);
        // 类别 2 只见 REMOTE，不会被类别 1 顺位更高的 NEW 抢走
        assertEquals("REMOTE", eval("2", "80", "2025-11-01 09:00:00").getRuleCode());
        assertEquals(1, service.countAvailable("2", at("2025-11-01 09:00:00")));
        assertEquals(2, service.countAvailable("1", at("2025-11-01 09:00:00"))); // OLD 已封口不算，NEW+AID 两条
    }

    // ---- 老口子签名不变；同名另立的类别取法 ----

    @Test
    void legacyEvaluateByCodeKeepsSignature() {
        seedHandoverPair();
        // 库面 selectOne 按 code 取：这里用桩模拟
        TPostQualityRule oldLine = rows.get(0);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(oldLine);
        assertEquals(3, service.evaluate("TC-OLD", new BigDecimal("80"), at("2025-09-30 23:59:59")));
        // 同一条已封口线，拿到交棒后去算 → 0
        assertEquals(0, service.evaluate("TC-OLD", new BigDecimal("80"), at("2025-10-01 00:00:00")));
        // 越界仍 0
        assertEquals(0, service.evaluate("TC-OLD", new BigDecimal("120"), at("2025-09-30 23:59:59")));
    }

    @Test
    void evaluateTopVariants() {
        seedHandoverPair();
        assertEquals(3, service.evaluateTop("1", new BigDecimal("85"), at("2025-11-01 09:00:00")));
        assertEquals(0, service.evaluateTop("2", new BigDecimal("85"), at("2025-11-01 09:00:00"))); // 类别无线
        assertEquals(3, service.evaluateTop(new BigDecimal("85"), at("2025-11-01 09:00:00")));      // 不分类别
    }

    @Test
    void usableAgreesWithSelection() {
        seedHandoverPair();
        when(mapper.selectById(1L)).thenReturn(rows.get(0));
        when(mapper.selectById(2L)).thenReturn(rows.get(1));
        assertTrue(service.usable(1L, at("2025-09-30 23:59:59")));
        assertFalse(service.usable(1L, at("2025-10-01 00:00:00"))); // 交棒后封口
        assertTrue(service.usable(2L, at("2025-10-01 00:00:00")));
        assertFalse(service.usable(99L, at("2025-10-01 00:00:00")));
    }
}
