package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TPostRectifyCard;
import com.fc.v2.service.ITPostRectifyCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 责令整改事务卡 Controller（state-machine 形状：流转入口）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "责令整改事务卡")
@Controller
@RequestMapping("/postRectifyCard")
public class PostRectifyCardController extends BaseController {

    private final String prefix = "admin/postRectifyCard";

    @Autowired
    private ITPostRectifyCardService postRectifyCardService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("postRectifyCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "责令整改事务卡流转台账", action = "list")
    @ApiOperation(value = "流转台账", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("postRectifyCard:list")
    @ResponseBody
    public ResultTable list(TPostRectifyCard record) {
        QueryWrapper<TPostRectifyCard> queryWrapper = new QueryWrapper<TPostRectifyCard>();
        startPage();
        com.github.pagehelper.PageInfo<TPostRectifyCard> page =
                new com.github.pagehelper.PageInfo<TPostRectifyCard>(postRectifyCardService.selectTPostRectifyCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "责令整改事务卡推进", action = "advance")
    @ApiOperation(value = "推进一档", notes = "推进一档")
    @PostMapping("/advance")
    @RequiresPermissions("postRectifyCard:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String remark) {
        return toAjax(postRectifyCardService.advance(id, remark) != null ? 1 : 0);
    }

    @Log(title = "责令整改事务卡回退", action = "rollback")
    @ApiOperation(value = "回退一档", notes = "回退一档")
    @PostMapping("/rollback")
    @RequiresPermissions("postRectifyCard:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return toAjax(postRectifyCardService.rollback(id, remark) != null ? 1 : 0);
    }
}
