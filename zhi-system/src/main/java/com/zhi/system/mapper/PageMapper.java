package com.zhi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.zhi.system.domain.MediaReference;
import com.zhi.system.domain.Page;

/**
 * 自定义页面Mapper接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
public interface PageMapper 
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
     * 校验别名是否唯一
     * 
     * @param slug 页面别名
     * @return 已存在的页面（不存在返回 null）
     */
    public Page checkSlugUnique(String slug);

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
     * 增加浏览次数
     * 
     * @param id 页面ID
     * @return 结果
     */
    public int increaseViewCount(Long id);

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

    /**
     * 查询引用了指定文件名的自定义页面（媒体库删除前检查，含已删除页面）
     *
     * @param keyword 文件名
     * @return 引用位置
     */
    public List<MediaReference> selectMediaReferences(@Param("keyword") String keyword);
}
