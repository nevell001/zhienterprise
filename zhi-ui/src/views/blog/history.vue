<template>
  <div ref="rootEl" class="tpl-site history-page mo-home-page">
    <!-- 页头 -->
    <header class="aph">
      <div class="eh-wrap">
        <span class="eyebrow rv">Milestones</span>
        <h1 class="aph-title rv">发展历程</h1>
        <p class="aph-sub rv d1">从初创到生态，每一步都算数。</p>
      </div>
    </header>

    <main class="pad">
      <div class="eh-wrap">
        <!-- 时间轴：阶段分组，荣誉节点用方形区分（方案文档 §3.5）。
             事件数据为设计稿附录 A 占位，接入内容模型后替换。 -->
        <div class="tl">
          <div v-for="stage in STAGES" :key="stage.name" class="tl-stage">
            <div class="tl-stage-head rv">
              <span class="tl-range">{{ stage.range }}</span>
              <h2>{{ stage.name }}</h2>
              <p>{{ stage.theme }}</p>
            </div>
            <div class="tl-events">
              <div
                v-for="(event, ei) in stage.events"
                :key="event.title"
                class="tl-event rv"
                :class="`d${ei % 4}`"
              >
                <span class="tl-node" :class="{ square: event.honor }" aria-hidden="true"></span>
                <div class="tl-body">
                  <span class="tl-year">{{ event.year }}</span>
                  <h3>
                    {{ event.title }}
                    <span v-if="event.honor" class="tag">荣誉</span>
                  </h3>
                  <p>{{ event.desc }}</p>
                </div>
              </div>
            </div>
          </div>
          <div class="tl-end rv">
            <span class="tl-node square" aria-hidden="true"></span>
            <p>未来，正在书写。</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { useScrollReveal } from '@/composables/useScrollReveal'

const blogSettingsStore = useBlogSettingsStore()

/* 历程数据：设计稿附录 A 占位（初创/成长/领先/生态四阶段），
   接入内容模型后由后端数据替换 */
const STAGES = [
  {
    range: '2011 - 2014',
    name: '初创期',
    theme: '找准方向，活下来',
    events: [
      {
        year: '2011',
        title: '公司成立',
        desc: '核心团队组建，确立深耕行业信息化的发展方向。',
        honor: false
      },
      {
        year: '2012',
        title: '首个标杆客户',
        desc: '签约第一家行业头部客户，产品完成从 0 到 1 的验证。',
        honor: false
      },
      {
        year: '2014',
        title: '获得高新技术企业认定',
        desc: '研发投入与自主知识产权获得官方认可。',
        honor: true
      }
    ]
  },
  {
    range: '2015 - 2019',
    name: '成长期',
    theme: '打磨产品，规模复制',
    events: [
      {
        year: '2015',
        title: '产品线成型',
        desc: '三大产品线发布，完成从项目交付到标准产品的转型。',
        honor: false
      },
      {
        year: '2017',
        title: '服务客户突破 500 家',
        desc: '建立全国服务网络，交付与售后体系全面标准化。',
        honor: false
      },
      {
        year: '2019',
        title: '通过 ISO 9001 认证',
        desc: '质量管理体系与国际标准接轨。',
        honor: true
      }
    ]
  },
  {
    range: '2020 - 2023',
    name: '领先期',
    theme: '行业头部，方案引领',
    events: [
      {
        year: '2020',
        title: '行业解决方案体系发布',
        desc: '覆盖制造、金融、零售、医疗四大行业的方案矩阵成型。',
        honor: false
      },
      {
        year: '2022',
        title: '服务客户突破 1200 家',
        desc: '续约率与客户满意度连续三年提升。',
        honor: false
      },
      {
        year: '2023',
        title: '斩获行业年度解决方案奖',
        desc: '头部客户的联合实践获得行业认可。',
        honor: true
      }
    ]
  },
  {
    range: '2024 -',
    name: '生态期',
    theme: '开放共建，长期主义',
    events: [
      {
        year: '2024',
        title: '通过 ISO 27001 认证',
        desc: '信息安全管理体系升级，为数据类业务护航。',
        honor: true
      },
      {
        year: '2025',
        title: '合作伙伴计划启动',
        desc: '与上下游厂商共建交付与培训生态。',
        honor: false
      }
    ]
  }
]

const rootEl = ref<HTMLElement | null>(null)
useScrollReveal(rootEl)

onMounted(() => {
  const siteName = blogSettingsStore.blogSettings?.blog_name || '知企建站系统'
  applySeo({
    title: `发展历程 - ${siteName}`,
    description: blogSettingsStore.blogSettings?.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
})
</script>

<style scoped>
@import './about-shared.css';

/* ---------- 时间轴 ---------- */
.tl {
  max-width: 880px;
}

.tl-stage-head {
  display: flex;
  align-items: baseline;
  gap: var(--mo-sp-5);
  flex-wrap: wrap;
  margin: var(--mo-sp-9) 0 var(--mo-sp-6);
}

.tl-stage-head:first-child {
  margin-top: 0;
}

.tl-range {
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: 999px;
}

.tl-stage-head h2 {
  margin: 0;
  font-size: var(--mo-fs-2xl);
  color: var(--tpl-ink-900);
}

.tl-stage-head p {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--tpl-ink-400);
}

.tl-events {
  border-left: 2px solid var(--tpl-border);
  margin-left: 7px;
  padding-left: var(--mo-sp-7);
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-7);
}

.tl-event {
  position: relative;
  display: flex;
  gap: var(--mo-sp-5);
  align-items: flex-start;
}

/* 节点：普通圆形 / 荣誉方形 */
.tl-node {
  position: absolute;
  left: calc(-1 * var(--mo-sp-7) - 8px);
  top: 6px;
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--tpl-p);
  box-shadow: 0 0 0 3px var(--tpl-bg);
}

.tl-node.square {
  border-radius: 3px;
}

.tl-year {
  font-size: var(--mo-fs-sm);
  font-weight: 500;
  color: var(--tpl-p-text);
  font-variant-numeric: tabular-nums;
}

.tl-body h3 {
  margin: var(--mo-sp-1) 0 var(--mo-sp-2);
  font-size: var(--mo-fs-lg);
  color: var(--tpl-ink-900);
  display: flex;
  align-items: center;
  gap: var(--mo-sp-3);
}

.tl-body p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-500);
  line-height: 1.75;
  max-width: 620px;
}

.tag {
  display: inline-block;
  font-size: var(--mo-fs-xs);
  font-weight: 500;
  color: var(--tpl-p-text);
  background: var(--tpl-p-sub);
  padding: 2px var(--mo-sp-3);
  border-radius: 999px;
}

.tl-end {
  margin-top: var(--mo-sp-9);
  padding-left: var(--mo-sp-7);
  border-left: 2px solid var(--tpl-border);
  position: relative;
}

.tl-end .tl-node {
  left: -8px;
  top: 4px;
}

.tl-end p {
  margin: 0;
  font-size: var(--mo-fs-md);
  color: var(--tpl-ink-400);
}

@media (max-width: 639px) {
  .tl-events {
    padding-left: var(--mo-sp-5);
  }

  .tl-node {
    left: calc(-1 * var(--mo-sp-5) - 8px);
  }
}
</style>
