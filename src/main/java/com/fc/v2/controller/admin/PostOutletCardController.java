package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TPostOutletCard;
import com.fc.v2.service.ITPostOutletCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 快递末端网点备案建档卡 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "快递末端网点备案建档卡")
@Controller
@RequestMapping("/PostOutletCardController")
public class PostOutletCardController extends BaseController {

    private final String prefix = "admin/postOutletCard";

    @Autowired
    private ITPostOutletCardService postOutletCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("post:postOutletCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "快递末端网点备案建档卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("post:postOutletCard:list")
    @ResponseBody
    public ResultTable list(TPostOutletCard record) {
        QueryWrapper<TPostOutletCard> queryWrapper = new QueryWrapper<TPostOutletCard>();
        startPage();
        com.github.pagehelper.PageInfo<TPostOutletCard> page =
                new com.github.pagehelper.PageInfo<TPostOutletCard>(postOutletCardService.selectTPostOutletCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "快递末端网点备案建档卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("post:postOutletCard:add")
    @ResponseBody
    public AjaxResult add(TPostOutletCard record) {
        return toAjax(postOutletCardService.insertTPostOutletCard(record));
    }

    @Log(title = "快递末端网点备案建档卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("post:postOutletCard:edit")
    @ResponseBody
    public AjaxResult editSave(TPostOutletCard record) {
        return toAjax(postOutletCardService.updateTPostOutletCard(record));
    }

    @Log(title = "快递末端网点备案建档卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("post:postOutletCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(postOutletCardService.deleteTPostOutletCardByIds(ids));
    }
}
