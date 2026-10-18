package com.zhi.system.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.zhi.common.core.domain.entity.SysUser;
import com.zhi.system.domain.MediaReference;

/**
 * 用户表 数据层
 * 
 * @author ruoyi
 */
public interface SysUserMapper
{
    /**
     * 根据条件分页查询用户列表
     * 
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    /**
     * 查询启用状态的后台系统用户（站内通知、邮件通知的接收人）
     *
     * <p>不带数据权限：由访客触发的通知没有登录上下文，不能走 {@code @DataScope}。</p>
     *
     * @return 用户信息集合
     */
    public List<SysUser> selectEnabledSystemUsers();

    /**
     * 根据条件分页查询已配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    public SysUser selectUserById(Long userId);

    /**
     * 新增用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    /**
     * 修改用户头像
     *
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 修改用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 结果
     */
    public int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 更新用户登录信息（IP和登录时间）
     *
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginDate 登录时间
     * @return 结果
     */
    public int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp, @Param("loginDate") Date loginDate);

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     * 
     * @param userName 用户名称
     * @return 结果
     */
    public SysUser checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);

    /**
     * 查询用户数量
     * 
     * @param user 查询条件
     * @return 用户数量
     */
    Long selectUserCount(SysUser user);

    /**
     * 获取活跃用户数量（最近N天登录的用户）
     * 
     * @param days 天数
     * @return 活跃用户数量
     */
    Long selectActiveUserCount(Integer days);

    /**
     * 获取新增用户数量（最近N天注册的用户）
     * 
     * @param days 天数
     * @return 新增用户数量
     */
    Long selectNewUserCount(Integer days);

    /**
     * 获取管理员用户数量
     * 
     * @return 管理员数量
     */
    Long selectAdminUserCount();

    /**
     * 获取在线用户数量（最近指定分钟内有登录记录的用户）
     *
     * @param minutes 分钟数
     * @return 在线用户数量
     */
    Long selectOnlineUserCount(Integer minutes);

    /**
     * 获取用户注册趋势（按月份统计）
     *
     * @param months 统计最近几个月
     * @return 用户注册趋势数据
     */
    List<Map<String, Object>> selectUserRegisterTrend(@Param("months") Integer months);

    /**
     * 获取用户角色分布
     *
     * @return 用户角色分布数据
     */
    List<Map<String, Object>> selectUserRoleDistribution();

    /**
     * 查询用该文件作头像的未删除用户（媒体库删除前检查）
     *
     * @param keyword 文件名
     * @return 引用位置
     */
    List<MediaReference> selectMediaReferences(@Param("keyword") String keyword);
}
