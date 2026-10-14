package com.zhi.system.service;

import java.util.List;
import com.zhi.system.domain.Page;

/**
 * 自定义页面Service接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
public interface IPageService 
{
    /**
     * 查询页面列表
     * 
     * @param page 页面
     * @return 页面集合
     */
    public List<Page> selectPageList(Page page);

    /**
     * 查询已发布页面列表（前台用）
     * 
     * @return 页面集合
     */
    public List<Page> selectPublishedPageList();

    /**
     * 通过ID查询单条数据
     * 
     * @param id 页面ID
     * @return 实例对象
     */
    public Page selectPageById(Long id);

    /**
     * 通过别名查询已发布页面
     * 
     * @param slug 页面别名
     * @return 实例对象
     */
    public Page selectPageBySlug(String slug);

    /**
     * 新增页面
     * 
     * @param page 页面
     * @return 结果
     */
    public int insertPage(Page page);

    /**
     * 修改页面
     * 
     * @param page 页面
     * @return 结果
     */
    public int updatePage(Page page);

    /**
     * 切换页面发布状态
     * 
     * @param id 页面ID
     * @param status 状态（0草稿 1已发布）
     * @return 结果
     */
    public int changePageStatus(Long id, String status);

    /**
     * 增加浏览次数
     * 
     * @param id 页面ID
     * @return 结果
     */
    public int increaseViewCount(Long id);

    /**
     * 按访客去重增加浏览次数（同一访客 24 小时内对同一页面只计一次）
     *
     * @param id 页面ID
     * @param viewerKey 访客标识（登录用户ID或IP），为空时不去重直接累加
     * @return 实际累加的行数（0 表示被去重跳过）
     */
    public int addViewCount(Long id, String viewerKey);

    /**
     * 通过主键删除数据
     * 
     * @param id 页面ID
     * @return 影响行数
     */
    public int deletePageById(Long id);

    /**
     * 批量删除页面
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    public int deletePageByIds(Long[] ids);
}
