package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TPostReportRow;

/**
 * 月度业务报表核收行 Service接口（batch-process 形状：整批提交，无单条增删改）
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITPostReportRowService {

    /** 按主键回查单行 */
    TPostReportRow selectTPostReportRowById(Long id);

    /**
     * 整批提交：逐行校验，合法的入库、非法的记入失败明细（保留原始行号），返回**成功行数**。
     * 同一批次重复提交不得重复入库；行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     */
    int submitBatch(String batchNo, List<TPostReportRow> rows);

    /** 该批次的失败行明细（按行号升序） */
    List<TPostReportRow> listErrors(String batchNo);
}
