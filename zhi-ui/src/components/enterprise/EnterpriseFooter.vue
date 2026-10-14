<template>
  <footer class="ent-footer">
    <div class="eh-wrap">
      <div class="fgrid">
        <div class="fbrand">
          <div class="flogo">
            <svg width="26" height="26" viewBox="0 0 26 26" fill="none" aria-hidden="true">
              <rect
                x="1.5"
                y="1.5"
                width="23"
                height="23"
                rx="6"
                stroke="currentColor"
                stroke-width="1.5"
              />
              <path
                d="M7 13h12M13 7v12"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
              />
            </svg>
            {{ blogSettings.blog_name || '知企建站系统' }}
          </div>
          <p class="fdesc">{{ blogSettings.blog_desc || '' }}</p>
          <p v-if="isBeianEnabled && blogSettings.blog_beian" class="fbeian">
            <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">
              {{ blogSettings.blog_beian }}
            </a>
          </p>
        </div>

        <div class="fcol">
          <h4>快速导航</h4>
          <router-link to="/">首页</router-link>
          <router-link to="/business">{{ businessLabel }}</router-link>
          <router-link to="/news">新闻动态</router-link>
          <router-link to="/about">关于我们</router-link>
        </div>

        <div class="fcol">
          <h4>合作</h4>
          <router-link to="/contact">咨询合作</router-link>
          <router-link v-if="isGuestbookEnabled" to="/guestbook">留言板</router-link>
          <a
            v-if="blogSettings.github_url"
            :href="formatUrl(blogSettings.github_url)"
            target="_blank"
            rel="noopener noreferrer"
          >
            GitHub
          </a>
          <a :href="rssUrl" target="_blank" rel="noopener noreferrer">RSS 订阅</a>
        </div>

        <div class="fcol">
          <h4>联系我们</h4>
          <span v-if="blogSettings.contact_address">{{ blogSettings.contact_address }}</span>
          <a
            v-if="blogSettings.contact_phone"
            :href="`tel:${blogSettings.contact_phone.replace(/\s/g, '')}`"
          >
            {{ blogSettings.contact_phone }}
          </a>
          <a v-if="blogSettings.blog_email" :href="`mailto:${blogSettings.blog_email}`">
            {{ blogSettings.blog_email }}
          </a>
          <span v-if="!hasContactInfo">通过「联系我们」页面获取联系方式</span>
        </div>
      </div>

      <div class="fbot">
        <span>© {{ currentYear }} {{ blogSettings.blog_name || '知企建站系统' }} 保留所有权利</span>
        <span class="fbot-links">
          <router-link to="/agreement">法律声明</router-link>
          <router-link to="/privacy">隐私政策</router-link>
        </span>
      </div>
    </div>

    <!-- 返回顶部：滚动超过 600px 滑入（方案文档 §1.2） -->
    <transition name="totop">
      <button v-if="showToTop" type="button" class="to-top" aria-label="返回顶部" @click="toTop">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path
            d="M8 13V3M4 7l4-4 4 4"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </transition>
  </footer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { getApiBaseUrl } from '@/utils/index'

const blogSettingsStore = useBlogSettingsStore()
const blogSettings = computed(() => blogSettingsStore.blogSettings)
const businessLabel = computed(
  () => String(blogSettings.value.business_label || '').trim() || '核心业务'
)
const isGuestbookEnabled = computed(() => blogSettingsStore.isFeatureEnabled('guestbook_enabled'))
const isBeianEnabled = computed(() => blogSettingsStore.isFeatureEnabled('beian_enabled'))
const currentYear = computed(() => new Date().getFullYear())
const hasContactInfo = computed(
  () =>
    Boolean(blogSettings.value.contact_address) ||
    Boolean(blogSettings.value.contact_phone) ||
    Boolean(blogSettings.value.blog_email)
)

const rssUrl = computed(() => {
  const base = getApiBaseUrl().replace(/\/$/, '')
  return `${base}/blog/rss`
})

const formatUrl = (url?: string) => {
  if (!url) return '#'
  return /^https?:\/\//.test(url) ? url : `https://${url}`
}

/* ---------- 返回顶部 ---------- */
const showToTop = ref(false)
const onScroll = () => {
  showToTop.value = window.scrollY > 600
}
const toTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.ent-footer {
  background: var(--tpl-dark);
  color: var(--tpl-dark-dim);
  padding-bottom: var(--mo-sp-8);
  margin-top: var(--mo-sp-12);
}

.eh-wrap {
  max-width: var(--tpl-grid);
  margin: 0 auto;
  padding: 0 var(--mo-sp-7);
}

.fgrid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1.4fr;
  gap: var(--mo-sp-11);
  padding: var(--mo-sp-11) 0;
}

.flogo {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
  color: var(--tpl-white);
  font-weight: 600;
  font-size: var(--mo-fs-md);
  margin-bottom: var(--mo-sp-4);
}

.fdesc {
  font-size: var(--mo-fs-sm);
  max-width: 280px;
  line-height: 1.7;
  margin: 0;
}

.fbeian {
  font-size: var(--mo-fs-sm);
  margin: var(--mo-sp-4) 0 0;
  opacity: 0.6;
}

.fbeian a {
  color: inherit;
  text-decoration: none;
}

.fcol {
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-3);
}

.fcol h4 {
  color: var(--tpl-white);
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  margin: 0 0 var(--mo-sp-2);
}

.fcol a,
.fcol span {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-dark-dim);
  text-decoration: none;
}

.fcol a:hover {
  color: var(--tpl-white);
}

.fbot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--mo-sp-4);
  padding-top: var(--mo-sp-6);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  font-size: var(--mo-fs-sm);
}

.fbot-links {
  display: flex;
  gap: var(--mo-sp-6);
}

.fbot a {
  color: inherit;
  text-decoration: none;
}

.fbot a:hover {
  color: var(--tpl-white);
}

/* 返回顶部 */
.to-top {
  position: fixed;
  right: var(--mo-sp-6);
  bottom: var(--mo-sp-6);
  width: 44px;
  height: 44px;
  border: 1px solid var(--tpl-border);
  border-radius: 999px;
  background: var(--tpl-bg);
  color: var(--tpl-ink-700);
  cursor: pointer;
  display: grid;
  place-items: center;
  box-shadow: var(--tpl-sh-md);
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    color 0.2s;
}

.to-top:hover {
  transform: translateY(-3px);
  color: var(--tpl-p-text);
}

.totop-enter-active,
.totop-leave-active {
  transition:
    opacity 0.25s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.totop-enter-from,
.totop-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@media (max-width: 991px) {
  .eh-wrap {
    padding: 0 var(--mo-sp-5);
  }

  .fgrid {
    grid-template-columns: 1fr 1fr;
    gap: var(--mo-sp-7);
  }
}

@media (max-width: 639px) {
  .fgrid {
    grid-template-columns: 1fr;
    gap: var(--mo-sp-6);
  }
}
</style>
