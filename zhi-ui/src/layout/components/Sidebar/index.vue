<template>
  <div :class="{ 'has-logo': showLogo }" class="sidebar-container">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="getMenuBackground"
        :text-color="getMenuTextColor"
        :unique-opened="false"
        :active-text-color="theme"
        :collapse-transition="false"
        :router="true"
        mode="vertical"
        :class="sideTheme"
        @select="handleSelect"
        @open="handleOpen"
        @close="handleClose"
      >
        <sidebar-item
          v-for="(sidebarRoute, index) in sidebarRouters.filter(
            r => r.meta && r.meta.title && !r.hidden
          )"
          :key="sidebarRoute.path + index"
          :item="sidebarRoute"
          :base-path="sidebarRoute.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import Logo from './Logo.vue'
import SidebarItem from './SidebarItem.vue'
import { SIDEBAR_THEME } from '@/assets/styles/sidebarTheme'
import { useAppStore } from '@/stores/app'
import { useSettingsStore } from '@/stores/settings'
import { usePermissionStore } from '@/stores/permission'
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()

// 使用动态生成的路由数据
const sidebarRouters = computed(() => {
  return (permissionStore.sidebarRouters || []) as any[]
})
const showLogo = computed(() => settingsStore.sidebarLogo)
const sideTheme = computed(() => settingsStore.sideTheme)
const theme = computed(() => settingsStore.theme)
const isCollapse = computed(() => !appStore.sidebar.opened)

// 获取菜单背景色
const getMenuBackground = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-bg)'
  }
  return sideTheme.value === 'theme-dark' ? SIDEBAR_THEME.menuBg : SIDEBAR_THEME.menuLightBg
})

// 获取菜单文字颜色
const getMenuTextColor = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-text)'
  }
  return sideTheme.value === 'theme-dark' ? SIDEBAR_THEME.menuText : SIDEBAR_THEME.menuLightText
})

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

// 判断是否只有一个子菜单并生成标题
function onlyOneChild(children: any[]) {
  // 确保children是数组
  if (!Array.isArray(children)) {
    return false
  }

  const showChildren = children.filter(item => {
    // 确保item存在且未隐藏
    return item && !item.hidden
  })

  if (showChildren.length === 1) {
    return showChildren[0]
  }
  return false
}

// 处理菜单选择事件
function handleSelect(_key: string, _keyPath: string[]) {
  // Element Plus的:router="true"会自动处理路由跳转
}

// 处理子菜单展开事件
function handleOpen(_key: string, _keyPath: string[]) {
  // 子菜单展开处理
}

// 处理子菜单关闭事件
function handleClose(_key: string, _keyPath: string[]) {
  // 子菜单关闭处理
}

// 组件挂载后执行初始化
onMounted(() => {
  // 初始化完成
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  background-color: v-bind(getMenuBackground);

  .scrollbar-wrapper {
    background-color: v-bind(getMenuBackground);
  }

  .el-menu {
    border: none;
    height: 100%;
    width: 100% !important;

    .el-menu-item,
    .el-sub-menu__title {
      &:hover {
        background-color: var(
          --menu-hover,
          color-mix(in srgb, var(--el-color-primary) 12%, transparent)
        ) !important;
      }
    }

    .el-menu-item {
      color: v-bind(getMenuTextColor);

      &.is-active {
        color: var(--menu-active-text, var(--el-color-primary));
        background-color: var(
          --menu-hover,
          color-mix(in srgb, var(--el-color-primary) 12%, transparent)
        ) !important;
      }
    }

    .el-sub-menu__title {
      color: v-bind(getMenuTextColor);
    }
  }
}
</style>
