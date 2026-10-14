package com.zhi.system.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhi.common.core.controller.BaseController;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.core.page.TableDataInfo;
import com.zhi.system.domain.Upload;
import com.zhi.system.service.IUploadService;

/**
 * 博客媒体库（上传记录管理）Controller
 *
 * @author nevell
 * @date 2026-09-05
 */
@RestController
@RequestMapping("/system/media")
public class MediaController extends BaseController
{
    @Autowired
    private IUploadService uploadService;

    /**
     * 查询上传记录列表
     */
    @PreAuthorize("@ss.hasPermi('zhi:media:list')")
    @GetMapping("/list")
    public TableDataInfo list(Upload upload)
    {
        startPage();
        List<Upload> list = uploadService.selectUploadList(upload);
        return getDataTable(list);
    }

    /**
     * 删除上传记录（同时尝试删除物理文件）
     */
    @PreAuthorize("@ss.hasPermi('zhi:media:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return error("参数不合法");
        }
        int total = 0;
        for (Long id : ids)
        {
            total += uploadService.deleteUploadById(id);
        }
        return toAjax(total);
    }
}
