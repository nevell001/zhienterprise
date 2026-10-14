<template>
  <div class="enterprise-home mo-home-page">
    <!-- Hero：品牌主视觉 + 双 CTA -->
    <header class="eh-hero">
      <div class="eh-container">
        <p class="eh-hero-kicker">{{ businessLabel }}</p>
        <h1>{{ settings.blog_name || '知企建站系统' }}</h1>
        <p class="eh-hero-desc">
          {{ settings.blog_desc || '以专业的产品与服务，陪伴每一家企业成长。' }}
        </p>
        <div class="eh-hero-actions">
          <button type="button" class="eh-btn eh-btn-primary" @click="scrollToBusiness">
            查看{{ businessLabel }}
          </button>
          <button type="button" class="eh-btn eh-btn-ghost" @click="goContact">联系我们</button>
        </div>
      </div>
    </header>

    <!-- 核心业务：article_type=business 的内容卡片 -->
    <section id="eh-business" class="eh-section">
      <div class="eh-container">
        <div class="eh-section-head">
          <h2>{{ businessLabel }}</h2>
          <p>了解我们的主要业务与产品</p>
        </div>
        <div v-if="businessLoading" class="eh-grid">
          <div v-for="n in 3" :key="n" class="eh-card eh-skeleton"></div>
        </div>
        <div v-else-if="businessItems.length" class="eh-grid">
          <article
            v-for="item in businessItems"
            :key="item.id"
            class="eh-card"
            @click="goBusinessItem(item.id)"
          >
            <div class="eh-card-thumb" :style="thumbStyle(item.id)">
              {{ (item.title || '').slice(0, 1) }}
            </div>
            <div class="eh-card-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <span class="eh-card-more">了解详情 →</span>
            </div>
          </article>
        </div>
        <p v-else class="eh-empty">暂无内容，可在后台「核心业务管理」中添加</p>
      </div>
    </section>

    <!-- 数据一览 -->
    <section v-if="stats" class="eh-stats">
      <div class="eh-container eh-stats-row">
        <div class="eh-stat">
          <strong>{{ stats.articleCount ?? 0 }}</strong>
          <span>发布内容</span>
        </div>
        <div class="eh-stat">
          <strong>{{ stats.categoryCount ?? 0 }}</strong>
          <span>栏目分类</span>
        </div>
        <div class="eh-stat">
          <strong>{{ formatCount(stats.totalViews) }}</strong>
          <span>累计浏览</span>
        </div>
        <div class="eh-stat">
          <strong>{{ stats.tagCount ?? 0 }}</strong>
          <span>内容标签</span>
        </div>
      </div>
    </section>

    <!-- 客户案例：article_type=case，无内容时整块隐藏 -->
    <section v-if="caseItems.length" class="eh-section eh-alt">
      <div class="eh-container">
        <div class="eh-section-head">
          <h2>客户案例</h2>
          <p>他们选择了与我们同行</p>
        </div>
        <div class="eh-grid">
          <article
            v-for="item in caseItems"
            :key="item.id"
            class="eh-card"
            @click="goBusinessItem(item.id)"
          >
            <div class="eh-card-thumb" :style="thumbStyle(item.id)">
              {{ (item.title || '').slice(0, 1) }}
            </div>
            <div class="eh-card-body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <span class="eh-card-more">查看案例 →</span>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 新闻动态：article_type=news -->
    <section class="eh-section">
      <div class="eh-container">
        <div class="eh-section-head eh-head-row">
          <div>
            <h2>新闻动态</h2>
            <p>了解公司的最新资讯</p>
          </div>
          <router-link class="eh-more" to="/blog/articles">更多动态 →</router-link>
        </div>
        <ul v-if="newsItems.length" class="eh-news">
          <li v-for="item in newsItems" :key="item.id" @click="goNewsItem(item.id)">
            <span class="eh-news-title">{{ item.title }}</span>
            <span class="eh-news-date">{{ formatDate(item.createTime) }}</span>
          </li>
        </ul>
        <p v-else class="eh-empty">暂无动态</p>
      </div>
    </section>

    <!-- CTA 收尾：联系信息 -->
    <section class="eh-cta">
      <div class="eh-container eh-cta-inner">
        <h2>期待与您的合作</h2>
        <p class="eh-cta-contact">
          <span v-if="settings.contact_phone">电话：{{ settings.contact_phone }}</span>
          <span v-if="settings.contact_address">地址：{{ settings.contact_address }}</span>
          <span v-if="settings.blog_email">邮箱：{{ settings.blog_email }}</span>
        </p>
        <a
          v-if="settings.blog_email"
          class="eh-btn eh-btn-light"
          :href="`mailto:${settings.blog_email}`"
        >
          邮件联系我们
        </a>
        <button v-else type="button" class="eh-btn eh-btn-light" @click="goContact">联系我们</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/blog'
import type { Article } from '@/types'
import { getPublicStatisticsOverview } from '@/api/statistics'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { parseTime } from '@/utils/zhi'

interface StatsOverview {
  articleCount?: number
  categoryCount?: number
  tagCount?: number
  commentCount?: number
  totalViews?: number
}

const router = useRouter()
const blogSettingsStore = useBlogSettingsStore()
const settings = computed(() => blogSettingsStore.blogSettings)
const businessLabel = computed(() => String(settings.value.business_label || '').trim() || '核心业务')

const businessItems = ref<Article[]>([])
const caseItems = ref<Article[]>([])
const newsItems = ref<Article[]>([])
const businessLoading = ref(true)
const stats = ref<StatsOverview | null>(null)

const loadSection = async (articleType: string, pageSize: number): Promise<Article[]> => {
  const response = await getArticleList({ pageNum: 1, pageSize, articleType })
  if (response && response.code === 200) {
    return (response.rows || []) as Article[]
  }
  return []
}

onMounted(async () => {
  const [business, cases, news, statsRes] = await Promise.allSettled([
    loadSection('business', 6),
    loadSection('case', 3),
    loadSection('news', 6),
    getPublicStatisticsOverview()
  ])
  if (business.status === 'fulfilled') businessItems.value = business.value
  if (cases.status === 'fulfilled') caseItems.value = cases.value
  if (news.status === 'fulfilled') newsItems.value = news.value
  if (statsRes.status === 'fulfilled' && statsRes.value?.code === 200) {
    stats.value = statsRes.value.data as StatsOverview
  }
  businessLoading.value = false
})

const formatCount = (value?: number) => {
  const n = Number(value) || 0
  return n >= 10000 ? `${(n / 10000).toFixed(1)}w+` : String(n)
}

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')

const thumbStyle = (id?: number) => ({
  background:
    Math.abs(Number(id) || 0) % 2 === 0
      ? 'linear-gradient(135deg, var(--mo-p500), var(--mo-p700))'
      : 'linear-gradient(135deg, var(--mo-p700), var(--mo-n700))'
})

const scrollToBusiness = () =>
  document.getElementById('eh-business')?.scrollIntoView({ behavior: 'smooth' })
const goContact = () => router.push('/blog/contact')
const goBusinessItem = (id?: number) => id && router.push(`/blog/business/${id}`)
const goNewsItem = (id?: number) => id && router.push(`/blog/article/${id}`)
</script>

<style scoped>
.eh-container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 var(--mo-sp-5);
}

/* Hero */
.eh-hero {
  padding: var(--mo-sp-10) 0 calc(var(--mo-sp-9) + 12px);
  text-align: center;
  color: var(--mo-n50);
  background:
    radial-gradient(circle at 20% 12%, rgba(255, 255, 255, 0.16), transparent 42%),
    linear-gradient(135deg, var(--mo-p700), var(--mo-p900));
}

.eh-hero-kicker {
  margin: 0 0 var(--mo-sp-3);
  display: inline-block;
  padding: 4px 14px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: var(--mo-r-full);
  font-size: var(--mo-fs-sm);
  letter-spacing: 2px;
}

.eh-hero h1 {
  margin: 0 0 var(--mo-sp-4);
  font-size: clamp(30px, 5vw, 44px);
  font-weight: 800;
  letter-spacing: 1px;
}

.eh-hero-desc {
  max-width: 640px;
  margin: 0 auto var(--mo-sp-6);
  font-size: var(--mo-fs-md);
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.86);
}

.eh-hero-actions {
  display: flex;
  justify-content: center;
  gap: var(--mo-sp-4);
  flex-wrap: wrap;
}

.eh-btn {
  display: inline-block;
  padding: 10px 26px;
  border-radius: var(--mo-r-md);
  border: 1px solid transparent;
  font-size: var(--mo-fs-md);
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background-color 0.18s ease;
}

.eh-btn:hover {
  transform: translateY(-2px);
}

.eh-btn-primary {
  background: var(--mo-n50);
  color: var(--mo-p700);
}

.eh-btn-primary:hover {
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
}

.eh-btn-ghost {
  background: transparent;
  color: var(--mo-n50);
  border-color: rgba(255, 255, 255, 0.6);
}

.eh-btn-ghost:hover {
  background: rgba(255, 255, 255, 0.14);
}

.eh-btn-light {
  background: var(--mo-n50);
  color: var(--mo-p700);
}

/* 区块通用 */
.eh-section {
  padding: var(--mo-sp-9) 0;
}

.eh-section.eh-alt {
  background: var(--mo-n50);
}

.eh-section-head {
  text-align: center;
  margin-bottom: var(--mo-sp-7);
}

.eh-section-head h2 {
  margin: 0 0 var(--mo-sp-2);
  font-size: var(--mo-fs-2xl);
  color: var(--mo-n900);
}

.eh-section-head p {
  margin: 0;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
}

.eh-head-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  text-align: left;
}

.eh-head-row p {
  margin-top: var(--mo-sp-1);
}

.eh-more {
  color: var(--mo-p600);
  font-size: var(--mo-fs-sm);
  text-decoration: none;
  font-weight: 600;
}

.eh-more:hover {
  color: var(--mo-p500);
}

/* 卡片网格 */
.eh-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-5);
}

.eh-card {
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-xl);
  overflow: hidden;
  background: var(--el-bg-color);
  box-shadow: var(--mo-shadow-sm);
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.eh-card:hover {
  transform: translateY(-3px);
  border-color: var(--mo-p200);
  box-shadow: var(--mo-shadow-lg);
}

.eh-card-thumb {
  height: 132px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 34px;
  font-weight: 700;
}

.eh-card-body {
  padding: var(--mo-sp-5);
}

.eh-card-body h3 {
  margin: 0 0 var(--mo-sp-2);
  font-size: var(--mo-fs-lg);
  color: var(--mo-n900);
}

.eh-card-body p {
  margin: 0 0 var(--mo-sp-3);
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.eh-card-more {
  color: var(--mo-p600);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
}

.eh-skeleton {
  height: 260px;
  background: linear-gradient(90deg, var(--mo-n100), var(--mo-n50), var(--mo-n100));
  background-size: 200% 100%;
  animation: eh-shimmer 1.4s ease infinite;
  cursor: default;
}

@keyframes eh-shimmer {
  from {
    background-position: 200% 0;
  }

  to {
    background-position: -200% 0;
  }
}

.eh-empty {
  text-align: center;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
}

/* 数据条 */
.eh-stats {
  background: var(--mo-n900);
  padding: var(--mo-sp-7) 0;
}

.eh-stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--mo-sp-5);
  text-align: center;
}

.eh-stat strong {
  display: block;
  color: var(--mo-n50);
  font-size: 30px;
  font-weight: 800;
}

.eh-stat span {
  color: rgba(255, 255, 255, 0.64);
  font-size: var(--mo-fs-sm);
}

/* 新闻列表 */
.eh-news {
  list-style: none;
  margin: 0;
  padding: 0;
}

.eh-news li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mo-sp-4);
  padding: var(--mo-sp-4) var(--mo-sp-2);
  border-bottom: 1px solid var(--mo-n200);
  cursor: pointer;
}

.eh-news li:hover .eh-news-title {
  color: var(--mo-p600);
}

.eh-news-title {
  color: var(--mo-n800);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  transition: color 0.18s ease;
}

.eh-news-date {
  flex-shrink: 0;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
}

/* CTA 收尾 */
.eh-cta {
  background: var(--mo-n50);
  padding: var(--mo-sp-9) 0;
}

.eh-cta-inner {
  text-align: center;
  padding: var(--mo-sp-8) var(--mo-sp-5);
  border-radius: var(--mo-r-xl);
  background:
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.14), transparent 40%),
    linear-gradient(135deg, var(--mo-p800), var(--mo-p600));
}

.eh-cta h2 {
  margin: 0 0 var(--mo-sp-3);
  color: var(--mo-n50);
  font-size: var(--mo-fs-2xl);
}

.eh-cta-contact {
  margin: 0 0 var(--mo-sp-5);
  color: rgba(255, 255, 255, 0.86);
  font-size: var(--mo-fs-sm);
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--mo-sp-4);
}

/* 响应式 */
@media (max-width: 900px) {
  .eh-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .eh-stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: var(--mo-sp-6);
  }
}

@media (max-width: 600px) {
  .eh-hero {
    padding: var(--mo-sp-8) 0 var(--mo-sp-8);
  }

  .eh-grid {
    grid-template-columns: 1fr;
  }

  .eh-news li {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--mo-sp-1);
  }

  .eh-head-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--mo-sp-2);
  }
}
</style>
