<template>
  <div class="bookmark-page">
    <BlogLayout>
      <div class="bookmark-container">
        <div class="bookmark-header">
          <h1 class="bookmark-title">我的收藏</h1>
          <p class="bookmark-desc">收藏过的文章都在这里，随时回来继续阅读</p>
        </div>

        <div v-if="loading" v-loading="loading" class="bookmark-loading"></div>

        <template v-else-if="bookmarks.length > 0">
          <div class="bookmark-grid">
            <div v-for="item in bookmarks" :key="item.bookmarkId" class="bookmark-card">
              <div class="bookmark-card-main" @click="goDetail(item)">
                <h3 class="bookmark-article-title">{{ item.title }}</h3>
                <p v-if="item.summary" class="bookmark-summary">{{ item.summary }}</p>
                <div class="bookmark-meta">
                  <span class="meta-item">👁 {{ item.viewCount || 0 }}</span>
                  <span class="meta-item">👍 {{ item.likeCount || 0 }}</span>
                  <span class="meta-item">💬 {{ item.commentCount || 0 }}</span>
                  <span class="meta-item">收藏于 {{ formatDate(item.bookmarkTime) }}</span>
                </div>
              </div>
              <div class="bookmark-card-actions">
                <el-button size="small" type="primary" link @click="goDetail(item)">
                  阅读全文
                </el-button>
                <el-button size="small" type="danger" link @click="removeBookmark(item)">
                  取消收藏
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <el-empty v-else description="还没有收藏任何文章">
          <el-button type="primary" @click="$router.push('/')">去首页看看</el-button>
        </el-empty>
      </div>
    </BlogLayout>
  </div>
</template>

<script setup lang="ts" name="BlogBookmarks">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBookmarkList, toggleBookmark } from '@/api/blog/bookmark'
import { ElMessage } from '@/plugins/element-plus-service'
import { useUserStore } from '@/stores/user'
import BlogLayout from '@/components/BlogLayout.vue'

interface BookmarkArticle {
  bookmarkId: number
  bookmarkTime: string
  articleId: number
  title: string
  summary?: string
  coverUrl?: string
  authorName?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
}

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const bookmarks = ref<BookmarkArticle[]>([])

const formatDate = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return time
  return date.toLocaleDateString('zh-CN')
}

const loadBookmarks = async () => {
  if (!userStore.token) {
    ElMessage.info('请先登录后再查看收藏')
    router.push(`/login?redirect=${encodeURIComponent('/bookmarks')}`)
    return
  }
  loading.value = true
  try {
    const response = await getBookmarkList()
    const list = (response && response.data) || []
    bookmarks.value = Array.isArray(list) ? list : []
  } catch (error) {
    // 静默失败，展示空态
    ElMessage.error('获取收藏列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goDetail = (item: BookmarkArticle) => {
  router.push(`/article/${item.articleId}`)
}

const removeBookmark = async (item: BookmarkArticle) => {
  try {
    const response = await toggleBookmark(item.articleId)
    const bookmarked = response?.data?.bookmarked
    if (!bookmarked) {
      bookmarks.value = bookmarks.value.filter(b => b.articleId !== item.articleId)
      ElMessage.success('已取消收藏')
    }
  } catch (error) {
    // 静默失败
    ElMessage.error('操作失败，请稍后重试')
  }
}

onMounted(loadBookmarks)
</script>

<style scoped>
.bookmark-page {
  min-height: 100vh;
}

.bookmark-container {
  max-width: 1080px;
  margin: 0 auto;
  padding: var(--mo-sp-12) var(--mo-sp-5) var(--mo-sp-8);
}

.bookmark-header {
  margin-bottom: var(--mo-sp-6);
}

.bookmark-title {
  margin: 0 0 var(--mo-sp-2);
  font-size: 26px;
  font-weight: 700;
  color: var(--el-text-color-primary, #303133);
}

.bookmark-desc {
  margin: 0;
  font-size: var(--mo-fs-sm);
  color: var(--el-text-color-secondary, #606266);
}

.bookmark-loading {
  min-height: 200px;
}

.bookmark-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--mo-sp-4);
}

.bookmark-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
  background: var(--el-bg-color-overlay, #ffffff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.bookmark-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.bookmark-card-main {
  cursor: pointer;
}

.bookmark-article-title {
  margin: 0 0 var(--mo-sp-2);
  font-size: 17px;
  line-height: 1.4;
  color: var(--el-text-color-primary, #303133);
}

.bookmark-article-title:hover {
  color: var(--mo-primary-text, #409eff);
}

.bookmark-summary {
  margin: 0 0 var(--mo-sp-3);
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-regular, #606266);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bookmark-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-3);
  font-size: var(--mo-fs-xs);
  color: var(--el-text-color-secondary, #909399);
}

.bookmark-card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--mo-sp-3);
  padding-top: 10px;
  border-top: 1px dashed var(--el-border-color-lighter, #ebeef5);
}

@media (max-width: 480px) {
  .bookmark-grid {
    grid-template-columns: 1fr;
  }
}
</style>
