<template>
  <div class="tag-page">
    <SiteLayout>
      <div class="tag-shell">
        <header class="tag-head">
          <span class="tag-head-icon" aria-hidden="true">
            <el-icon><PriceTag /></el-icon>
          </span>
          <div class="tag-head-copy">
            <h1 class="tag-head-title">文章标签</h1>
            <p class="tag-head-desc">
              共 {{ tags.length }} 个标签 · 字号越大表示该标签下的文章越多
            </p>
          </div>
        </header>

        <div v-loading="loading" class="tag-cloud-wrap">
          <div v-if="tags.length > 0" class="tag-cloud">
            <router-link
              v-for="tag in tags"
              :key="tag.id ?? tag.name"
              :to="tag.id ? `/tag/${tag.id}` : '/tag'"
              class="tag-chip"
              :class="[`is-level-${levelOf(tag)}`, { 'is-empty': !tag.articleCount }]"
              :style="{ '--tag-accent': tag.color || undefined }"
              :title="`${tag.name}：${tag.articleCount} 篇文章`"
            >
              <span class="tag-dot" aria-hidden="true"></span>
              <span class="tag-name">{{ tag.name }}</span>
              <span v-if="tag.articleCount" class="tag-count">{{ tag.articleCount }}</span>
            </router-link>
          </div>

          <el-empty v-else-if="!loading" description="暂无标签">
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </el-empty>
        </div>
      </div>
    </SiteLayout>
  </div>
</template>

<script setup lang="ts" name="SiteTagOverview">
import { computed, onMounted, ref } from 'vue'
import { PriceTag } from '@element-plus/icons-vue'
import SiteLayout from '@/components/SiteLayout.vue'
import { getTagCloud } from '@/api/site/tag'

interface TagCloudItem {
  id?: number
  name: string
  color?: string
  articleCount: number
}

const loading = ref(false)
const tags = ref<TagCloudItem[]>([])

// 接口返回的是 article_count（snake_case map），此前页面按 articleCount 读取，
// 计数永远取不到值、尺寸只能退化成「按索引取模」。这里统一归一化后按热度排序。
const normalize = (list: unknown): TagCloudItem[] =>
  (Array.isArray(list) ? list : [])
    .filter(
      (item): item is Record<string, unknown> =>
        !!item && typeof item === 'object' && typeof (item as TagCloudItem).name === 'string'
    )
    .map(item => ({
      id: typeof item.id === 'number' ? item.id : undefined,
      name: String(item.name),
      color: typeof item.color === 'string' && item.color ? item.color : undefined,
      articleCount: Number(item.article_count) || 0
    }))
    .sort((a, b) => b.articleCount - a.articleCount || a.name.localeCompare(b.name))

const maxArticles = computed(() =>
  tags.value.reduce((max, tag) => Math.max(max, tag.articleCount), 0)
)

// 相对热度分 5 级：字号由真实文章数决定，全站还没有文章时不做分级。
const levelOf = (tag: TagCloudItem): number => {
  if (maxArticles.value === 0) return 1
  if (tag.articleCount === 0) return 0
  const ratio = tag.articleCount / maxArticles.value
  if (ratio >= 0.8) return 4
  if (ratio >= 0.6) return 3
  if (ratio >= 0.4) return 2
  return 1
}

const loadTags = async () => {
  loading.value = true
  try {
    const response = (await getTagCloud()) as any
    const list = Array.isArray(response)
      ? response
      : Array.isArray(response?.data)
        ? response.data
        : response?.rows || []
    tags.value = normalize(list)
  } catch {
    tags.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadTags)
</script>

<style scoped>
.tag-page {
  min-height: 100vh;
  background: var(--mo-n50);
}

.tag-shell {
  max-width: 1000px;
  margin: 0 auto;
  padding: var(--mo-sp-12) var(--mo-sp-5) var(--mo-sp-9);
}

.tag-head {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-4);
  margin-bottom: var(--mo-sp-7);
}

.tag-head-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: var(--mo-sp-9);
  height: var(--mo-sp-9);
  border-radius: var(--mo-r-lg);
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-size: var(--mo-fs-2xl);
}

.tag-head-title {
  margin: 0;
  font-size: var(--mo-fs-4xl);
  font-weight: 700;
  line-height: 1.2;
  color: var(--mo-n900);
}

.tag-head-desc {
  margin: var(--mo-sp-1) 0 0;
  font-size: var(--mo-fs-sm);
  color: var(--mo-n500);
}

.tag-cloud-wrap {
  min-height: var(--mo-sp-12);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--mo-sp-3);
}

/* 每个标签的强调色来自标签自身配置（--tag-accent 由内联变量给出），
   未配置或深色模式下回退到主题色。文字仍走中性/主题令牌，保证对比度。 */
.tag-chip {
  --tag-accent: var(--mo-p600);
  --tag-tint: var(--mo-n0);
  display: inline-flex;
  align-items: center;
  gap: var(--mo-sp-2);
  padding: var(--mo-sp-2) var(--mo-sp-4);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-full);
  background: var(--tag-tint);
  color: var(--mo-n800);
  font-weight: 500;
  text-decoration: none;
  box-shadow: var(--mo-shadow-sm);
  transition:
    color 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.tag-chip:hover {
  color: var(--mo-p700);
  border-color: color-mix(in srgb, var(--tag-accent) 55%, transparent);
  background: color-mix(in srgb, var(--tag-accent) 10%, var(--tag-tint));
  box-shadow: var(--mo-shadow-md);
  transform: translateY(-2px);
}

.tag-dot {
  flex: none;
  width: var(--mo-sp-2);
  height: var(--mo-sp-2);
  border-radius: var(--mo-r-full);
  background: var(--tag-accent);
}

.tag-name {
  font-size: var(--mo-fs-md);
  line-height: 1.2;
}

.tag-count {
  min-width: var(--mo-sp-4);
  padding: 0 var(--mo-sp-1);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
  text-align: center;
  color: var(--mo-n600);
  background: var(--mo-n100);
  border-radius: var(--mo-r-full);
}

/* 热度分级：越高频的标签字号越大、配色越浓 */
.tag-chip.is-level-2 .tag-name {
  font-size: var(--mo-fs-lg);
}

.tag-chip.is-level-3 .tag-name {
  font-size: var(--mo-fs-xl);
  font-weight: 600;
}

.tag-chip.is-level-4 .tag-name {
  font-size: var(--mo-fs-2xl);
  font-weight: 700;
}

.tag-chip.is-level-3,
.tag-chip.is-level-4 {
  background: color-mix(in srgb, var(--tag-accent) 12%, var(--tag-tint));
  border-color: color-mix(in srgb, var(--tag-accent) 35%, var(--tag-tint));
}

/* 该标签下没有已发布文章：降级为次要文字色，但保持 AA 对比度（不用 opacity，
   半透明叠白后 16px 正文会掉到 4.3:1 以下）。 */
.tag-chip.is-empty {
  color: var(--mo-n500);
}

.tag-chip.is-empty .tag-dot {
  background: var(--mo-n300);
}

html.dark .tag-head-icon {
  color: var(--mo-p300);
}

html.dark .tag-head-title {
  color: var(--mo-n100);
}

html.dark .tag-head-desc {
  color: var(--mo-n400);
}

html.dark .tag-chip {
  --tag-accent: var(--mo-p300);
  --tag-tint: var(--mo-n800);
  border-color: var(--mo-n700);
  color: var(--mo-n200);
}

html.dark .tag-chip:hover {
  color: var(--mo-p300);
}

html.dark .tag-chip.is-empty {
  color: var(--mo-n400);
}

html.dark .tag-count {
  color: var(--mo-n200);
  background: var(--mo-n700);
}

@media (max-width: 768px) {
  /* 顶部内边距必须大于固定导航高度（约 61px），否则标题会被压到导航下面 */
  .tag-shell {
    padding: var(--mo-sp-11) var(--mo-sp-4) var(--mo-sp-7);
  }

  .tag-head {
    gap: var(--mo-sp-3);
    margin-bottom: var(--mo-sp-5);
  }

  .tag-head-icon {
    width: var(--mo-sp-8);
    height: var(--mo-sp-8);
    font-size: var(--mo-fs-xl);
  }

  .tag-head-title {
    font-size: var(--mo-fs-3xl);
  }

  .tag-cloud {
    gap: var(--mo-sp-2);
  }

  .tag-chip.is-level-4 .tag-name {
    font-size: var(--mo-fs-xl);
  }

  .tag-chip.is-level-3 .tag-name {
    font-size: var(--mo-fs-lg);
  }
}
</style>
