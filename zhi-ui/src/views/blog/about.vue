<template>
  <BlogLayout>
    <div class="about-page mo-about-page">
      <div class="about-shell">
        <header class="about-panel about-hero">
          <div class="about-hero-copy">
            <span class="section-label">About Us</span>
            <h1 class="about-name">
              {{ blogSettings.blog_name || '关于我们' }}
            </h1>
            <p class="about-desc">
              {{ blogSettings.blog_desc || '以专业的产品与服务，持续为客户创造价值。' }}
            </p>
          </div>

          <div class="about-stat-strip" aria-label="站点统计">
            <div v-for="item in statItems" :key="item.label" class="stat-item">
              <div class="stat-num">
                {{ item.value }}
              </div>
              <div class="stat-label">
                {{ item.label }}
              </div>
            </div>
          </div>
        </header>

        <section class="about-panel about-content-panel">
          <div class="section-head">
            <span class="section-label">Company</span>
            <h2>关于我们</h2>
          </div>
          <div class="about-content" v-html="aboutContentHtml"></div>
        </section>

        <section class="about-panel contact-section">
          <div class="section-head">
            <span class="section-label">Contact</span>
            <h2>联系我们</h2>
          </div>

          <div v-if="contactItems.length" class="contact-grid">
            <div v-for="item in contactItems" :key="item.key" class="contact-card">
              <div class="contact-icon" aria-hidden="true">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="contact-info">
                <div class="contact-type">
                  {{ item.label }}
                </div>
                <a
                  v-if="item.href"
                  :href="item.href"
                  target="_blank"
                  rel="noopener"
                  class="contact-value"
                >
                  {{ item.value }}
                </a>
                <div v-else class="contact-value">{{ item.value }}</div>
              </div>
            </div>
          </div>
          <p v-else class="contact-empty">暂无公开联系方式</p>
        </section>
      </div>
    </div>
  </BlogLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Component } from 'vue'
import BlogLayout from '@/components/BlogLayout.vue'
import { getBlogSettingsAnonymous } from '@/api/blog/setting'
import { getPublishedPageBySlug } from '@/api/blog/page'
import { getPublicStatisticsOverview } from '@/api/statistics'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { renderMarkdown } from '@/utils/markdown'
import { logger } from '@/utils/logger'
import {
  Link as LinkIcon,
  Location,
  Message,
  Phone,
  Platform,
  Promotion
} from '@element-plus/icons-vue'

const blogSettingsStore = useBlogSettingsStore()
const blogSettings = computed(() => blogSettingsStore.blogSettings)

// 页面内容仅来自页面管理里 slug=about 的已发布页面；不存在时显示空态（导航入口亦随之隐藏）
const customAboutContent = ref('')

const aboutContentHtml = computed(() => {
  const raw = customAboutContent.value
  if (!raw || typeof raw !== 'string' || !raw.trim()) return '暂无关于内容'
  return sanitizeArticleContent(renderMarkdown(raw))
})

interface BlogStats {
  articleCount?: number
  categoryCount?: number
  tagCount?: number
  commentCount?: number
  totalViews?: number
}

const stats = ref<BlogStats>({})

interface ContactItem {
  key: string
  label: string
  value: string
  href?: string
  icon: Component
}

const statItems = computed(() => [
  { label: '篇文章', value: formatNumber(stats.value.articleCount || 0) },
  { label: '个分类', value: formatNumber(stats.value.categoryCount || 0) },
  { label: '个标签', value: formatNumber(stats.value.tagCount || 0) },
  { label: '条评论', value: formatNumber(stats.value.commentCount || 0) },
  { label: '总访问', value: formatNumber(stats.value.totalViews || 0) }
])

const contactItems = computed<ContactItem[]>(() => {
  const items: ContactItem[] = []

  // 企业联系方式是一等公民（contact_phone/contact_address），位置信息回退到 author_location
  if (blogSettings.value.contact_phone) {
    items.push({
      key: 'phone',
      label: '电话',
      value: blogSettings.value.contact_phone,
      href: `tel:${blogSettings.value.contact_phone.replace(/\s/g, '')}`,
      icon: Phone
    })
  }

  if (blogSettings.value.blog_email) {
    items.push({
      key: 'email',
      label: '邮箱',
      value: blogSettings.value.blog_email,
      href: `mailto:${blogSettings.value.blog_email}`,
      icon: Message
    })
  }

  if (blogSettings.value.contact_address || blogSettings.value.author_location) {
    items.push({
      key: 'address',
      label: '地址',
      value: blogSettings.value.contact_address || blogSettings.value.author_location,
      icon: Location
    })
  }

  if (blogSettings.value.github_url) {
    items.push({
      key: 'github',
      label: 'GitHub',
      value: blogSettings.value.github_url,
      href: formatUrl(blogSettings.value.github_url),
      icon: Platform
    })
  }

  if (blogSettings.value.weibo_url) {
    items.push({
      key: 'weibo',
      label: '微博',
      value: blogSettings.value.weibo_url,
      href: formatUrl(blogSettings.value.weibo_url),
      icon: Promotion
    })
  }

  if (blogSettings.value.personal_website) {
    items.push({
      key: 'website',
      label: '个人网站',
      value: blogSettings.value.personal_website,
      href: formatUrl(blogSettings.value.personal_website),
      icon: LinkIcon
    })
  }

  return items
})

const loadBlogSettings = async () => {
  try {
    const response = await getBlogSettingsAnonymous()
    const settings = response?.data || {}
    blogSettingsStore.updateBlogSettings(settings)
  } catch (error) {
    logger.error('加载站点设置失败:', error)
  }
}

const loadCustomAbout = async () => {
  try {
    const response = await getPublishedPageBySlug('about')
    customAboutContent.value = response?.data?.content || ''
  } catch {
    // slug=about 的页面不存在或未发布：保持空，页面显示空态
    customAboutContent.value = ''
  }
}

const loadStats = async () => {
  try {
    const response = await getPublicStatisticsOverview()
    const data = response?.data || {}
    stats.value = {
      articleCount: data.articleCount || 0,
      categoryCount: data.categoryCount || 0,
      tagCount: data.tagCount || 0,
      commentCount: data.commentCount || 0,
      totalViews: data.totalViews || 0
    }
  } catch (error) {
    logger.error('加载统计数据失败:', error)
    stats.value = { articleCount: 0, categoryCount: 0, tagCount: 0, commentCount: 0, totalViews: 0 }
  }
}

const formatNumber = (num: number) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'W'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

const formatUrl = (url: string) => {
  if (!url) return ''
  if (typeof url !== 'string') return url
  if (!url.startsWith('http://') && !url.startsWith('https://')) return 'https://' + url
  return url
}

onMounted(() => {
  loadBlogSettings()
  loadCustomAbout()
  loadStats()
})
</script>

<style scoped>
.mo-about-page {
  min-height: 100vh;
  padding: 84px 24px 72px;
  background: var(--mo-n50);
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
}

.about-shell {
  width: min(960px, 100%);
  margin: 0 auto;
  display: grid;
  gap: var(--mo-sp-5);
}

.about-panel {
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: #fff;
  box-shadow: var(--mo-shadow-sm);
}

.about-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 28px;
  padding: var(--mo-sp-7);
}

.about-avatar-wrap {
  width: 128px;
  height: 128px;
}

.about-avatar-wrap img {
  width: 100%;
  height: 100%;
  border: 1px solid var(--mo-p200);
  border-radius: var(--mo-r-full);
  background: var(--mo-p50);
  object-fit: cover;
}

.about-hero-copy {
  min-width: 0;
}

.section-label {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 4px 10px;
  border-radius: var(--mo-r-full);
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 700;
  letter-spacing: 0;
}

.about-name {
  margin: 0 0 var(--mo-sp-2);
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
}

.about-role {
  margin: 0 0 14px;
  color: var(--mo-p700);
  font-size: 15px;
  font-weight: 600;
}

.about-desc {
  max-width: 560px;
  margin: 0;
  color: var(--mo-n600);
  font-size: 15px;
  line-height: 1.8;
}

.about-stat-strip {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-md);
  background: var(--mo-n200);
}

.stat-item {
  min-width: 0;
  padding: var(--mo-sp-4) var(--mo-sp-3);
  background: var(--mo-n50);
  text-align: center;
}

.stat-num {
  color: var(--mo-p700);
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  margin-top: 7px;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
}

.about-content-panel,
.contact-section {
  padding: 28px 32px 32px;
}

.section-head {
  margin-bottom: var(--mo-sp-5);
}

.section-head h2 {
  margin: 0;
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-size: var(--mo-fs-2xl);
  font-weight: 700;
}

.about-content {
  color: var(--mo-n700);
  font-family: var(--mo-font-serif);
  font-size: var(--mo-fs-md);
  line-height: 1.9;
}

.about-content :deep(h1),
.about-content :deep(h2),
.about-content :deep(h3) {
  margin: 28px 0 14px;
  color: var(--mo-n900);
  font-family: var(--mo-font-serif);
  font-weight: 700;
  line-height: 1.35;
}

.about-content :deep(p) {
  margin: 0 0 var(--mo-sp-4);
}

.about-content :deep(a) {
  color: var(--mo-p700);
  text-decoration: none;
}

.about-content :deep(a:hover) {
  color: var(--mo-p800);
  text-decoration: underline;
}

.about-content :deep(ul),
.about-content :deep(ol) {
  padding-left: 22px;
}

.contact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--mo-sp-3);
}

.contact-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: var(--mo-sp-4);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-md);
  background: var(--mo-n50);
  transition:
    border-color 0.2s,
    background 0.2s;
}

.contact-card:hover {
  border-color: var(--mo-p200);
  background: var(--mo-p50);
}

.contact-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  border-radius: var(--mo-r-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: var(--mo-p600);
  box-shadow: var(--mo-shadow-sm);
}

.contact-icon .el-icon {
  font-size: var(--mo-fs-lg);
}

.contact-info {
  min-width: 0;
}

.contact-type {
  margin-bottom: 3px;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
}

.contact-value {
  display: block;
  overflow: hidden;
  color: var(--mo-n800);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

a.contact-value {
  color: var(--mo-p700);
}

a.contact-value:hover {
  color: var(--mo-p800);
}

.contact-empty {
  margin: 0;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
}

html.dark .mo-about-page {
  background: var(--mo-n900);
  color: var(--mo-n100);
}

html.dark .about-panel {
  border-color: var(--mo-n700);
  background: var(--mo-n800);
}

html.dark .about-avatar-wrap img {
  border-color: var(--mo-p700);
  background: var(--mo-n700);
}

html.dark .section-label {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

/* 深色下 --mo-* 色阶被重映射：n50/n0/n800/n900 是背景色，
   文字必须用 n100~n200（主文字）或 n300~n500（次要文字） */
html.dark .about-name,
html.dark .section-head h2,
html.dark .about-content :deep(h1),
html.dark .about-content :deep(h2),
html.dark .about-content :deep(h3) {
  color: var(--mo-n200);
}

html.dark .about-role,
html.dark .stat-num,
html.dark a.contact-value {
  color: var(--mo-p300);
}

html.dark .about-desc,
html.dark .about-content {
  color: var(--mo-n500);
}

/* 深色下正文链接沿用浅色主题的 p700/p800 会几乎不可见（≈2:1），改用浅色阶 */
html.dark .about-content :deep(a) {
  color: var(--mo-p300);
}

html.dark .about-content :deep(a:hover) {
  color: var(--mo-p200);
}

html.dark .about-stat-strip {
  border-color: var(--mo-n700);
  background: var(--mo-n700);
}

html.dark .stat-item,
html.dark .contact-card {
  background: var(--mo-n900);
}

html.dark .stat-label,
html.dark .contact-type,
html.dark .contact-empty {
  color: var(--mo-n500);
}

html.dark .contact-card {
  border-color: var(--mo-n700);
}

html.dark .contact-card:hover {
  border-color: var(--mo-p700);
  background: rgba(99, 102, 241, 0.1);
}

html.dark .contact-icon {
  background: var(--mo-n800);
  color: var(--mo-p300);
}

html.dark .contact-value {
  color: var(--mo-n100);
}

@media (max-width: 768px) {
  .mo-about-page {
    padding: 72px 16px 48px;
  }

  .about-hero {
    grid-template-columns: 1fr;
    gap: 18px;
    padding: var(--mo-sp-6);
    text-align: center;
  }

  .about-avatar-wrap {
    margin: 0 auto;
    width: 104px;
    height: 104px;
  }

  .about-desc {
    margin: 0 auto;
  }

  .about-stat-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-item:last-child {
    grid-column: 1 / -1;
  }

  .about-content-panel,
  .contact-section {
    padding: 22px 20px 24px;
  }

  .about-name {
    font-size: 26px;
  }

  .contact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
