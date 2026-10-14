<template>
  <div class="author-page">
    <BlogLayout>
      <div class="author-container">
        <div v-if="loading" v-loading="loading" class="author-loading"></div>

        <template v-else>
          <header class="author-header">
            <div class="author-avatar">{{ (authorName || '作').charAt(0) }}</div>
            <div class="author-info">
              <h1>{{ authorName || '该作者' }}</h1>
              <p>共发布 {{ total }} 篇文章</p>
            </div>
          </header>

          <div v-if="articles.length > 0" class="author-list">
            <article
              v-for="article in articles"
              :key="article.id"
              class="author-article"
              @click="goArticle(article.id)"
            >
              <h3 class="aa-title">{{ article.title }}</h3>
              <p v-if="article.summary" class="aa-excerpt">{{ article.summary }}</p>
              <div class="aa-meta">
                <span>{{ formatDate(article.createTime) }}</span>
                <span>👁 {{ article.viewCount || 0 }}</span>
                <span>💬 {{ article.commentCount || 0 }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="该作者还没有发布文章">
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
          </el-empty>
        </template>
      </div>
    </BlogLayout>
  </div>
</template>

<script setup lang="ts" name="BlogAuthor">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BlogLayout from '@/components/BlogLayout.vue'
import { getArticleList } from '@/api/blog/article'
import { ElMessage } from '@/plugins/element-plus-service'
import { logger } from '@/utils/logger'
import { parseTime } from '@/utils/zhi'

const route = useRoute()
const router = useRouter()

const articles = ref([])
const authorName = ref('')
const total = ref(0)
const loading = ref(false)

const authorId = computed(() => {
  const raw = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
  const id = Number(raw)
  return Number.isFinite(id) && id > 0 ? id : 0
})

const formatDate = (date: string) => parseTime(date, '{y}-{m}-{d}')

const loadAuthorArticles = async () => {
  const id = authorId.value
  if (!id) return
  loading.value = true
  try {
    const params = { pageNum: 1, pageSize: 50, status: 1, authorId: id }
    const response = await getArticleList(params)
    const rows = (response && (response.rows || response.data)) || []
    articles.value = Array.isArray(rows) ? rows : []
    total.value = response.total || rows.length || 0
    const first = rows[0] as any
    if (first && first.authorName) {
      authorName.value = first.authorName
    }
  } catch (error) {
    logger.error('获取作者文章失败:', error)
    ElMessage.error('获取文章列表失败')
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const goArticle = (id: number) => {
  if (id) router.push(`/article/${id}`)
}

onMounted(loadAuthorArticles)
watch(authorId, loadAuthorArticles)
</script>

<style scoped>
.author-page {
  min-height: 100vh;
}
.author-container {
  max-width: 860px;
  margin: 0 auto;
  padding: var(--mo-sp-12) var(--mo-sp-5) var(--mo-sp-9);
}
.author-loading {
  min-height: 160px;
}
.author-header {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-4);
  margin-bottom: var(--mo-sp-6);
  padding: var(--mo-sp-5);
  background: var(--el-bg-color-overlay, #fff);
  border-radius: var(--mo-r-lg);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
}
.author-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mo-p100);
  color: var(--mo-p600);
  font-size: var(--mo-fs-2xl);
  font-weight: 700;
  flex-shrink: 0;
}
.author-info h1 {
  margin: 0 0 var(--mo-sp-1);
  font-size: 22px;
  color: var(--el-text-color-primary, #303133);
}
.author-info p {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}
.author-article {
  padding: 16px 18px;
  margin-bottom: var(--mo-sp-3);
  background: var(--el-bg-color-overlay, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.author-article:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}
.aa-title {
  margin: 0 0 var(--mo-sp-2);
  font-size: 17px;
  color: var(--el-text-color-primary, #303133);
}
.author-article:hover .aa-title {
  color: var(--mo-primary-text, #409eff);
}
.aa-excerpt {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-regular, #606266);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.aa-meta {
  display: flex;
  gap: var(--mo-sp-3);
  font-size: var(--mo-fs-xs);
  color: var(--el-text-color-secondary, #909399);
}
</style>
