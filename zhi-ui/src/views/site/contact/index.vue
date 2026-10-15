<template>
  <SiteLayout>
    <div ref="rootEl" class="tpl-site contact-page mo-home-page">
      <!-- 页头 -->
      <header class="aph">
        <div class="eh-wrap">
          <span class="eyebrow rv">Contact</span>
          <h1 class="aph-title rv">联系我们</h1>
          <p class="aph-sub rv d1">期待与您的沟通与合作，我们将尽快响应每一份咨询。</p>
        </div>
      </header>

      <main>
        <!-- 基础联系信息（来自站点设置） -->
        <section class="pad">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">联系方式</div>
              <h2>随时找到我们</h2>
            </div>
            <div v-if="basicItems.length" class="ct-grid rv d1">
              <div v-for="item in basicItems" :key="item.key" class="ct-card">
                <div class="ct-type">{{ item.label }}</div>
                <a v-if="item.href" :href="item.href" class="ct-value ct-link">{{ item.value }}</a>
                <div v-else class="ct-value">{{ item.value }}</div>
              </div>
            </div>
            <el-empty v-else description="暂未公开联系方式，可在后台「站点设置」中填写" />
          </div>
        </section>

        <!-- 分渠道咨询（渠道专属联系方式为占位，接入后替换） -->
        <section class="pad surface">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">分渠道咨询</div>
              <h2>按需求选择对接渠道</h2>
              <p>以下渠道均可通过页面底部电话与邮箱联系，备注对应渠道即可精准触达。</p>
            </div>
            <div class="ct-grid ct-four rv d1">
              <div v-for="ch in channels" :key="ch.name" class="ct-card">
                <div class="ct-type">{{ ch.name }}</div>
                <p class="ct-desc">{{ ch.desc }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 地图占位（点击跳转外部地图搜索，第三方脚本延后加载，§6.1） -->
        <section v-if="siteSettings.contact_address" class="pad">
          <div class="eh-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">到访指引</div>
              <h2>我们的位置</h2>
            </div>
            <div class="ct-map rv d1">
              <svg
                class="ct-map-bg"
                viewBox="0 0 800 320"
                preserveAspectRatio="none"
                aria-hidden="true"
              >
                <g stroke="currentColor" stroke-width="1" fill="none">
                  <path d="M0 80h800M0 160h800M0 240h800M160 0v320M400 0v320M640 0v320" />
                </g>
                <circle cx="400" cy="160" r="10" fill="currentColor" />
                <circle
                  cx="400"
                  cy="160"
                  r="22"
                  stroke="currentColor"
                  stroke-width="1.5"
                  fill="none"
                />
              </svg>
              <div class="ct-map-body">
                <p>{{ siteSettings.contact_address }}</p>
                <a class="btn-p" :href="mapLink" target="_blank" rel="noopener noreferrer">
                  获取路线
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
                    <path
                      d="M4 3l4 4-4 4"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </a>
              </div>
            </div>
          </div>
        </section>

        <!-- FAQ 折叠面板（方案文档 §3.6：手风琴，同时展开 1 项） -->
        <section class="pad surface">
          <div class="eh-wrap ct-faq-wrap">
            <div class="tpl-sec-head rv">
              <div class="sec-tag">常见问题</div>
              <h2>你可能想问</h2>
            </div>
            <div class="ct-faq rv d1">
              <div v-for="(f, fi) in faqs" :key="f.q" class="ct-faq-item">
                <button
                  type="button"
                  class="ct-faq-q"
                  :aria-expanded="openFaq === fi"
                  @click="openFaq = openFaq === fi ? -1 : fi"
                >
                  {{ f.q }}
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 14 14"
                    fill="none"
                    aria-hidden="true"
                    :style="{ transform: openFaq === fi ? 'rotate(180deg)' : 'none' }"
                  >
                    <path
                      d="M3 5l4 4 4-4"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
                <p v-show="openFaq === fi" class="ct-faq-a">{{ f.a }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 留言入口（询盘表单后置：使用现有留言板承接） -->
        <section class="pad">
          <div class="eh-wrap ct-cta rv">
            <h2>有任何合作意向或问题？</h2>
            <p>给我们留言，将在 1 个工作日内回复。</p>
            <router-link v-if="isGuestbookEnabled" class="btn-p" to="/guestbook">
              前往留言板
            </router-link>
            <a
              v-else-if="siteSettings.blog_email"
              class="btn-p"
              :href="`mailto:${siteSettings.blog_email}`"
            >
              邮件联系
            </a>
          </div>
        </section>
      </main>
    </div>
  </SiteLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import SiteLayout from '@/components/SiteLayout.vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { setJsonLd } from '@/utils/jsonld'
import { getSiteChannels, getSiteFaqs } from '@/utils/siteContent'
import { useScrollReveal } from '@/composables/useScrollReveal'

interface ContactItem {
  key: string
  label: string
  value: string
  href?: string
}

const siteSettingsStore = useSiteSettingsStore()
const siteSettings = computed(() => siteSettingsStore.siteSettings)
const isGuestbookEnabled = computed(() => siteSettingsStore.isFeatureEnabled('guestbook_enabled'))

/* 分渠道与 FAQ：管理后台「官网内容」页签维护（缺失回退内置占位） */
const channels = computed(() => getSiteChannels(siteSettings.value?.site_channels))
const faqs = computed(() => getSiteFaqs(siteSettings.value?.site_faq))

const openFaq = ref(-1)

const basicItems = computed<ContactItem[]>(() => {
  const items: ContactItem[] = []
  if (siteSettings.value.contact_phone) {
    items.push({
      key: 'phone',
      label: '联系电话',
      value: siteSettings.value.contact_phone,
      href: `tel:${siteSettings.value.contact_phone.replace(/\s/g, '')}`
    })
  }
  if (siteSettings.value.blog_email) {
    items.push({
      key: 'email',
      label: '联系邮箱',
      value: siteSettings.value.blog_email,
      href: `mailto:${siteSettings.value.blog_email}`
    })
  }
  if (siteSettings.value.contact_address) {
    items.push({
      key: 'address',
      label: '办公地址',
      value: siteSettings.value.contact_address
    })
  }
  return items
})

const mapLink = computed(() => {
  const addr = encodeURIComponent(siteSettings.value.contact_address || '')
  return `https://www.amap.com/search?query=${addr}`
})

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

// FAQ 结构化数据（方案文档 §6.2）：设置异步加载完成后同步刷新
const updateFaqJsonLd = () => {
  setJsonLd('zh-jsonld-faqpage', {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: faqs.value.map(f => ({
      '@type': 'Question',
      name: f.q,
      acceptedAnswer: { '@type': 'Answer', text: f.a }
    }))
  })
}

watch(faqs, updateFaqJsonLd)

onMounted(() => {
  const siteName = siteSettings.value.blog_name || '知企建站系统'
  applySeo({
    title: `联系我们 - ${siteName}`,
    description: siteSettings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
  updateFaqJsonLd()
})
</script>

<style scoped>
@import '../about-shared.css';

.ct-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-6);
}

.ct-grid.ct-four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.ct-card {
  padding: var(--mo-sp-7);
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  background: var(--tpl-bg);
}

.ct-type {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  margin-bottom: var(--mo-sp-3);
}

.ct-value {
  color: var(--tpl-ink-900);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  word-break: break-all;
}

.ct-link:hover {
  color: var(--tpl-p-text);
}

.ct-desc {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-500);
  line-height: 1.7;
}

/* ---------- 地图占位 ---------- */
.ct-map {
  position: relative;
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
  background: var(--tpl-surface);
  color: var(--tpl-border);
  box-shadow: var(--tpl-sh-sm);
}

.ct-map-bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.ct-map-body {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mo-sp-6);
  padding: var(--mo-sp-9) var(--mo-sp-8);
  flex-wrap: wrap;
}

.ct-map-body p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-700);
  font-weight: 500;
}

/* ---------- 按钮 ---------- */
.btn-p {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  min-height: 48px;
  padding: 0 var(--mo-sp-6);
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p);
  color: var(--tpl-white);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  text-decoration: none;
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    background 0.2s,
    box-shadow 0.2s;
}

.btn-p:hover {
  background: var(--tpl-p-h);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-md);
}

/* ---------- FAQ ---------- */
.ct-faq-wrap {
  max-width: 760px;
}

.ct-faq {
  border-top: 1px solid var(--tpl-border);
}

.ct-faq-item {
  border-bottom: 1px solid var(--tpl-border);
}

.ct-faq-q {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--mo-sp-4);
  padding: var(--mo-sp-5) 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-size: var(--mo-fs-md);
  font-weight: 600;
  color: var(--tpl-ink-900);
  font-family: inherit;
}

.ct-faq-q svg {
  flex: none;
  color: var(--tpl-ink-400);
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.ct-faq-a {
  margin: 0 0 var(--mo-sp-5);
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-500);
  line-height: 1.75;
}

/* ---------- CTA ---------- */
.ct-cta {
  text-align: center;
  padding: var(--mo-sp-9) var(--mo-sp-7);
  border-radius: var(--tpl-r-xl);
  background:
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.14), transparent 40%), var(--tpl-dark);
}

.ct-cta h2 {
  margin: 0 0 var(--mo-sp-3);
  color: var(--tpl-white);
  font-size: var(--mo-fs-3xl);
}

.ct-cta p {
  margin: 0 0 var(--mo-sp-6);
  color: var(--tpl-dark-dim);
  font-size: var(--mo-fs-md);
}

.ct-cta .btn-p {
  align-self: center;
}

.ct-cta {
  display: flex;
  flex-direction: column;
  align-items: center;
}

@media (max-width: 991px) {
  .ct-grid,
  .ct-grid.ct-four {
    grid-template-columns: 1fr 1fr;
  }

  .eh-wrap {
    padding: 0 var(--mo-sp-5);
  }

  .pad {
    padding: var(--mo-sp-11) 0;
  }

  .aph {
    padding: calc(var(--mo-sp-10) + 64px) 0 var(--mo-sp-9);
  }

  .aph-title {
    font-size: var(--mo-fs-5xl);
  }
}

@media (max-width: 639px) {
  .ct-grid,
  .ct-grid.ct-four {
    grid-template-columns: 1fr;
  }
}
</style>
