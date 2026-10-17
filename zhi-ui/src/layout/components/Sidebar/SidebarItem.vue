<template>
  <div v-if="!item.hidden">
    <!-- 调试信息 -->
    <div
      v-if="false"
      style="color: red; font-size: 12px; margin-bottom: 10px; padding: 5px; border: 1px solid red"
    >
      <div>菜单标题: {{ (item.meta && item.meta.title) || '无标题' }}</div>
      <div>菜单路径: {{ item.path || '无路径' }}</div>
      <div>是否有子菜单: {{ item.children ? '有' : '无' }}</div>
      <div>子菜单数量: {{ (item.children && item.children.length) || 0 }}</div>
      <div>是否隐藏: {{ item.hidden ? '是' : '否' }}</div>
      <div>hasOneShowingChild结果: {{ hasOneShowingChild(item.children, item) }}</div>
      <div>onlyOneChild: {{ onlyOneChild }}</div>
    </div>

    <template v-if="false && hasOneShowingChild(item.children, item)">
      <div
        class="menu-item"
        :class="{
          'submenu-title-noDropdown': !isNest,
          'no-permission': !hasMenuPermission(onlyOneChild)
        }"
        @click="handleMenuClick(onlyOneChild, $event)"
      >
        <svg-icon
          :icon-class="
            (onlyOneChild.meta && onlyOneChild.meta.icon) || (item.meta && item.meta.icon) || ''
          "
        />
        <span
          class="menu-title"
          :title="hasTitle(onlyOneChild.meta && onlyOneChild.meta.title)"
          :class="{ 'permission-denied': !hasMenuPermission(onlyOneChild) }"
        >
          {{ (onlyOneChild.meta && onlyOneChild.meta.title) || '' }}
          <el-tag
            v-if="!hasMenuPermission(onlyOneChild)"
            size="small"
            type="warning"
            class="permission-tag"
          >
            权限不足
          </el-tag>
        </span>
      </div>
    </template>

    <!-- 简单菜单项 -->
    <el-menu-item
      v-if="item.path && item.meta && item.meta.title && !item.children"
      :index="resolvePath(item.path)"
      :class="{
        'no-permission': !hasMenuPermission(item)
      }"
      @click="handleMenuItemClick(item)"
    >
      <!-- 调试信息 -->
      <div
        v-if="false"
        style="
          color: green;
          font-size: 10px;
          position: absolute;
          right: 20px;
          top: 2px;
          z-index: 100;
        "
      >
        [{{ resolvePath(item.path) }}]
      </div>
      <svg-icon :icon-class="(item.meta && item.meta.icon) || 'documentation'" />
      <template #title>
        <span>{{ (item.meta && item.meta.title) || '' }}</span>
      </template>
    </el-menu-item>

    <el-sub-menu
      v-else-if="item.children && item.children.length > 0"
      ref="subMenu"
      :index="resolvePath(item.path)"
      :popper-class="'sidebar-submenu'"
      :class="{ 'no-permission': !hasMenuPermission(item) }"
    >
      <!-- 调试信息 -->
      <div
        v-if="false"
        style="
          color: blue;
          font-size: 10px;
          position: absolute;
          right: 20px;
          top: 18px;
          z-index: 100;
        "
      >
        [{{ item.meta?.title }}:{{ item.children?.length }}]
      </div>
      <template v-if="item.meta" #title>
        <svg-icon :icon-class="(item.meta && item.meta.icon) || 'documentation'" />
        <span
          class="menu-title"
          :title="hasTitle(item.meta && item.meta.title)"
          :class="{ 'permission-denied': !hasMenuPermission(item) }"
        >
          {{ (item.meta && item.meta.title) || '' }}
          <el-tag
            v-if="!hasMenuPermission(item)"
            size="small"
            type="warning"
            class="permission-tag"
          >
            权限不足
          </el-tag>
        </span>
      </template>

      <sidebar-item
        v-for="(child, index) in item.children || []"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(item.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, type PropType } from 'vue'
import { ElNotification } from '@/plugins/element-plus-service'
import { getNormalPath } from '@/utils/zhi'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

interface SidebarRouteItem {
  path?: string
  hidden?: boolean
  meta?: Record<string, any>
  children?: SidebarRouteItem[]
  [key: string]: any
}

const props = defineProps({
  // route object
  item: {
    type: Object as PropType<SidebarRouteItem>,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

const onlyOneChild = ref<SidebarRouteItem>({})
const subMenu = ref<any>(null)
const router = useRouter()

// 🔥 关键改进2: 优化事件绑定处理
onMounted(() => {
  if (subMenu.value) {
    subMenu.value.updatePopper && subMenu.value.updatePopper()
  }
})

// 🔥 关键改进4: 权限检查函数
function hasMenuPermission(menuItem?: SidebarRouteItem) {
  if (!menuItem || !menuItem.meta) {
    return true
  }

  // 如果meta中明确标记了权限状态
  if (menuItem.meta.hasPermission !== undefined) {
    return menuItem.meta.hasPermission
  }

  // 检查用户角色和权限
  const userStore = useUserStore()
  const roles = userStore.roles || []
  const permissions = userStore.permissions || []

  // 检查角色权限
  if (menuItem.meta?.roles && menuItem.meta.roles.length > 0) {
    const hasRole = menuItem.meta.roles.some((role: string) => roles.includes(role))
    if (!hasRole) {
      return false
    }
  }

  // 检查具体权限
  if (menuItem.meta?.permissions && menuItem.meta.permissions.length > 0) {
    const hasPermission = menuItem.meta.permissions.some((permission: string) =>
      permissions.includes(permission)
    )
    if (!hasPermission) {
      return false
    }
  }
  return true
}

function hasOneShowingChild(children: SidebarRouteItem[] = [], parent: SidebarRouteItem) {
  // 如果没有子菜单或子菜单为空数组，则使用父菜单
  if (!children || !Array.isArray(children) || children.length === 0) {
    onlyOneChild.value = { ...parent }
    return true
  }

  // 查找可见的子菜单
  const showingChildren = children.filter(item => {
    if (!item || item.hidden) return false
    return true
  })

  // 如果只有一个可见的子菜单，使用该子菜单
  if (showingChildren.length === 1) {
    onlyOneChild.value = showingChildren[0]
    return true
  }

  // 如果没有可见的子菜单，使用父菜单
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent }
    return true
  }
  return false
}

// 修复路径解析 - 确保二级菜单路径正确
function resolvePath(routePath?: string) {
  // 如果传入了路由路径
  if (routePath) {
    // 如果已经是完整路径，直接返回
    if (routePath.startsWith('/')) {
      return routePath
    }

    // 拼接基础路径
    const basePath = props.basePath || '/'
    const normalizedBasePath = basePath.endsWith('/') ? basePath.slice(0, -1) : basePath
    const normalizedRoutePath = routePath.startsWith('/') ? routePath : `/${routePath}`

    return `${normalizedBasePath}${normalizedRoutePath}`
  }

  // 如果没有传入路径，使用basePath
  return props.basePath || '/'
}

function hasTitle(title?: string) {
  if (!title) {
    return ''
  }
  if (title.length > 5) {
    return title
  } else {
    return ''
  }
}

// 菜单项点击处理 - 强制路由跳转
function handleMenuItemClick(menuItem: SidebarRouteItem) {
  const targetPath = resolvePath(menuItem.path)

  // 权限检查
  if (!hasMenuPermission(menuItem)) {
    const reason = (menuItem.meta && menuItem.meta.permissionReason) || '权限不足'

    ElNotification({
      title: '访问受限',
      message: `您没有权限访问"${(menuItem.meta && menuItem.meta.title) || ''}"功能。原因：${reason}`,
      type: 'warning',
      duration: 4000,
      showClose: true
    })
    return false
  }

  // 强制路由跳转
  if (targetPath && targetPath !== '/') {
    router.push(targetPath).catch(err => {
      console.error('路由跳转失败:', err)
    })
  }

  return true
}

// 简化的菜单点击处理 - Element Plus原生处理
function handleMenuClick(menuItem: SidebarRouteItem, event?: Event) {
  // 权限检查
  if (!hasMenuPermission(menuItem)) {
    event?.preventDefault()
    event?.stopPropagation()

    const reason = (menuItem.meta && menuItem.meta.permissionReason) || '权限不足'

    ElNotification({
      title: '访问受限',
      message: `您没有权限访问"${(menuItem.meta && menuItem.meta.title) || ''}"功能。原因：${reason}`,
      type: 'warning',
      duration: 4000,
      showClose: true
    })
    return false
  }

  // Element Plus的:router="true"会自动处理路由跳转
  return true
}
</script>

<style scoped>
.permission-denied {
  opacity: 0.6;
  color: #e6a23c;
}

.permission-tag {
  margin-left: 8px;
  font-size: 10px;
}

.no-permission {
  opacity: 0.7;
}

.menu-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.menu-item {
  padding: 0 20px;
  height: 56px;
  line-height: 56px;
  font-size: 14px;
  color: var(--menu-text, #bfcbd9);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.menu-item:hover {
  background-color: var(
    --menu-hover,
    color-mix(in srgb, var(--el-color-primary) 12%, transparent)
  ) !important;
  color: var(--menu-active-text, var(--el-color-primary)) !important;
  transform: translateX(3px);
  transition: all 0.3s ease;
}

.menu-item.is-active {
  color: var(--menu-active-text, var(--el-color-primary));
  background-color: var(
    --menu-hover,
    color-mix(in srgb, var(--el-color-primary) 12%, transparent)
  ) !important;
}

.menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background-color: var(--menu-active-text, var(--el-color-primary));
}

.menu-title {
  margin-left: 10px;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 图标样式 */
.svg-icon {
  font-size: 18px;
  vertical-align: middle;
  margin-right: 5px;
  width: 1em;
  height: 1em;
  display: inline-block;
}

/* 增加点击反馈效果 */
.el-sub-menu__title:hover {
  transition: all 0.3s ease;
}

/* 🔥 关键修复: 确保二级菜单能正常显示和展开 */
.el-sub-menu .el-menu {
  background-color: inherit !important;
}

.el-sub-menu .el-menu-item {
  background-color: var(--submenu-bg, transparent) !important;
  color: var(--submenu-text, #606266) !important;
}

.el-sub-menu .el-menu-item:hover {
  background-color: var(
    --submenu-hover,
    color-mix(in srgb, var(--el-color-primary) 12%, transparent)
  ) !important;
  color: var(--menu-active-text, var(--el-color-primary)) !important;
}

.el-sub-menu .el-menu-item.is-active {
  background-color: var(
    --submenu-active,
    color-mix(in srgb, var(--el-color-primary) 18%, transparent)
  ) !important;
  color: var(--menu-active-text, var(--el-color-primary)) !important;
}

/* 确保子菜单标题可点击 */
.el-sub-menu__title {
  cursor: pointer !important;
  user-select: none !important;
  pointer-events: auto !important;
}

/* 移除可能的遮挡层 */
.sidebar-container .el-menu--collapse .el-sub-menu > .el-menu {
  display: none !important;
}

.sidebar-container .el-menu--collapse .el-sub-menu.is-opened > .el-menu {
  display: block !important;
}

/* 确保嵌套菜单正确显示 */
.nest-menu .el-menu-item,
.nest-menu .el-sub-menu__title {
  padding-left: 40px !important;
  min-height: 50px;
  line-height: 50px;
}
</style>
