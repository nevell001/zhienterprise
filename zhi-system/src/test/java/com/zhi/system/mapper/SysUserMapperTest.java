package com.zhi.system.mapper;

import com.zhi.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 注册链路在 SysUserMapper 上的行为契约。
 *
 * SiteUserService 注册时设置 userType='01'，登录（selectUserByUserName）必须原样读回；
 * 曾经 insertUser 漏了 user_type 列，注册用户落库变成 '00'（系统用户），
 * 前台因此显示「管理后台」入口并把个人中心渲染成管理员界面。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@MapperScan("com.zhi.system.mapper")
@TestPropertySource(properties = {
    "mybatis.mapperLocations=classpath:mapper/system/SysUserMapper.xml",
    "mybatis.typeAliasesPackage=com.zhi.system.domain,com.zhi.common.core.domain.entity",
    "spring.sql.init.mode=always"
})
@Sql(scripts = "/schema-sys-user.sql")
class SysUserMapperTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void insertUserShouldPersistUserType() {
        SysUser siteUser = new SysUser();
        siteUser.setUserName("mapper_site_user");
        siteUser.setNickName("注册用户");
        siteUser.setPassword("$2a$10$abcdefghijklmnopqrstuv");
        siteUser.setStatus("0");
        siteUser.setUserType("01");

        assertEquals(1, sysUserMapper.insertUser(siteUser));
        assertNotNull(siteUser.getUserId(), "插入后应回填自增主键");

        SysUser loaded = sysUserMapper.selectUserByUserName("mapper_site_user");
        assertNotNull(loaded, "登录链路应能按用户名读回用户");
        assertEquals("01", loaded.getUserType(), "注册用户的 user_type 必须原样持久化");
    }

    @Test
    void insertUserShouldDefaultToSystemUserTypeWhenNotProvided() {
        SysUser adminCreated = new SysUser();
        adminCreated.setUserName("mapper_sys_user");
        adminCreated.setNickName("后台创建用户");
        adminCreated.setPassword("$2a$10$abcdefghijklmnopqrstuv");
        adminCreated.setStatus("0");

        assertEquals(1, sysUserMapper.insertUser(adminCreated));

        SysUser loaded = sysUserMapper.selectUserByUserName("mapper_sys_user");
        assertNotNull(loaded);
        assertEquals("00", loaded.getUserType(), "后台不传 userType 时应落到库默认值 00（系统用户）");
    }
}
