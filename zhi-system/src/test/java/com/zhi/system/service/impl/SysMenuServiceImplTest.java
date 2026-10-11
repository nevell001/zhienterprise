package com.zhi.system.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zhi.common.core.domain.TreeSelect;
import com.zhi.common.core.domain.entity.SysMenu;
import com.zhi.common.core.domain.entity.SysRole;
import com.zhi.system.domain.vo.RouterVo;
import com.zhi.system.mapper.SysMenuMapper;
import com.zhi.system.mapper.SysRoleMapper;
import com.zhi.system.mapper.SysRoleMenuMapper;

/**
 * 菜单服务层测试
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper menuMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    @InjectMocks
    private SysMenuServiceImpl menuService;

    private SysMenu testMenu;

    @BeforeEach
    void setUp() {
        testMenu = new SysMenu();
        testMenu.setMenuId(1L);
        testMenu.setMenuName("系统管理");
        testMenu.setParentId(0L);
        testMenu.setOrderNum(1);
        testMenu.setPath("system");
        testMenu.setComponent("Layout");
        testMenu.setIsFrame("1");
        testMenu.setIsCache("0");
        testMenu.setMenuType("M");
        testMenu.setVisible("0");
        testMenu.setStatus("0");
        testMenu.setIcon("system");
    }

    /**
     * 测试查询菜单列表 - 管理员
     */
    @Test
    void testSelectMenuList_Admin() {
        Long userId = 1L;
        List<SysMenu> menuList = Arrays.asList(testMenu);
        when(menuMapper.selectMenuList(any(SysMenu.class))).thenReturn(menuList);

        List<SysMenu> result = menuService.selectMenuList(new SysMenu(), userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("系统管理", result.get(0).getMenuName());
        verify(menuMapper).selectMenuList(any(SysMenu.class));
    }

    /**
     * 测试查询菜单列表 - 普通用户
     */
    @Test
    void testSelectMenuList_NormalUser() {
        Long userId = 2L;
        List<SysMenu> menuList = Arrays.asList(testMenu);
        when(menuMapper.selectMenuListByUserId(any(SysMenu.class))).thenReturn(menuList);

        List<SysMenu> result = menuService.selectMenuList(new SysMenu(), userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(menuMapper).selectMenuListByUserId(any(SysMenu.class));
    }

    /**
     * 测试根据用户ID查询权限
     */
    @Test
    void testSelectMenuPermsByUserId() {
        Long userId = 1L;
        List<String> perms = Arrays.asList("system:user:list,system:user:add", "system:role:list");
        when(menuMapper.selectMenuPermsByUserId(userId)).thenReturn(perms);

        Set<String> result = menuService.selectMenuPermsByUserId(userId);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("system:user:list"));
        assertTrue(result.contains("system:user:add"));
        assertTrue(result.contains("system:role:list"));
        verify(menuMapper).selectMenuPermsByUserId(userId);
    }

    /**
     * 测试根据用户ID查询权限 - 空权限
     */
    @Test
    void testSelectMenuPermsByUserId_Empty() {
        Long userId = 1L;
        when(menuMapper.selectMenuPermsByUserId(userId)).thenReturn(Collections.emptyList());

        Set<String> result = menuService.selectMenuPermsByUserId(userId);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(menuMapper).selectMenuPermsByUserId(userId);
    }

    /**
     * 测试根据角色ID查询权限
     */
    @Test
    void testSelectMenuPermsByRoleId() {
        Long roleId = 1L;
        List<String> perms = Arrays.asList("system:user:list", "system:role:list");
        when(menuMapper.selectMenuPermsByRoleId(roleId)).thenReturn(perms);

        Set<String> result = menuService.selectMenuPermsByRoleId(roleId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("system:user:list"));
        assertTrue(result.contains("system:role:list"));
        verify(menuMapper).selectMenuPermsByRoleId(roleId);
    }

    /**
     * 测试根据角色ID查询权限 - 空权限
     */
    @Test
    void testSelectMenuPermsByRoleId_Empty() {
        Long roleId = 1L;
        when(menuMapper.selectMenuPermsByRoleId(roleId)).thenReturn(Collections.emptyList());

        Set<String> result = menuService.selectMenuPermsByRoleId(roleId);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(menuMapper).selectMenuPermsByRoleId(roleId);
    }

    /**
     * 测试根据角色ID查询菜单树信息
     */
    @Test
    void testSelectMenuListByRoleId() {
        Long roleId = 1L;
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setMenuCheckStrictly(false);

        List<Long> menuIds = Arrays.asList(1L, 2L, 3L);
        when(roleMapper.selectRoleById(roleId)).thenReturn(role);
        when(menuMapper.selectMenuListByRoleId(roleId, false)).thenReturn(menuIds);

        List<Long> result = menuService.selectMenuListByRoleId(roleId);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(roleMapper).selectRoleById(roleId);
        verify(menuMapper).selectMenuListByRoleId(roleId, false);
    }

    /**
     * 测试根据菜单ID查询信息
     */
    @Test
    void testSelectMenuById() {
        when(menuMapper.selectMenuById(1L)).thenReturn(testMenu);

        SysMenu result = menuService.selectMenuById(1L);

        assertNotNull(result);
        assertEquals("系统管理", result.getMenuName());
        verify(menuMapper).selectMenuById(1L);
    }

    /**
     * 测试是否存在菜单子节点 - 有子节点
     */
    @Test
    void testHasChildByMenuId_HasChildren() {
        when(menuMapper.hasChildByMenuId(1L)).thenReturn(1);

        boolean result = menuService.hasChildByMenuId(1L);

        assertTrue(result);
        verify(menuMapper).hasChildByMenuId(1L);
    }

    /**
     * 测试是否存在菜单子节点 - 无子节点
     */
    @Test
    void testHasChildByMenuId_NoChildren() {
        when(menuMapper.hasChildByMenuId(1L)).thenReturn(0);

        boolean result = menuService.hasChildByMenuId(1L);

        assertFalse(result);
        verify(menuMapper).hasChildByMenuId(1L);
    }

    /**
     * 测试查询菜单使用数量 - 存在角色
     */
    @Test
    void testCheckMenuExistRole_Exists() {
        when(roleMenuMapper.checkMenuExistRole(1L)).thenReturn(1);

        boolean result = menuService.checkMenuExistRole(1L);

        assertTrue(result);
        verify(roleMenuMapper).checkMenuExistRole(1L);
    }

    /**
     * 测试查询菜单使用数量 - 不存在角色
     */
    @Test
    void testCheckMenuExistRole_NotExists() {
        when(roleMenuMapper.checkMenuExistRole(1L)).thenReturn(0);

        boolean result = menuService.checkMenuExistRole(1L);

        assertFalse(result);
        verify(roleMenuMapper).checkMenuExistRole(1L);
    }

    /**
     * 测试新增菜单
     */
    @Test
    void testInsertMenu() {
        when(menuMapper.insertMenu(testMenu)).thenReturn(1);

        int result = menuService.insertMenu(testMenu);

        assertEquals(1, result);
        verify(menuMapper).insertMenu(testMenu);
    }

    /**
     * 测试修改菜单
     */
    @Test
    void testUpdateMenu() {
        when(menuMapper.updateMenu(testMenu)).thenReturn(1);

        int result = menuService.updateMenu(testMenu);

        assertEquals(1, result);
        verify(menuMapper).updateMenu(testMenu);
    }

    /**
     * 测试删除菜单
     */
    @Test
    void testDeleteMenuById() {
        when(menuMapper.deleteMenuById(1L)).thenReturn(1);

        int result = menuService.deleteMenuById(1L);

        assertEquals(1, result);
        verify(menuMapper).deleteMenuById(1L);
    }

    /**
     * 测试校验菜单名称唯一性 - 唯一
     */
    @Test
    void testCheckMenuNameUnique_Unique() {
        when(menuMapper.checkMenuNameUnique("系统管理", 0L)).thenReturn(null);

        boolean result = menuService.checkMenuNameUnique(testMenu);

        assertTrue(result);
        verify(menuMapper).checkMenuNameUnique("系统管理", 0L);
    }

    /**
     * 测试校验菜单名称唯一性 - 不唯一
     */
    @Test
    void testCheckMenuNameUnique_NotUnique() {
        SysMenu existingMenu = new SysMenu();
        existingMenu.setMenuId(2L);
        existingMenu.setMenuName("系统管理");

        when(menuMapper.checkMenuNameUnique("系统管理", 0L)).thenReturn(existingMenu);

        boolean result = menuService.checkMenuNameUnique(testMenu);

        assertFalse(result);
        verify(menuMapper).checkMenuNameUnique("系统管理", 0L);
    }

    /**
     * 测试校验菜单名称唯一性 - 同一菜单
     */
    @Test
    void testCheckMenuNameUnique_SameMenu() {
        when(menuMapper.checkMenuNameUnique("系统管理", 0L)).thenReturn(testMenu);

        boolean result = menuService.checkMenuNameUnique(testMenu);

        assertTrue(result);
        verify(menuMapper).checkMenuNameUnique("系统管理", 0L);
    }

    /**
     * 测试构建前端路由 - 简单菜单
     */
    @Test
    void testBuildMenus_SimpleMenu() {
        SysMenu menu = new SysMenu();
        menu.setMenuId(1L);
        menu.setMenuName("用户管理");
        menu.setParentId(1L);
        menu.setPath("user");
        menu.setComponent("system/user/index");
        menu.setIsFrame("1");
        menu.setIsCache("0");
        menu.setMenuType("C");
        menu.setVisible("0");
        menu.setIcon("user");

        List<SysMenu> menus = Arrays.asList(menu);

        List<RouterVo> result = menuService.buildMenus(menus);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("User", result.get(0).getName());
        assertEquals("用户管理", result.get(0).getMeta().getTitle());
    }

    /**
     * 测试构建前端路由 - 空列表
     */
    @Test
    void testBuildMenus_EmptyList() {
        List<SysMenu> menus = Collections.emptyList();

        List<RouterVo> result = menuService.buildMenus(menus);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * 测试构建菜单树 - 简单树
     */
    @Test
    void testBuildMenuTree_SimpleTree() {
        SysMenu parentMenu = new SysMenu();
        parentMenu.setMenuId(1L);
        parentMenu.setMenuName("系统管理");
        parentMenu.setParentId(0L);

        SysMenu childMenu = new SysMenu();
        childMenu.setMenuId(2L);
        childMenu.setMenuName("用户管理");
        childMenu.setParentId(1L);

        List<SysMenu> menus = Arrays.asList(parentMenu, childMenu);

        List<SysMenu> result = menuService.buildMenuTree(menus);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("系统管理", result.get(0).getMenuName());
        assertEquals(1, result.get(0).getChildren().size());
    }

    /**
     * 测试构建菜单树 - 空列表
     */
    @Test
    void testBuildMenuTree_EmptyList() {
        List<SysMenu> menus = Collections.emptyList();

        List<SysMenu> result = menuService.buildMenuTree(menus);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * 测试构建菜单树选择 - 简单树
     */
    @Test
    void testBuildMenuTreeSelect_SimpleTree() {
        SysMenu parentMenu = new SysMenu();
        parentMenu.setMenuId(1L);
        parentMenu.setMenuName("系统管理");
        parentMenu.setParentId(0L);

        SysMenu childMenu = new SysMenu();
        childMenu.setMenuId(2L);
        childMenu.setMenuName("用户管理");
        childMenu.setParentId(1L);

        List<SysMenu> menus = Arrays.asList(parentMenu, childMenu);

        List<TreeSelect> result = menuService.buildMenuTreeSelect(menus);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("系统管理", result.get(0).getLabel());
    }

    /**
     * 测试获取路由名称 - 菜单框架
     */
    @Test
    void testGetRouteName_MenuFrame() {
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setMenuType("C");
        menu.setIsFrame("0");
        menu.setPath("user");
        menu.setRouteName("User");

        String result = menuService.getRouteName(menu);

        assertEquals("User", result);
    }

    /**
     * 测试获取路由名称 - 非菜单框架
     */
    @Test
    void testGetRouteName_NotMenuFrame() {
        SysMenu menu = new SysMenu();
        menu.setParentId(1L);
        menu.setMenuType("C");
        menu.setIsFrame("0");
        menu.setPath("user");
        menu.setRouteName("User");

        String result = menuService.getRouteName(menu);

        assertEquals("User", result);
    }

    /**
     * 测试获取路由名称 - 使用path
     */
    @Test
    void testGetRouteName_UsePath() {
        String result = menuService.getRouteName("", "user");

        assertEquals("User", result);
    }

    /**
     * 测试获取路由地址 - 内链
     */
    @Test
    void testGetRouterPath_InnerLink() {
        SysMenu menu = new SysMenu();
        menu.setParentId(1L);
        menu.setPath("http://www.example.com");
        menu.setIsFrame("1");
        menu.setMenuType("C");

        String result = menuService.getRouterPath(menu);

        assertEquals("example/com", result);
    }

    /**
     * 测试获取路由地址 - 一级目录
     */
    @Test
    void testGetRouterPath_TopLevelDir() {
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setPath("system");
        menu.setIsFrame("1");
        menu.setMenuType("M");

        String result = menuService.getRouterPath(menu);

        assertEquals("/system", result);
    }

    /**
     * 测试获取路由地址 - 菜单框架
     */
    @Test
    void testGetRouterPath_MenuFrame() {
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setPath("user");
        menu.setIsFrame("1");
        menu.setMenuType("C");

        String result = menuService.getRouterPath(menu);

        assertEquals("/", result);
    }

    /**
     * 测试获取组件信息 - 有组件
     */
    @Test
    void testGetComponent_HasComponent() {
        SysMenu menu = new SysMenu();
        menu.setComponent("system/user/index");
        menu.setParentId(1L);
        menu.setIsFrame("1");
        menu.setMenuType("C");

        String result = menuService.getComponent(menu);

        assertEquals("system/user/index", result);
    }

    /**
     * 测试获取组件信息 - 无组件且非菜单框架
     */
    @Test
    void testGetComponent_NoComponent() {
        SysMenu menu = new SysMenu();
        menu.setComponent("");
        menu.setParentId(1L);
        menu.setIsFrame("1");
        menu.setMenuType("C");
        menu.setPath("http://www.example.com");

        String result = menuService.getComponent(menu);

        assertEquals("InnerLink", result);
    }

    /**
     * 测试是否为菜单内部跳转 - 是
     */
    @Test
    void testIsMenuFrame_True() {
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setMenuType("C");
        menu.setIsFrame("1");

        boolean result = menuService.isMenuFrame(menu);

        assertTrue(result);
    }

    /**
     * 测试是否为菜单内部跳转 - 否
     */
    @Test
    void testIsMenuFrame_False() {
        SysMenu menu = new SysMenu();
        menu.setParentId(1L);
        menu.setMenuType("C");
        menu.setIsFrame("1");

        boolean result = menuService.isMenuFrame(menu);

        assertFalse(result);
    }

    /**
     * 测试是否为内链组件 - 是
     */
    @Test
    void testIsInnerLink_True() {
        SysMenu menu = new SysMenu();
        menu.setIsFrame("1");
        menu.setPath("http://www.example.com");

        boolean result = menuService.isInnerLink(menu);

        assertTrue(result);
    }

    /**
     * 测试是否为内链组件 - 否
     */
    @Test
    void testIsInnerLink_False() {
        SysMenu menu = new SysMenu();
        menu.setIsFrame("1");
        menu.setPath("user");

        boolean result = menuService.isInnerLink(menu);

        assertFalse(result);
    }

    /**
     * 测试是否为parent_view组件 - 是
     */
    @Test
    void testIsParentView_True() {
        SysMenu menu = new SysMenu();
        menu.setParentId(1L);
        menu.setMenuType("M");

        boolean result = menuService.isParentView(menu);

        assertTrue(result);
    }

    /**
     * 测试是否为parent_view组件 - 否
     */
    @Test
    void testIsParentView_False() {
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setMenuType("M");

        boolean result = menuService.isParentView(menu);

        assertFalse(result);
    }

    /**
     * 测试获取子节点权限
     */
    @Test
    void testGetChildPerms() {
        SysMenu parentMenu = new SysMenu();
        parentMenu.setMenuId(1L);
        parentMenu.setParentId(0L);

        SysMenu childMenu = new SysMenu();
        childMenu.setMenuId(2L);
        childMenu.setParentId(1L);

        List<SysMenu> menus = Arrays.asList(parentMenu, childMenu);

        List<SysMenu> result = menuService.getChildPerms(menus, 0);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getMenuId());
    }

    /**
     * 测试内链域名特殊字符替换
     */
    @Test
    void testInnerLinkReplaceEach() {
        String path = "http://www.example.com:8080";

        String result = menuService.innerLinkReplaceEach(path);

        assertEquals("example/com/8080", result);
    }

    private SysMenu menu(long menuId, long parentId, String type, String name, String path, String component) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setMenuType(type);
        menu.setMenuName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIsFrame("1");
        menu.setIsCache("0");
        menu.setVisible("0");
        menu.setIcon("icon");
        return menu;
    }

    private SysMenu childOf(SysMenu parent, SysMenu... children) {
        parent.setChildren(new ArrayList<>(Arrays.asList(children)));
        return parent;
    }

    /**
     * 一级目录的跳转重定向按菜单路径匹配，改名后仍然生效（曾因按菜单名判断而静默失效）。
     */
    @Test
    void topLevelDirectoryRedirectsAreKeyedByPath() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("admin/system", "/admin/system/user");
        expected.put("admin/monitor", "/admin/monitor/online");
        expected.put("admin/tool", "/admin/tool/gen");
        expected.put("admin/blog", "/admin/blog/article");
        expected.put("admin/statistics", "/admin/statistics/overview");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            SysMenu dir = menu(1L, 0L, "M", "任意名称", entry.getKey(), "Layout");
            childOf(dir, menu(2L, 1L, "C", "子菜单", "child", "some/index"));

            List<RouterVo> routers = menuService.buildMenus(Arrays.asList(dir));

            assertEquals(entry.getValue(), routers.get(0).getRedirect(),
                    "路径 " + entry.getKey() + " 应重定向到固定首子页");
            assertEquals("/" + entry.getKey(), routers.get(0).getPath());
        }
    }

    /**
     * 不在重定向清单里的一级目录会清空自身 meta，并合成一个空路径子路由。
     * 这是当前实现的行为，此处锁定它，避免被误当成回归。
     */
    @Test
    void topLevelDirectoryOutsideRedirectListLosesMetaAndGetsSyntheticChild() {
        SysMenu dir = menu(1L, 0L, "M", "自定义模块", "custom", "Layout");

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(dir));

        RouterVo router = routers.get(0);
        assertNull(router.getMeta(), "非清单内的一级目录 meta 被置空");
        assertEquals(1, router.getChildren().size());
        RouterVo synthetic = router.getChildren().get(0);
        assertEquals("", synthetic.getPath());
        assertEquals("Layout", synthetic.getComponent());
        assertEquals("Custom", synthetic.getName());
        assertEquals("自定义模块", synthetic.getMeta().getTitle());
    }

    @Test
    void topLevelDirectoryWithChildrenUsesChildrenAsRoutes() {
        SysMenu dir = menu(1L, 0L, "M", "站点管理", "admin/blog", "Layout");
        SysMenu article = menu(2L, 1L, "C", "文章管理", "article", "blog/article/index");
        SysMenu category = menu(3L, 1L, "C", "分类管理", "category", "blog/category/index");
        childOf(dir, article, category);

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(dir));

        RouterVo router = routers.get(0);
        assertEquals(2, router.getChildren().size(), "子菜单必须覆盖合成路由，而不是只剩合成路由");
        assertEquals("文章管理", router.getChildren().get(0).getMeta().getTitle());
        assertEquals("category", router.getChildren().get(1).getPath());
    }

    /** 一级菜单（类型 C）即 menu frame：自身作为容器，子菜单挂成 children。 */
    @Test
    void topLevelMenuFrameWrapsItsChildren() {
        SysMenu frame = menu(1L, 0L, "C", "首页", "index", "home/index");
        childOf(frame, menu(2L, 1L, "C", "子页", "sub", "home/sub/index"));

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(frame));

        RouterVo router = routers.get(0);
        assertEquals("/", router.getPath(), "menu frame 的路由固定为 /");
        assertEquals("", router.getName(), "menu frame 不参与命名，避免路由名冲突");
        assertEquals("Layout", router.getComponent(), "frame 自身组件回退为 Layout");
        assertEquals(Boolean.FALSE, router.getAlwaysShow());
        assertEquals("noRedirect", router.getRedirect());
        assertEquals(1, router.getChildren().size());
    }

    /** 非一级的目录（M）带子菜单时是 parent_view，且必须把 children 带上。 */
    @Test
    void nestedDirectoryBecomesParentView() {
        SysMenu nested = menu(11L, 1L, "M", "多级目录", "nested", "");
        childOf(nested, menu(12L, 11L, "C", "叶子", "leaf", "a/b/index"));

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(nested));

        RouterVo router = routers.get(0);
        assertEquals("ParentView", router.getComponent());
        assertEquals(Boolean.TRUE, router.getAlwaysShow());
        assertEquals("noRedirect", router.getRedirect());
        assertEquals(1, router.getChildren().size());
    }

    /** 内链菜单（isFrame=1 非外链且 path 是 http(s) 地址）要把协议/域名改写成路径片段。 */
    @Test
    void innerLinkMenuRewritesPathAndUsesInnerLinkComponent() {
        SysMenu inner = menu(21L, 1L, "C", "外部文档", "https://www.doc.com/guide", "");

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(inner));

        RouterVo router = routers.get(0);
        assertEquals("doc/com/guide", router.getPath());
        assertEquals("InnerLink", router.getComponent());
    }

    @Test
    void leafMenuKeepsItsOwnComponentAndHiddenFlagFollowsVisible() {
        SysMenu visible = menu(31L, 1L, "C", "列表", "list", "blog/list/index");
        SysMenu hidden = menu(32L, 1L, "C", "详情", "detail", "blog/detail/index");
        hidden.setVisible("1");

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(visible, hidden));

        assertEquals(2, routers.size());
        assertFalse(routers.get(0).getHidden());
        assertTrue(routers.get(1).getHidden());
        assertEquals("blog/list/index", routers.get(0).getComponent());
        assertNull(routers.get(0).getChildren(), "叶子菜单不应被合成 children");
    }

    /** 缓存与 query 透传给前端，isCache=1 才认为需要缓存。 */
    @Test
    void cacheFlagAndQueryArePassedThrough() {
        SysMenu cached = menu(41L, 1L, "C", "字典", "dict", "system/dict/index");
        cached.setIsCache("1");
        cached.setQuery("{\"id\":1}");

        List<RouterVo> routers = menuService.buildMenus(Arrays.asList(cached));

        assertTrue(routers.get(0).getMeta().isNoCache());
        assertEquals("{\"id\":1}", routers.get(0).getQuery());
    }
}