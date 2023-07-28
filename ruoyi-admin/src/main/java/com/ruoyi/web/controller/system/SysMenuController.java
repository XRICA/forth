package com.ruoyi.web.controller.system;

import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.monitor.domain.Pollutant;
import com.ruoyi.monitor.utils.APICenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysMenuService;

/**
 * 菜单信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController
{
    @Autowired
    private ISysMenuService menuService;
    @Autowired
    APICenter apiCenter;
    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    ThreadPoolTaskExecutor pool;
    @Autowired
    private RedisCache redisCache;
    private String statusM = "0";
    @GetMapping("/function/preStatus")
    public AjaxResult functionPredict()
    {
        String status = redisCache.getCacheObject("preInfo");
        if (ObjectUtil.isEmpty(status)){
            redisCache.setCacheObject("preInfo","0");
            return success("0");
        }else {
            return success(status);
        }

    }

    /**
     * 修改自动发送状态，"0"开启，"1"关闭
     * 若为开启立刻调用
     * @param status
     * @return
     */
    @PostMapping("/function/editPredict")
    public AjaxResult editPredict(@RequestBody String status){
        System.out.println("获取状态"+status);
        if (!ObjectUtil.isEmpty(status)){
            redisCache.setCacheObject("preInfo",status);
            if ("0".equals(status)) return success("0");
            else if ("1".equals(status)) return success("1");
            }
        return error();
    }

    /**
     * 检查自动发送的状态
     * @param
     * @return
     */
    @GetMapping("/function/status")
    public AjaxResult functionAutoStatus()
    {
        String status = redisCache.getCacheObject("autoSend");
        if (ObjectUtil.isEmpty(status)){
            redisCache.setCacheObject("autoSend","0");
            return success("0");
        }else {
            return success(status);
        }
    }

    /**
     * 修改自动发送状态，"0"开启，"1"关闭
     * 若为开启立刻调用
     * @param status
     * @return
     */
    @PostMapping("/function/editAutoStatus")
    public AjaxResult editAutoSend(@RequestBody String status){
        System.out.println("获取状态"+status);
        if (!ObjectUtil.isEmpty(status)){
            redisCache.setCacheObject("autoSend",status);
            //用来判断是否是第一次发送
            if ("0".equals(statusM)){
                if ("0".equals(status)){
                    statusM = "1";
                    Pollutant pollutant = redisCache.getCacheObject("latest");
                    if (!ObjectUtil.isEmpty(pollutant)){
                        apiCenter.sendNotification(pollutant);
                    }
                }
            }
            return success(status);
        }else {
            return error();
        }

    }



    @GetMapping("/function/update")
    public AjaxResult editFunctionStatus(SysMenu menu)
    {
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 获取菜单列表
     */
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(SysMenu menu)
    {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menus);
    }

    /**
     * 根据菜单编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping(value = "/{menuId}")
    public AjaxResult getInfo(@PathVariable Long menuId)
    {
        return success(menuService.selectMenuById(menuId));
    }

    /**
     * 获取菜单下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysMenu menu)
    {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     */
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId)
    {
        List<SysMenu> menus = menuService.selectMenuList(getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(menus));
        return ajax;
    }

    /**
     * 新增菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysMenu menu)
    {
        if (UserConstants.NOT_UNIQUE.equals(menuService.checkMenuNameUnique(menu)))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        menu.setCreateBy(getUsername());
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysMenu menu)
    {
        if (UserConstants.NOT_UNIQUE.equals(menuService.checkMenuNameUnique(menu)))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (menu.getMenuId().equals(menu.getParentId()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        menu.setUpdateBy(getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 删除菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        if (menuService.hasChildByMenuId(menuId))
        {
            return warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuId))
        {
            return warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }
}