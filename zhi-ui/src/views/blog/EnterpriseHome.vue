<template>
  <div ref="rootEl" class="tpl-site enterprise-home mo-home-page" :class="tpl.className">
    <!-- ① Hero：Eyebrow → 主标题 → 副标题 → 双 CTA → 母题主视觉 → 信任指标条 -->
    <header class="eh-hero">
      <div class="eh-wrap eh-hero-grid">
        <div class="rv">
          <span class="eyebrow">{{ tpl.hero.eyebrow }}</span>
          <!-- titleHtml 为仓库内置静态配置（siteTemplates.ts），非用户输入 -->
          <!-- eslint-disable-next-line vue/no-v-html -->
          <h1 class="eh-title" v-html="tpl.hero.titleHtml"></h1>
          <p class="eh-sub">{{ tpl.hero.subtitle }}</p>
          <div class="eh-cta">
            <router-link class="btn-p" to="/business">
              了解我们的业务
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path
                  d="M3 8h10M9 4l4 4-4 4"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </router-link>
            <router-link class="btn-s" to="/contact">预约咨询</router-link>
          </div>
          <div class="metrics">
            <div v-for="m in SITE_METRICS" :key="m.label" class="metric">
              <b>{{ m.value }}</b>
              <span>{{ m.label }}</span>
            </div>
          </div>
        </div>
        <!-- eslint-disable-next-line vue/no-v-html -->
        <div class="hero-viz rv d1" v-html="motifSvg"></div>
      </div>
    </header>

    <!-- ② 核心业务总览：两条业务线卡片（列表项接入核心业务内容，空时回退占位） -->
    <section class="pad surface">
      <div class="eh-wrap">
        <div class="tpl-sec-head rv">
          <div class="sec-tag">核心业务</div>
          <h2>两条业务线，覆盖你的完整链路</h2>
          <p>产品解决「用什么」，服务解决「怎么落地」。两者共用一套方法论，互为支撑。</p>
        </div>
        <div class="biz">
          <div
            v-for="(card, ci) in bizCards"
            :key="card.title"
            class="biz-card rv"
            :class="`d${ci}`"
          >
            <div class="ico" aria-hidden="true">
              <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                <path
                  :d="card.iconPath"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linejoin="round"
                />
                <path
                  d="M11 2l8 4.5v9L11 20l-8-4.5v-9L11 2z"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <h3>{{ card.title }}</h3>
            <p>{{ card.desc }}</p>
            <ul class="biz-list">
              <li
                v-for="item in card.items"
                :key="item.label"
                :class="{ clickable: item.id }"
                @click="item.id && goBusinessItem(item.id)"
              >
                {{ item.label }}
                <svg width="15" height="15" viewBox="0 0 15 15" fill="none" aria-hidden="true">
                  <path
                    d="M3 7.5h9M8 4l3.5 3.5L8 11"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- ③ 解决方案精选：行业方案卡，插画随模板变化 -->
    <section class="pad">
      <div class="eh-wrap">
        <div class="tpl-sec-head rv">
          <div class="sec-tag">解决方案</div>
          <h2>{{ tpl.solutionsTitle }}</h2>
          <p>每个方案都基于真实业务场景，包含客户挑战、方案架构、实施路径与可量化成果。</p>
        </div>
        <div class="sol">
          <div
            v-for="(item, si) in SOLUTION_SHOWCASE"
            :key="item.title"
            class="sol-card rv"
            :class="`d${si % 4}`"
          >
            <div class="num">{{ String(si + 1).padStart(2, '0') }} / 行业</div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
            <router-link class="lnk" to="/business">
              查看方案
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path
                  d="M3 8h10M9 4l4 4-4 4"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </router-link>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <svg
              class="sol-art"
              viewBox="0 0 200 200"
              fill="none"
              aria-hidden="true"
              v-html="tpl.art"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- ④ 数据成果（深色区块，占位文案见 siteTemplates.ts） -->
    <section class="pad dark">
      <div class="eh-wrap">
        <div class="tpl-sec-head rv">
          <div class="sec-tag">数据成果</div>
          <h2>可量化的改变</h2>
          <p>我们只把能被验证的数字写进这里。</p>
        </div>
        <div class="numgrid rv d1">
          <div v-for="o in SITE_OUTCOMES" :key="o.label" class="numcell">
            <b>
              <i>{{ o.mark }}</i>
              {{ o.suffix }}
            </b>
            <span>{{ o.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ⑤ 客户与案例：Logo 墙（接入友链数据，不足回退占位） -->
    <section class="pad surface">
      <div class="eh-wrap">
        <div class="tpl-sec-head rv">
          <div class="sec-tag">客户与案例</div>
          <h2>他们已经在用</h2>
        </div>
        <div class="logos rv">
          <div v-for="name in partnerNames" :key="name" class="logocell">{{ name }}</div>
        </div>
      </div>
    </section>

    <!-- ⑥ 新闻动态：头条 + 两小条（接入资讯内容） -->
    <section class="pad">
      <div class="eh-wrap">
        <div class="tpl-sec-head eh-head-row rv">
          <div>
            <div class="sec-tag">新闻动态</div>
            <h2>最近在做什么</h2>
          </div>
          <router-link class="lnk" to="/news">
            更多动态
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path
                d="M3 8h10M9 4l4 4-4 4"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </router-link>
        </div>
        <div v-if="newsItems.length" class="news">
          <article
            v-for="(item, ni) in newsItems"
            :key="item.id"
            class="n-card rv"
            :class="`d${ni % 3}`"
            @click="goNewsItem(item.id)"
          >
            <div class="n-thumb">
              <img
                v-if="item.coverUrl"
                :src="item.coverUrl"
                :alt="item.title"
                loading="lazy"
                decoding="async"
              />
              <svg
                v-else
                width="56"
                height="56"
                viewBox="0 0 56 56"
                fill="none"
                opacity=".5"
                aria-hidden="true"
              >
                <rect
                  x="14"
                  y="14"
                  width="28"
                  height="28"
                  rx="8"
                  stroke="currentColor"
                  stroke-width="1.5"
                />
                <path
                  d="M20 28h16"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                />
              </svg>
            </div>
            <div class="n-body">
              <h3>{{ item.title }}</h3>
              <div class="n-meta">
                <span class="tag">企业动态</span>
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </article>
        </div>
        <p v-else class="eh-empty rv">暂无动态，发布资讯后将在这里展示</p>
      </div>
    </section>

    <!-- ⑦ 转化区块（深色 + 网格背景图形） -->
    <section class="cta pad">
      <svg class="cta-bg" viewBox="0 0 1440 400" preserveAspectRatio="none" aria-hidden="true">
        <g stroke="currentColor" stroke-width="1" fill="none">
          <path
            d="M0 80h1440M0 200h1440M0 320h1440M120 0v400M400 0v400M680 0v400M960 0v400M1240 0v400"
          />
        </g>
      </svg>
      <div class="eh-wrap eh-cta-inner">
        <h2 class="rv">把下一步交给我们</h2>
        <p class="rv d1">一个小时的免费诊断，换一份可执行的问题清单。</p>
        <div class="rv d2 eh-cta-btns">
          <router-link class="btn-p" to="/contact">
            预约咨询
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path
                d="M3 8h10M9 4l4 4-4 4"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </router-link>
          <router-link class="btn-d" to="/about">了解我们</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getArticleList } from '@/api/blog'
import type { Article } from '@/types'
import { getFrontFriendLinkList } from '@/api/blog/friendLink'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import {
  getSiteTemplate,
  SITE_METRICS,
  SITE_OUTCOMES,
  SOLUTION_SHOWCASE
} from '@/config/siteTemplates'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { parseTime } from '@/utils/zhi'
import { logger } from '@/utils/logger'

const router = useRouter()
const blogSettingsStore = useBlogSettingsStore()
const settings = computed(() => blogSettingsStore.blogSettings)

/** 当前行业模板（站点设置 site_template，非法值回退默认） */
const tpl = computed(() => getSiteTemplate(settings.value.site_template))
const motifSvg = computed(() => tpl.value.motif(tpl.value.color))

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

/* ---------- 数据：核心业务 / 新闻动态 / 合作伙伴 ---------- */

const businessItems = ref<Article[]>([])
const newsItems = ref<Article[]>([])
const partnerNames = ref<string[]>([])

const FALLBACK_PARTNERS = [
  '客户 A',
  '客户 B',
  '客户 C',
  '客户 D',
  '客户 E',
  '客户 F',
  '客户 G',
  '客户 H'
]

/** 核心业务两卡的列表项：优先用真实业务条目，空数据回退模板占位 */
const bizCards = computed(() => {
  const half = Math.ceil(businessItems.value.length / 2) || 3
  const toItems = (list: Article[], fallback: string[]) => {
    const items = list.map(a => ({ id: a.id, label: a.title || '' })).filter(i => i.label)
    return items.length > 0 ? items : fallback.map(label => ({ id: undefined, label }))
  }
  return [
    {
      ...tpl.value.biz.one,
      iconPath: 'M11 6v10M6 11h10',
      items: toItems(businessItems.value.slice(0, half), tpl.value.biz.fallbackItems)
    },
    {
      ...tpl.value.biz.two,
      iconPath: 'M11 6.5v9M6.5 11h9',
      items: toItems(businessItems.value.slice(half, half + 3), tpl.value.biz.fallbackItems)
    }
  ]
})

onMounted(async () => {
  const [business, news, partners] = await Promise.allSettled([
    getArticleList({ pageNum: 1, pageSize: 6, articleType: 'business' }),
    getArticleList({ pageNum: 1, pageSize: 3, articleType: 'news' }),
    getFrontFriendLinkList()
  ])
  if (business.status === 'fulfilled' && business.value?.code === 200) {
    businessItems.value = (business.value.rows || []) as Article[]
  }
  if (news.status === 'fulfilled' && news.value?.code === 200) {
    newsItems.value = (news.value.rows || []) as Article[]
  }
  if (partners.status === 'fulfilled') {
    // 友链接口结构宽松处理：数组或 {rows} 均可，只取名称
    const raw: unknown = partners.value
    const list: unknown = Array.isArray(raw)
      ? raw
      : ((raw as { data?: unknown[]; rows?: unknown[] })?.data ??
        (raw as { data?: unknown[]; rows?: unknown[] })?.rows ??
        [])
    const names = (Array.isArray(list) ? list : [])
      .map((item: unknown) => String((item as { name?: unknown })?.name ?? '').trim())
      .filter(Boolean)
      .slice(0, 8)
    if (names.length > 0) {
      // 不足 8 个时用占位补齐网格（设计稿 Logo 墙固定 8 格）
      partnerNames.value = [...names, ...FALLBACK_PARTNERS.slice(names.length)]
    }
  }
  if (partnerNames.value.length === 0) {
    partnerNames.value = FALLBACK_PARTNERS
  }
})

const formatDate = (date?: string) => (date ? parseTime(date, '{y}-{m}-{d}') : '')
const goBusinessItem = (id?: number) => id && router.push(`/business/${id}`)
const goNewsItem = (id?: number) => id && router.push(`/article/${id}`)
</script>

<style scoped>
/* 企业官网模板首页：五套共用组件层（base 层）。
   颜色一律引用 --tpl-* 变量（变量层见 themes/site-templates.scss），
   模板差异不写死在本组件。 */
.eh-wrap {
  max-width: var(--tpl-grid);
  margin: 0 auto;
  padding: 0 var(--mo-sp-7);
}

.pad {
  padding: var(--mo-sp-12) 0;
}

.pad.surface {
  background: var(--tpl-surface);
}

.sec-tag {
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--tpl-p-text);
  margin-bottom: var(--mo-sp-3);
}

.tpl-sec-head {
  margin-bottom: var(--mo-sp-11);
}

.tpl-sec-head h2 {
  margin: 0 0 var(--mo-sp-3);
  font-size: var(--mo-fs-5xl);
  color: var(--tpl-ink-900);
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.tpl-sec-head p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-500);
  max-width: 620px;
}

.eh-head-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--mo-sp-4);
}

/* ---------- 按钮（base.css .btn-p/.btn-s/.btn-d） ---------- */
.btn-p,
.btn-s,
.btn-d {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--mo-sp-2);
  min-height: 48px;
  padding: 0 var(--mo-sp-6);
  border-radius: var(--tpl-r-md);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
  transition:
    transform 0.2s cubic-bezier(0.16, 1, 0.3, 1),
    background 0.2s,
    box-shadow 0.2s,
    border-color 0.2s,
    color 0.2s;
}

.btn-p {
  background: var(--tpl-p);
  color: var(--tpl-white);
}

.btn-p:hover {
  background: var(--tpl-p-h);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-md);
}

.btn-s {
  background: var(--tpl-bg);
  color: var(--tpl-p-text);
  border-color: var(--tpl-p);
}

.btn-s:hover {
  background: var(--tpl-p-sub);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-sm);
}

.btn-d {
  background: var(--tpl-ink-900);
  color: var(--tpl-white);
}

.btn-d:hover {
  background: var(--tpl-dark-2);
  transform: translateY(-2px);
  box-shadow: var(--tpl-sh-md);
}

.lnk {
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  font-size: var(--mo-fs-md);
  color: var(--tpl-p-text);
  font-weight: 500;
  text-decoration: none;
}

/* ---------- ① Hero ---------- */
.eh-hero {
  /* 顶部补偿 fixed 导航高度（64px）：透明导航浮于 Hero 之上 */
  padding: calc(var(--mo-sp-12) + 64px) 0 var(--mo-sp-10);
  overflow: hidden;
}

.eh-hero-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mo-sp-11);
  align-items: center;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
  margin-bottom: var(--mo-sp-6);
}

.eh-title {
  margin: 0 0 var(--mo-sp-6);
  font-size: 58px;
  line-height: 1.15;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--tpl-ink-900);
  font-family: var(--tpl-serif);
}

.eh-title :deep(em) {
  font-style: normal;
  color: var(--tpl-p-text);
}

.eh-sub {
  font-size: var(--mo-fs-lg);
  line-height: 1.75;
  color: var(--tpl-ink-500);
  max-width: 480px;
  margin: 0 0 var(--mo-sp-8);
}

.eh-cta {
  display: flex;
  gap: var(--mo-sp-4);
  flex-wrap: wrap;
  margin-bottom: var(--mo-sp-11);
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--mo-sp-5);
  padding-top: var(--mo-sp-8);
  border-top: 1px solid var(--tpl-border);
}

.metric b {
  display: block;
  font-size: var(--mo-fs-4xl);
  color: var(--tpl-ink-900);
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.metric span {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
}

.hero-viz {
  border-radius: var(--tpl-r-xl);
  padding: var(--mo-sp-5);
  display: grid;
  place-items: center;
}

.hero-viz :deep(svg) {
  width: 100%;
  height: auto;
  aspect-ratio: 1;
}

/* ---------- ② 核心业务 ---------- */
.biz {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mo-sp-6);
}

.biz-card {
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  padding: var(--mo-sp-8);
  background: var(--tpl-bg);
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s,
    border-color 0.3s;
}

.biz-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
  border-color: var(--tpl-p);
}

.biz-card .ico {
  width: 44px;
  height: 44px;
  border-radius: var(--tpl-r-md);
  background: var(--tpl-p-sub);
  color: var(--tpl-p-text);
  display: grid;
  place-items: center;
  margin-bottom: var(--mo-sp-5);
}

.biz-card h3 {
  font-size: var(--mo-fs-2xl);
  margin: 0 0 var(--mo-sp-2);
  color: var(--tpl-ink-900);
}

.biz-card > p {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  margin: 0 0 var(--mo-sp-6);
}

.biz-list {
  list-style: none;
  border-top: 1px solid var(--tpl-border);
  margin: 0;
  padding: 0;
}

.biz-list li {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-3);
  padding: var(--mo-sp-4) 0;
  border-bottom: 1px solid var(--tpl-border);
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-700);
  transition: color 0.2s;
}

.biz-list li.clickable {
  cursor: pointer;
}

.biz-list li:hover {
  color: var(--tpl-p-text);
}

.biz-list svg {
  flex: none;
  margin-left: auto;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.biz-list li:hover svg {
  transform: translateX(3px);
}

/* ---------- ③ 解决方案 ---------- */
.sol {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mo-sp-6);
}

.sol-card {
  position: relative;
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  padding: var(--mo-sp-8);
  background: var(--tpl-surface);
  min-height: 264px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s;
}

.sol-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
}

.sol-card .num {
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  color: var(--tpl-p-text);
  letter-spacing: 0.08em;
  margin-bottom: var(--mo-sp-4);
}

.sol-card h3 {
  font-size: var(--mo-fs-xl);
  margin: 0 0 var(--mo-sp-2);
  color: var(--tpl-ink-900);
}

.sol-card p {
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-500);
  flex: 1;
  position: relative;
  z-index: 1;
  margin: 0;
}

.sol-card .lnk {
  margin-top: var(--mo-sp-5);
}

.sol-art {
  position: absolute;
  right: -20px;
  bottom: -20px;
  width: 180px;
  height: 180px;
  color: var(--tpl-p);
  opacity: 0.45;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.sol-card:hover .sol-art {
  transform: scale(1.06);
}

/* ---------- ④ 数据成果（深色区块） ---------- */
.pad.dark {
  background: var(--tpl-dark);
}

.pad.dark .sec-tag {
  color: var(--tpl-p-d);
}

.pad.dark h2 {
  color: var(--tpl-white);
}

.pad.dark p {
  color: var(--tpl-dark-dim);
}

.numgrid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  background: rgba(255, 255, 255, 0.13);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
}

.numcell {
  background: var(--tpl-dark);
  padding: var(--mo-sp-11) var(--mo-sp-9);
}

.numcell b {
  display: block;
  font-size: var(--mo-fs-6xl);
  color: var(--tpl-white);
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.numcell b i {
  font-style: normal;
  color: var(--tpl-p-d);
}

.numcell span {
  display: block;
  margin-top: var(--mo-sp-3);
  font-size: var(--mo-fs-sm);
  color: var(--tpl-dark-dim);
}

/* ---------- ⑤ Logo 墙 ---------- */
.logos {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: var(--tpl-border);
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
}

.logocell {
  background: var(--tpl-bg);
  height: 104px;
  display: grid;
  place-items: center;
  font-size: var(--mo-fs-md);
  font-weight: 500;
  color: var(--tpl-ink-400);
  filter: grayscale(1);
  opacity: 0.6;
  transition:
    opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1),
    color 0.4s,
    filter 0.4s;
}

.logocell:hover {
  opacity: 1;
  filter: grayscale(0);
  color: var(--tpl-p-text);
}

/* ---------- ⑥ 新闻动态 ---------- */
.news {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr;
  gap: var(--mo-sp-6);
}

.n-card {
  border: 1px solid var(--tpl-border);
  border-radius: var(--tpl-r-lg);
  overflow: hidden;
  background: var(--tpl-bg);
  cursor: pointer;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s;
}

.n-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tpl-sh-md);
}

.n-thumb {
  aspect-ratio: 16 / 9;
  background: var(--tpl-surface);
  display: grid;
  place-items: center;
  border-bottom: 1px solid var(--tpl-border);
  color: var(--tpl-p);
  overflow: hidden;
}

.n-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.n-body {
  padding: var(--mo-sp-5) var(--mo-sp-6) var(--mo-sp-6);
}

.n-body h3 {
  font-size: var(--mo-fs-lg);
  line-height: 1.5;
  margin: 0 0 var(--mo-sp-4);
  color: var(--tpl-ink-900);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.n-meta {
  display: flex;
  gap: var(--mo-sp-3);
  align-items: center;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
  flex-wrap: wrap;
}

.tag {
  display: inline-block;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  letter-spacing: 0.02em;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
}

.eh-empty {
  text-align: center;
  color: var(--tpl-ink-400);
  font-size: var(--mo-fs-sm);
}

/* ---------- ⑦ 转化区块 ---------- */
.cta {
  position: relative;
  background: var(--tpl-dark);
  overflow: hidden;
  text-align: center;
}

.cta-bg {
  position: absolute;
  inset: 0;
  color: var(--tpl-white);
  opacity: 0.18;
  pointer-events: none;
}

.eh-cta-inner {
  position: relative;
  max-width: 680px;
}

.cta h2 {
  color: var(--tpl-white);
  font-size: var(--mo-fs-6xl);
  margin: 0 0 var(--mo-sp-4);
  letter-spacing: -0.02em;
}

.cta p {
  color: var(--tpl-dark-dim);
  font-size: var(--mo-fs-lg);
  margin: 0 0 var(--mo-sp-8);
}

.eh-cta-btns {
  display: flex;
  gap: var(--mo-sp-4);
  justify-content: center;
  flex-wrap: wrap;
}

/* ---------- 响应式（原型 991/639 两档） ---------- */
@media (max-width: 991px) {
  .eh-wrap {
    padding: 0 var(--mo-sp-5);
  }

  .pad {
    padding: var(--mo-sp-11) 0;
  }

  .eh-hero {
    padding: var(--mo-sp-9) 0 var(--mo-sp-10);
  }

  .eh-hero-grid,
  .biz,
  .sol,
  .news {
    grid-template-columns: 1fr;
  }

  .eh-title {
    font-size: 38px;
  }

  .numgrid,
  .logos {
    grid-template-columns: 1fr 1fr;
  }

  .tpl-sec-head h2 {
    font-size: var(--mo-fs-3xl);
  }
}

@media (max-width: 639px) {
  .pad {
    padding: var(--mo-sp-10) 0;
  }

  .metrics {
    grid-template-columns: 1fr 1fr;
  }

  .numgrid {
    grid-template-columns: 1fr;
  }

  .numcell {
    padding: var(--mo-sp-8) var(--mo-sp-6);
  }
}
</style>
