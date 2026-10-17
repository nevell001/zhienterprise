<template>
  <div class="wscn-http404-container" :class="templateClass" :style="themeStyle">
    <div class="wscn-http404">
      <div class="pic-404">
        <img class="pic-404__parent" src="@/assets/404_images/404.png" alt="404" />
        <img class="pic-404__child left" src="@/assets/404_images/404_cloud.png" alt="404" />
        <img class="pic-404__child mid" src="@/assets/404_images/404_cloud.png" alt="404" />
        <img class="pic-404__child right" src="@/assets/404_images/404_cloud.png" alt="404" />
      </div>
      <div class="bullshit">
        <div class="bullshit__oops">404错误!</div>
        <div class="bullshit__headline">
          {{ message }}
        </div>
        <div class="bullshit__info">
          对不起，您正在寻找的页面不存在。尝试检查URL的错误，然后按浏览器上的刷新按钮或尝试在我们的应用程序中找到其他内容。
        </div>
        <router-link to="/index" class="bullshit__return-home">返回首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteSettingsAnonymous } from '@/api/site/setting'
import { getSiteTemplate } from '@/config/siteTemplates'
import { readableOnPrimary } from '@/utils/theme'

const message = computed(() => {
  return '找不到网页！'
})

/**
 * 404 不在 SiteLayout 内（没有导航/页脚，也不挂 <html> 模板类），
 * 这里单独读取当前行业模板，把主色收敛到本页的 --el-color-primary，
 * 避免所有模板下 404 都显示全局默认蓝。
 * 直接打开死链时 store 可能尚未加载，回退默认模板（consult，与前台一致）。
 */
const siteSettingsStore = useSiteSettingsStore()
const currentTemplate = computed(() =>
  getSiteTemplate(siteSettingsStore.siteSettings?.site_template)
)
const templateClass = computed(() => currentTemplate.value.className)
// 公开设置现在会下发主题色：优先用主题色（后台「界面主题」），
// 没有主题色时回退当前行业模板主色。
const themeStyle = computed(() => {
  const themed = String(siteSettingsStore.siteSettings.theme_color || '').trim()
  const color = themed || currentTemplate.value.color
  return {
    '--el-color-primary': color,
    '--el-color-primary-light-3': color,
    '--mo-on-primary': readableOnPrimary(color)
  }
})

// 404 不在 SiteLayout 内，直接打开死链时 store 里没有站点设置（只有默认 consult）。
// 这里补一次公开设置请求，保证 404 的主色与真实模板一致；失败则沿用默认，不影响展示。
onMounted(async () => {
  try {
    const response = await getSiteSettingsAnonymous()
    const settings = (response as { data?: Record<string, unknown> })?.data
    if (settings) siteSettingsStore.updateSiteSettings(settings)
  } catch {
    // 静默失败：404 页不因设置请求失败而白屏
  }
})
</script>

<style lang="scss" scoped>
.wscn-http404-container {
  transform: translate(-50%, -50%);
  position: absolute;
  top: 40%;
  left: 50%;
}
.wscn-http404 {
  position: relative;
  width: 1200px;
  padding: 0 50px;
  overflow: hidden;
  .pic-404 {
    position: relative;
    float: left;
    width: 600px;
    overflow: hidden;
    &__parent {
      width: 100%;
    }
    &__child {
      position: absolute;
      &.left {
        width: 80px;
        top: 17px;
        left: 220px;
        opacity: 0;
        animation-name: cloudLeft;
        animation-duration: 2s;
        animation-timing-function: linear;
        animation-fill-mode: forwards;
        animation-delay: 1s;
      }
      &.mid {
        width: 46px;
        top: 10px;
        left: 420px;
        opacity: 0;
        animation-name: cloudMid;
        animation-duration: 2s;
        animation-timing-function: linear;
        animation-fill-mode: forwards;
        animation-delay: 1.2s;
      }
      &.right {
        width: 62px;
        top: 100px;
        left: 500px;
        opacity: 0;
        animation-name: cloudRight;
        animation-duration: 2s;
        animation-timing-function: linear;
        animation-fill-mode: forwards;
        animation-delay: 1s;
      }
      @keyframes cloudLeft {
        0% {
          top: 17px;
          left: 220px;
          opacity: 0;
        }
        20% {
          top: 33px;
          left: 188px;
          opacity: 1;
        }
        80% {
          top: 81px;
          left: 92px;
          opacity: 1;
        }
        100% {
          top: 97px;
          left: 60px;
          opacity: 0;
        }
      }
      @keyframes cloudMid {
        0% {
          top: 10px;
          left: 420px;
          opacity: 0;
        }
        20% {
          top: 40px;
          left: 360px;
          opacity: 1;
        }
        70% {
          top: 130px;
          left: 180px;
          opacity: 1;
        }
        100% {
          top: 160px;
          left: 120px;
          opacity: 0;
        }
      }
      @keyframes cloudRight {
        0% {
          top: 100px;
          left: 500px;
          opacity: 0;
        }
        20% {
          top: 120px;
          left: 460px;
          opacity: 1;
        }
        80% {
          top: 180px;
          left: 340px;
          opacity: 1;
        }
        100% {
          top: 200px;
          left: 300px;
          opacity: 0;
        }
      }
    }
  }
  .bullshit {
    position: relative;
    float: left;
    width: 300px;
    padding: 30px 0;
    overflow: hidden;
    &__oops {
      font-size: 32px;
      font-weight: bold;
      line-height: 40px;
      /* 主题令牌：深色模式下 EP primary 会自动提亮，避免写死浅色 */
      color: var(--el-color-primary);
      opacity: 0;
      margin-bottom: 20px;
      animation-name: slideUp;
      animation-duration: 0.5s;
      animation-fill-mode: forwards;
    }
    &__headline {
      font-size: 20px;
      line-height: 24px;
      color: var(--el-text-color-primary);
      font-weight: bold;
      opacity: 0;
      margin-bottom: 10px;
      animation-name: slideUp;
      animation-duration: 0.5s;
      animation-delay: 0.1s;
      animation-fill-mode: forwards;
    }
    &__info {
      font-size: 13px;
      line-height: 21px;
      color: var(--el-text-color-regular);
      opacity: 0;
      margin-bottom: 30px;
      animation-name: slideUp;
      animation-duration: 0.5s;
      animation-delay: 0.2s;
      animation-fill-mode: forwards;
    }
    &__return-home {
      display: block;
      float: left;
      width: 110px;
      height: 36px;
      background: var(--el-color-primary);
      border-radius: 100px;
      text-align: center;
      color: var(--mo-on-primary, #fff);
      opacity: 0;
      font-size: 14px;
      line-height: 36px;
      cursor: pointer;
      animation-name: slideUp;
      animation-duration: 0.5s;
      animation-delay: 0.3s;
      animation-fill-mode: forwards;
    }
    @keyframes slideUp {
      0% {
        transform: translateY(60px);
        opacity: 0;
      }
      100% {
        transform: translateY(0);
        opacity: 1;
      }
    }
  }
}
</style>
