package com.zhi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.zhi.system.domain.FriendLink;
import com.zhi.system.domain.MediaReference;

/**
 * 友情链接Mapper接口
 * 
 * @author nevell
 * @date 2025-09-08
 */
public interface FriendLinkMapper 
{
    /**
     * 查询友情链接列表
     * 
     * @param friendLink 友情链接
     * @return 友情链接集合
     */
    public List<FriendLink> selectFriendLinkList(FriendLink friendLink);

    /**
     * 查询前台展示的友情链接列表
     * 
     * @return 友情链接集合
     */
    public List<FriendLink> selectFrontFriendLinkList();

    /**
     * 通过ID查询单条数据
     * 
     * @param id 友链ID
     * @return 实例对象
     */
    public FriendLink selectFriendLinkById(Long id);

    /**
     * 新增友情链接
     * 
     * @param friendLink 友情链接
     * @return 影响行数
     */
    public int insertFriendLink(FriendLink friendLink);

    /**
     * 修改友情链接
     * 
     * @param friendLink 友情链接
     * @return 影响行数
     */
    public int updateFriendLink(FriendLink friendLink);

    /**
     * 通过主键删除数据
     * 
     * @param id 友链ID
     * @return 影响行数
     */
    public int deleteFriendLinkById(Long id);

    /**
     * 批量删除友情链接
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    public int deleteFriendLinkByIds(Long[] ids);

    /**
     * 查询引用了指定文件名 Logo 的友链（媒体库删除前检查）
     *
     * @param keyword 文件名
     * @return 引用位置
     */
    public List<MediaReference> selectMediaReferences(@Param("keyword") String keyword);
}