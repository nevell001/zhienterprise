package com.zhi.system.mapper;

import java.util.List;
import com.zhi.system.domain.Message;

/**
 * 留言板Mapper接口
 * 
 * @author nevell
 * @date 2026-09-10
 */
public interface MessageMapper 
{
    /**
     * 查询留言列表
     * 
     * @param message 留言
     * @return 留言集合
     */
    public List<Message> selectMessageList(Message message);

    /**
     * 查询前台展示的留言列表（仅已发布）
     * 
     * @return 留言集合
     */
    public List<Message> selectPublishedMessageList();

    /**
     * 查询前台已发布留言总数
     * 
     * @return 留言总数
     */
    public Long selectPublishedMessageCount();

    /**
     * 通过ID查询单条数据
     * 
     * @param id 留言ID
     * @return 实例对象
     */
    public Message selectMessageById(Long id);

    /**
     * 新增留言
     * 
     * @param message 留言
     * @return 结果
     */
    public int insertMessage(Message message);

    /**
     * 修改留言
     * 
     * @param message 留言
     * @return 结果
     */
    public int updateMessage(Message message);

    /**
     * 通过主键删除数据
     * 
     * @param id 留言ID
     * @return 影响行数
     */
    public int deleteMessageById(Long id);

    /**
     * 批量删除留言
     * 
     * @param ids 需要删除的数据ID
     * @return 影响行数
     */
    public int deleteMessageByIds(Long[] ids);
}
