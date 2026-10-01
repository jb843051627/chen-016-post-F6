package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TPostRectifyCardMapper;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.service.ITPostRectifyCardService;

/**
 * 责令整改事务卡 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TPostRectifyCardServiceImpl implements ITPostRectifyCardService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TPostRectifyCardMapper postRectifyCardMapper;

    @Override
    public TPostRectifyCard selectTPostRectifyCardById(Long id) {
        return this.postRectifyCardMapper.selectById(id);
    }

    @Override
    public List<TPostRectifyCard> selectTPostRectifyCardList(QueryWrapper<TPostRectifyCard> queryWrapper) {
        return this.postRectifyCardMapper.selectList(queryWrapper);
    }

    @Override
    public TPostRectifyCard advance(Long id, String remark) {
        TPostRectifyCard r = this.postRectifyCardMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.postRectifyCardMapper.updateById(r);
        return r;
    }

    @Override
    public TPostRectifyCard rollback(Long id, String remark) {
        TPostRectifyCard r = this.postRectifyCardMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.postRectifyCardMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TPostRectifyCard r = this.postRectifyCardMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.postRectifyCardMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TPostRectifyCard r = this.postRectifyCardMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.postRectifyCardMapper.deleteById(id) > 0;
    }

}
