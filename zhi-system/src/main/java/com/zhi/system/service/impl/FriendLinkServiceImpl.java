package com.zhi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.system.domain.FriendLink;
import com.zhi.system.mapper.FriendLinkMapper;
import com.zhi.system.service.IFriendLinkService;

/**
 * 友情链接Service业务层处理
 * 
 * @author nevell
 * @date 2025-09-08
 */
@Service
public class FriendLinkServiceImpl  implements IFriendLinkService
{
    @Autowired
    private FriendLinkMapper friendLinkMapper;

    /**
     * 查询友情链接列表
     * 
     * @param friendLink 友情链接
     * @return 友情链接集合
     */
    @Override
    public List<FriendLink> selectFriendLinkList(FriendLink friendLink)
    {
        return friendLinkMapper.selectFriendLinkList(friendLink);
    }

    /**
     * 查询前台展示的友情链接列表
     * 
     * @return 友情链接集合
     */
    @Override
    public List<FriendLink> selectFrontFriendLinkList()
    {
        return friendLinkMapper.selectFrontFriendLinkList();
    }

    /**
     * 通过ID查询单条数据
     * 
     * @param id 友链ID
     * @return 实例对象
     */
    @Override
    public FriendLink selectFriendLinkById(Long id)
    {
        return friendLinkMapper.selectFriendLinkById(id);
    }

    /**
     * 新增友情链接
     * 
     * @param friendLink 友情链接
     * @return 结果
     */
    @Override
    public int insertFriendLink(FriendLink friendLink)
    {
        friendLink.setCreateBy(SecurityUtils.getUsername());
        return friendLinkMapper.insertFriendLink(friendLink);
    }

    /**
     * 修改友情链接
     * 
     * @param friendLink 友情链接
     * @return 结果
     */
    @Override
    public int updateFriendLink(FriendLink friendLink)
    {
        friendLink.setUpdateBy(SecurityUtils.getUsername());
        return friendLinkMapper.updateFriendLink(friendLink);
    }

    /**
     * 通过主键删除数据
     * 
     * @param id 友链ID
     * @return 影响行数
     */
    @Override
    public int deleteFriendLinkById(Long id)
    {
        return friendLinkMapper.deleteFriendLinkById(id);
    }

    /**
     * 批量删除友情链接
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    public int deleteFriendLinkByIds(Long[] ids)
    {
        return friendLinkMapper.deleteFriendLinkByIds(ids);
    }
}