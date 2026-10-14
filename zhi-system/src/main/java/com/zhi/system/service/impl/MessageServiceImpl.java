package com.zhi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.ModernDateUtils;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.system.domain.Message;
import com.zhi.system.mapper.MessageMapper;
import com.zhi.system.service.IMessageService;

/**
 * 留言板Service业务层处理
 * 
 * @author nevell
 * @date 2026-09-10
 */
@Service
public class MessageServiceImpl implements IMessageService
{
    /** 待审核 */
    private static final String STATUS_PENDING = "0";

    /** 已发布 */
    private static final String STATUS_PUBLISHED = "1";

    /** 已拒绝 */
    private static final String STATUS_REJECTED = "2";

    @Autowired
    private MessageMapper messageMapper;

    /**
     * 查询留言列表
     * 
     * @param message 留言
     * @return 留言集合
     */
    @Override
    public List<Message> selectMessageList(Message message)
    {
        return messageMapper.selectMessageList(message);
    }

    /**
     * 查询前台展示的留言列表（仅已发布）
     * 
     * @return 留言集合
     */
    @Override
    public List<Message> selectPublishedMessageList()
    {
        return messageMapper.selectPublishedMessageList();
    }

    /**
     * 查询前台已发布留言总数
     * 
     * @return 留言总数
     */
    @Override
    public Long selectPublishedMessageCount()
    {
        return messageMapper.selectPublishedMessageCount();
    }

    /**
     * 通过ID查询单条数据
     * 
     * @param id 留言ID
     * @return 实例对象
     */
    @Override
    public Message selectMessageById(Long id)
    {
        return messageMapper.selectMessageById(id);
    }

    /**
     * 新增留言
     * 
     * @param message 留言
     * @return 结果
     */
    @Override
    public int insertMessage(Message message)
    {
        if (StringUtils.isEmpty(message.getStatus()))
        {
            message.setStatus(STATUS_PENDING);
        }
        if (StringUtils.isEmpty(message.getDelFlag()))
        {
            message.setDelFlag("0");
        }
        message.setCreateTime(ModernDateUtils.now());
        return messageMapper.insertMessage(message);
    }

    /**
     * 修改留言
     * 
     * @param message 留言
     * @return 结果
     */
    @Override
    public int updateMessage(Message message)
    {
        message.setUpdateTime(ModernDateUtils.now());
        return messageMapper.updateMessage(message);
    }

    /**
     * 审核留言（status：0待审核 1已发布 2已拒绝）
     * 
     * @param id 留言ID
     * @param status 目标状态
     * @return 结果
     */
    @Override
    public int auditMessage(Long id, String status)
    {
        if (!STATUS_PENDING.equals(status) && !STATUS_PUBLISHED.equals(status) && !STATUS_REJECTED.equals(status))
        {
            throw new ServiceException("留言状态不合法");
        }
        Message message = new Message();
        message.setId(id);
        message.setStatus(status);
        message.setUpdateBy(SecurityUtils.getUsername());
        message.setUpdateTime(ModernDateUtils.now());
        return messageMapper.updateMessage(message);
    }

    /**
     * 回复留言
     * 
     * @param id 留言ID
     * @param replyContent 回复内容
     * @return 结果
     */
    @Override
    public int replyMessage(Long id, String replyContent)
    {
        if (StringUtils.isEmpty(replyContent))
        {
            throw new ServiceException("回复内容不能为空");
        }
        if (replyContent.length() > 500)
        {
            throw new ServiceException("回复内容长度不能超过500个字符");
        }
        Message message = new Message();
        message.setId(id);
        message.setReplyContent(replyContent);
        message.setReplyTime(ModernDateUtils.now());
        message.setReplyBy(SecurityUtils.getUsername());
        // 回复即视为审核通过
        message.setStatus(STATUS_PUBLISHED);
        message.setUpdateBy(SecurityUtils.getUsername());
        message.setUpdateTime(ModernDateUtils.now());
        return messageMapper.updateMessage(message);
    }

    /**
     * 通过主键删除数据
     * 
     * @param id 留言ID
     * @return 影响行数
     */
    @Override
    public int deleteMessageById(Long id)
    {
        return messageMapper.deleteMessageById(id);
    }

    /**
     * 批量删除留言
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    @Override
    public int deleteMessageByIds(Long[] ids)
    {
        return messageMapper.deleteMessageByIds(ids);
    }
}
