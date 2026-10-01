package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostReportRowMapper;
import com.fc.v2.model.auto.TPostReportRow;
import com.fc.v2.service.ITPostReportRowService;

/**
 * 月度业务报表核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostReportRowServiceImpl implements ITPostReportRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TPostReportRowMapper postReportRowMapper;

    @Override
    public TPostReportRow selectTPostReportRowById(Long id) {
        return this.postReportRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TPostReportRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TPostReportRow> errors = new java.util.ArrayList<TPostReportRow>();
        int seq = 0;
        for (TPostReportRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.postReportRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TPostReportRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.postReportRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TPostReportRow> listErrors(String batchNo) {
        return this.postReportRowMapper.selectList(new QueryWrapper<TPostReportRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
