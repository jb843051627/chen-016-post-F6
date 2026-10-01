package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TPostOutletCard;

import java.util.List;

/**
 * 快递末端网点备案建档卡 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITPostOutletCardService {

    /** 按主键查询 */
    TPostOutletCard selectTPostOutletCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TPostOutletCard> selectTPostOutletCardList(Wrapper<TPostOutletCard> queryWrapper);

    /** 新增 */
    int insertTPostOutletCard(TPostOutletCard record);

    /** 修改 */
    int updateTPostOutletCard(TPostOutletCard record);

    /** 批量删除 */
    int deleteTPostOutletCardByIds(String ids);

    /** 按主键删除 */
    int deleteTPostOutletCardById(Long id);
}
