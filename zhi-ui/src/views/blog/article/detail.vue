<template>
  <div class="article-detail-container" :class="{ 'tpl-site': isTplMode }">
    <!-- 企业官网模板：阅读进度条（方案文档 §3.4） -->
    <div v-if="isTplMode" class="read-progress" :style="{ width: readProgress + '%' }"></div>
    <!-- 站点导航 -->
    <BlogLayout>
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-skeleton">
          <el-skeleton :loading="loading" animated class="article-skeleton">
            <template #template>
              <div class="skeleton-content">
                <el-skeleton-item variant="h1" style="width: 60%; margin-bottom: 20px" />
                <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 10px" />
                <el-skeleton-item variant="text" style="width: 90%; margin-bottom: 10px" />
                <el-skeleton-item variant="text" style="width: 80%; margin-bottom: 20px" />
                <el-skeleton-item
                  variant="rect"
                  style="width: 100%; height: 400px; margin-bottom: 20px"
                />
                <el-skeleton-item
                  variant="text"
                  style="width: 100%; height: 20px; margin-bottom: 8px"
                />
                <el-skeleton-item
                  variant="text"
                  style="width: 95%; height: 20px; margin-bottom: 8px"
                />
                <el-skeleton-item
                  variant="text"
                  style="width: 85%; height: 20px; margin-bottom: 8px"
                />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- 文章不存在 -->
      <div v-else-if="!article" class="not-found-container">
        <div class="not-found-content">
          <el-icon class="not-found-icon"><DocumentCopy /></el-icon>
          <h2>文章不存在</h2>
          <p>抱歉，您访问的文章不存在或已被删除。</p>
          <router-link to="/" class="back-home-btn">
            <el-button type="primary" size="large">返回首页</el-button>
          </router-link>
        </div>
      </div>

      <div v-else class="mo-article-page">
        <header class="mo-article-header">
          <span v-if="article.categoryName" class="cat-badge">{{ article.categoryName }}</span>
          <h1>{{ article.title }}</h1>
          <div class="a-meta">
            <span class="author">{{ article.authorName || article.author || '匿名作者' }}</span>
            <span>·</span>
            <time :datetime="article.createTime">{{ formatDate(article.createTime) }}</time>
            <span v-if="isFeatureEnabled('view_count_enabled')">·</span>
            <span v-if="isFeatureEnabled('view_count_enabled')">
              👁 {{ article.viewCount || 0 }} 阅读
            </span>
            <span>·</span>
            <span>⏱ 约 {{ readingMinutes }} 分钟</span>
          </div>
        </header>

        <main class="body-layout">
          <aside class="toc">
            <ArticleTOC
              v-if="article && article.content"
              :content="article.content"
              @toc-ready="handleTOCReady"
            />
          </aside>

          <article class="article-content">
            <div v-if="article.coverUrl" class="article-cover">
              <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
            </div>
            <div class="content-body" v-html="processedContent"></div>

            <div class="article-actions">
              <el-button
                v-if="isFeatureEnabled('like_enabled')"
                :loading="likeLoading"
                :type="article.isLiked ? 'success' : 'primary'"
                plain
                @click="handleLike"
              >
                👍 点赞 {{ article.likeCount || 0 }}
              </el-button>
              <ShareButton v-if="isFeatureEnabled('share_enabled')" :article="article" />
              <el-button
                :type="article.isBookmarked ? 'warning' : 'default'"
                plain
                @click="handleBookmark"
              >
                {{ article.isBookmarked ? '已收藏' : '收藏' }}
              </el-button>
            </div>

            <div v-if="prevArticle || nextArticle" class="article-navigation">
              <router-link
                v-if="prevArticle"
                :to="{
                  name: 'PublicBlogArticleDetail',
                  params: { id: prevArticle.id ?? prevArticle.articleId ?? prevArticle.uuid }
                }"
                class="nav-item"
              >
                <span class="nav-label">上一篇</span>
                <span class="nav-title">{{ prevArticle.title }}</span>
              </router-link>
              <router-link
                v-if="nextArticle"
                :to="{
                  name: 'PublicBlogArticleDetail',
                  params: { id: nextArticle.id ?? nextArticle.articleId ?? nextArticle.uuid }
                }"
                class="nav-item next-article"
              >
                <span class="nav-label">下一篇</span>
                <span class="nav-title">{{ nextArticle.title }}</span>
              </router-link>
            </div>
          </article>

          <aside class="article-side">
            <div class="author-card">
              <div class="a-avatar">{{ authorInitial }}</div>
              <div class="a-name">{{ article.authorName || article.author || '匿名作者' }}</div>
              <div class="a-bio">
                {{ blogSettings.blog_desc || '记录技术、产品与生活里的认真思考。' }}
              </div>
              <button class="btn btn-primary btn-sm" type="button">+ 关注</button>
              <div class="a-stats">
                <div class="a-stat">
                  <div class="num">{{ relatedArticles.length || 0 }}</div>
                  <div class="lbl">相关</div>
                </div>
                <div class="a-stat">
                  <div class="num">{{ article.likeCount || 0 }}</div>
                  <div class="lbl">获赞</div>
                </div>
                <div class="a-stat">
                  <div class="num">{{ articleCommentCount }}</div>
                  <div class="lbl">评论</div>
                </div>
              </div>
            </div>

            <div v-if="article.tags && article.tags.length" class="side-widget">
              <div class="wt">文章标签</div>
              <div class="tag-cloud">
                <router-link
                  v-for="tag in article.tags"
                  :key="tag.id"
                  :to="`/blog/tag/${tag.id}`"
                  class="tc"
                >
                  {{ tag.name }}
                </router-link>
              </div>
            </div>

            <div v-if="relatedArticles.length > 0" class="side-widget">
              <div class="wt">相关推荐</div>
              <div class="related-mini-list">
                <router-link
                  v-for="related in relatedArticles.slice(0, 4)"
                  :key="related.id"
                  :to="{
                    name: 'PublicBlogArticleDetail',
                    params: { id: related.id ?? related.articleId ?? related.uuid }
                  }"
                  class="related-mini"
                >
                  {{ related.title }}
                </router-link>
              </div>
            </div>
          </aside>
        </main>

        <section v-if="isFeatureEnabled('comment_enabled')" class="comment-section">
          <div class="comment-head">
            <h3>💬 评论 ({{ articleCommentCount }})</h3>
            <div v-if="totalComments > 1" class="comment-sort">
              <button
                type="button"
                :class="{ active: commentSort === 'newest' }"
                @click="changeCommentSort('newest')"
              >
                最新
              </button>
              <button
                type="button"
                :class="{ active: commentSort === 'oldest' }"
                @click="changeCommentSort('oldest')"
              >
                最早
              </button>
            </div>
          </div>

          <div class="comment-input">
            <el-form
              ref="commentFormRef"
              :model="commentForm"
              :rules="commentRules"
              label-width="0"
            >
              <div v-if="!isLoggedIn" class="guest-fields">
                <el-form-item prop="nickname">
                  <el-input v-model="commentForm.nickname" placeholder="昵称" />
                </el-form-item>
                <el-form-item prop="email">
                  <el-input v-model="commentForm.email" placeholder="邮箱（可选）" />
                </el-form-item>
              </div>
              <el-form-item prop="content">
                <el-input
                  v-model="commentForm.content"
                  type="textarea"
                  :placeholder="replyTarget ? `回复 ${replyTarget.nickname}...` : '写下你的想法...'"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  @keydown="handleCommentKeydown"
                />
              </el-form-item>
              <div class="actions">
                <span class="md-hint">Ctrl/⌘ + Enter 发送</span>
                <el-button v-if="replyTarget" plain @click="cancelReply">取消回复</el-button>
                <el-button type="primary" :loading="commentSubmitting" @click="submitComment">
                  发表评论
                </el-button>
              </div>
            </el-form>
          </div>

          <div v-if="commentList.length > 0" class="comment-list">
            <div v-for="comment in commentList" :key="comment.id" class="comment-item">
              <div class="c-avatar">{{ (comment.nickname || '匿').charAt(0) }}</div>
              <div class="c-body">
                <div class="c-head">
                  <span class="c-name">{{ comment.nickname || '匿名' }}</span>
                  <span class="c-time">{{ formatDate(comment.createTime) }}</span>
                </div>
                <div class="c-text">{{ comment.content }}</div>
                <div class="c-actions">
                  <span @click="handleLikeComment(comment)">
                    {{ comment.liked ? '❤️' : '👍' }} {{ comment.likeCount || 0 }}
                  </span>
                  <span @click="handleReply(comment)">💬 回复</span>
                  <template v-if="isMyComment(comment)">
                    <span class="c-edit" @click="editMyComment(comment)">✏️ 编辑</span>
                    <span class="c-delete" @click="removeMyComment(comment)">🗑️ 删除</span>
                  </template>
                </div>

                <div v-if="comment.replies && comment.replies.length > 0" class="c-reply">
                  <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <div class="c-avatar small">{{ (reply.nickname || '匿').charAt(0) }}</div>
                    <div class="c-body">
                      <div class="c-head">
                        <span class="c-name">{{ reply.nickname || '匿名' }}</span>
                        <span class="c-time">{{ formatDate(reply.createTime) }}</span>
                      </div>
                      <div class="c-text">{{ reply.content }}</div>
                      <div class="c-actions">
                        <span @click="handleLikeComment(reply)">
                          {{ reply.liked ? '❤️' : '👍' }} {{ reply.likeCount || 0 }}
                        </span>
                        <span @click="handleReply(reply)">💬 回复</span>
                        <template v-if="isMyComment(reply)">
                          <span class="c-edit" @click="editMyComment(reply)">✏️ 编辑</span>
                          <span class="c-delete" @click="removeMyComment(reply)">🗑️ 删除</span>
                        </template>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <BlogPager
            :total="totalComments"
            :page-size="COMMENT_PAGE_SIZE"
            :page-num="commentPage"
            @page-change="handleCommentPageChange"
          />
        </section>
      </div>

      <!-- 站点底部 -->
    </BlogLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import BlogLayout from '@/components/BlogLayout.vue'
import ArticleTOC from '@/components/ArticleTOC.vue'
import ShareButton from '@/components/ShareButton.vue'
import BlogPager from '@/components/BlogPager.vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import { getArticleDetail, getRelatedArticles } from '@/api/blog/article'
import {
  getArticleLikeStatus,
  toggleArticleLike,
  toggleCommentLike,
  getCommentLikedStatuses
} from '@/api/blog/like'
import { toggleBookmark } from '@/api/blog/bookmark'

import {
  getArticleComments,
  addBlogComment as apiSubmitComment,
  updateMyComment,
  deleteMyComment
} from '@/api/blog/comment'
import { getBlogSettings, getBlogSettingsAnonymous } from '@/api/blog/setting'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { createRequestGuard } from '@/utils/requestGuard'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'

// JSON-LD 结构化数据随路由卸载移除，避免残留到下一页
onUnmounted(() => {
  removeJsonLd('zh-jsonld-article')
})

// 企业官网模板模式：阅读进度条 + 模板皮肤开关
const isTplMode = computed(
  () => String(blogSettings.value?.home_layout || '') === 'business'
)
const readProgress = ref(0)

const updateReadProgress = () => {
  const doc = document.documentElement
  const scrollable = doc.scrollHeight - window.innerHeight
  readProgress.value = scrollable > 0 ? Math.min(100, (window.scrollY / scrollable) * 100) : 0
}

onMounted(() => {
  window.addEventListener('scroll', updateReadProgress, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateReadProgress)
})
import { logger } from '@/utils/logger'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const blogSettingsStore = useBlogSettingsStore()

// 响应式数据
const article = ref(null)
const prevArticle = ref(null)
const nextArticle = ref(null)
const relatedArticles = ref([])
const commentList = ref([])
const totalComments = ref(0)
const commentPage = ref(1)
const commentSort = ref<'newest' | 'oldest'>('newest')
const COMMENT_PAGE_SIZE = 10
const loading = ref(false)
const likeLoading = ref(false)
const commentSubmitting = ref(false)
const isLoggedIn = ref(false)
const blogSettings = computed(() => blogSettingsStore.blogSettings)
const isFeatureEnabled = (feature: string) => blogSettingsStore.isFeatureEnabled(feature)
const tocItems = ref([])

// 处理文章内容，为标题添加ID并消毒内容
const processedContent = computed(() => {
  if (!article.value || !article.value.content) return ''

  // 首先消毒HTML内容，防止XSS攻击
  const sanitizedContent = sanitizeArticleContent(article.value.content)

  // 然后为标题添加ID，用于目录导航
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = sanitizedContent

  const headingElements = tempDiv.querySelectorAll('h1, h2, h3, h4, h5, h6')
  const idSet = new Set()

  headingElements.forEach((heading, index) => {
    let id = `heading-${index}`
    if (idSet.has(id)) {
      let counter = 1
      while (idSet.has(`${id}-${counter}`)) {
        counter++
      }
      id = `${id}-${counter}`
    }
    idSet.add(id)
    heading.id = id
  })

  return tempDiv.innerHTML
})

const readingMinutes = computed(() => {
  const text = article.value?.content?.replace(/<[^>]+>/g, '') || ''
  return Math.max(1, Math.ceil(text.length / 500))
})

const authorInitial = computed(() => {
  const name = article.value?.authorName || article.value?.author || '匿'
  return name.charAt(0)
})

// 评论总数（含回复的已发布数）：以文章详情/评论增删改返回的 commentCount 为准
const articleCommentCount = computed(() => {
  const count = Number(article.value?.commentCount)
  return Number.isFinite(count) ? count : totalComments.value
})

// 处理目录就绪事件
const handleTOCReady = items => {
  tocItems.value = items
}

// 评论表单
const commentForm = reactive({
  nickname: '',
  email: '',
  content: '',
  parentId: null as number | null
})

// 回复目标评论
const replyTarget = ref<{ id: number; nickname: string } | null>(null)

const commentRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 1, max: 500, message: '评论内容长度在1到500个字符', trigger: 'blur' }
  ]
}

const commentFormRef = ref<any>(null)

// 获取文章详情
const detailGuard = createRequestGuard()

const loadArticleDetail = async () => {
  const detailToken = detailGuard.next()
  try {
    loading.value = true
    // 路由参数兼容数字 ID（历史链接）与 slug 伪静态链接，由后端统一解析
    const articleKey = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id

    // 验证文章标识
    if (!articleKey) {
      logger.error('文章ID为空')
      ElMessage.error('文章ID不能为空')
      return
    }

    // 获取文章详情
    const response = await getArticleDetail(articleKey)
    // 快速切换文章时，旧详情不能覆盖新详情
    if (!detailGuard.isLatest(detailToken)) return

    // 检查响应状态
    if (response.code !== 200) {
      logger.error('API调用失败:', response.code, response.msg)
      return
    }

    if (!response.data) {
      logger.error('响应数据为空')
      return
    }

    // 正确解析API响应数据结构
    if (response.data.article) {
      // 提取文章主体数据
      article.value = response.data.article

      // 提取上下篇文章数据
      if (response.data.extraInfo) {
        prevArticle.value = response.data.extraInfo.prevArticle || null
        nextArticle.value = response.data.extraInfo.nextArticle || null

        // 设置分类名称
        if (response.data.extraInfo.category && response.data.extraInfo.category.name) {
          article.value.categoryName = response.data.extraInfo.category.name
        }
      }

      // 获取相关文章（slug 入口时用响应里的真实 ID）
      try {
        const relatedResponse = await getRelatedArticles(article.value.id!)
        relatedArticles.value = relatedResponse.data || []
      } catch (e) {
        // 相关文章加载失败不影响主内容
        relatedArticles.value = []
      }

      // 获取评论列表
      await loadComments()

      // 登录用户：回显文章点赞状态
      fetchArticleLikeStatus()

      // 应用文章级 SEO（TDK 优先用文章级字段，回退标题/摘要）
      const siteName = (blogSettings.value as any)?.blog_name || '知企建站系统'
      applySeo({
        title: article.value.seoTitle || `${article.value.title} - ${siteName}`,
        description: article.value.seoDescription || article.value.summary || '',
        keywords: article.value.seoKeywords || '',
        canonical: canonicalUrl((blogSettings.value as any)?.seo_canonical_url)
      })
      // JSON-LD 结构化数据（Article）
      setJsonLd('zh-jsonld-article', {
        '@context': 'https://schema.org',
        '@type': 'Article',
        headline: article.value.title,
        description: article.value.seoDescription || article.value.summary || undefined,
        image: article.value.coverUrl || undefined,
        datePublished: article.value.createTime || undefined,
        author: { '@type': 'Person', name: article.value.authorName || 'admin' }
      })
    } else {
      logger.error('未找到文章数据，响应数据:', response.data)
      article.value = null
    }
  } catch (error: any) {
    logger.error('获取文章详情失败，详细错误:', error)
    logger.error('错误类型:', typeof error)
    logger.error('错误状态:', error.response?.status)
    logger.error('错误状态文本:', error.response?.statusText)
    logger.error('请求URL:', error.config?.url)
    logger.error('请求方法:', error.config?.method)

    // 更详细的错误提示
    const errorMsg =
      error.response?.status === 404
        ? `系统接口404异常，请求路径: ${error.config?.url}`
        : `获取文章详情失败: ${error.message || '未知错误'}`

    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

// 登录用户：回显当前页评论（含楼层内回复）的点赞态
const echoCommentLikedStatuses = async (comments: any[]) => {
  if (!isLoggedIn.value || !Array.isArray(comments) || comments.length === 0) return
  const ids: number[] = []
  const collect = (list: any[]) => {
    list.forEach(c => {
      const id = Number(c?.id)
      if (Number.isFinite(id) && id > 0) ids.push(id)
      if (Array.isArray(c?.replies)) collect(c.replies)
    })
  }
  collect(comments)
  if (ids.length === 0) return

  try {
    const likeResponse = await getCommentLikedStatuses(ids)
    const likedIds = Array.isArray(likeResponse?.data) ? likeResponse.data.map(Number) : []
    const apply = (list: any[]) => {
      list.forEach(c => {
        c.liked = likedIds.includes(Number(c.id))
        if (Array.isArray(c.replies)) apply(c.replies)
      })
    }
    apply(comments)
  } catch {
    // 点赞态回显失败不影响评论展示
  }
}

// 获取评论列表：顶级评论分页，回复随楼层返回；当前页越界（如删除后总页数减少）时回退到最后一页
const loadComments = async () => {
  try {
    // slug 入口时路由参数不是数字 ID，统一用详情响应里的真实 ID
    const articleId = article.value?.id
    if (!articleId) return
    const response = await getArticleComments(articleId, {
      pageNum: commentPage.value,
      pageSize: COMMENT_PAGE_SIZE,
      sort: commentSort.value
    })

    // 处理响应数据格式（TableDataInfo: { rows, total }）
    let comments = []
    if (response && response.rows) {
      comments = response.rows
      totalComments.value = Number(response.total) || 0
    } else if (response && response.code === 200) {
      comments = response.data || []
      totalComments.value = comments.length
    } else if (response && Array.isArray(response)) {
      comments = response
      totalComments.value = comments.length
    }

    if (comments.length === 0 && totalComments.value > 0 && commentPage.value > 1) {
      const lastPage = Math.max(1, Math.ceil(totalComments.value / COMMENT_PAGE_SIZE))
      if (lastPage !== commentPage.value) {
        commentPage.value = lastPage
        return loadComments()
      }
    }

    commentList.value = comments
    await echoCommentLikedStatuses(comments)
  } catch (error: any) {
    logger.error('获取评论列表失败:', error)
    commentList.value = []
    totalComments.value = 0
  }
}

// 评论增删改后同步服务端返回的最新评论数（已按审核口径结算）
const syncCommentCount = (response: any) => {
  const count = Number(response?.data?.commentCount)
  if (Number.isFinite(count) && article.value) {
    article.value.commentCount = count
  }
}

// 切换评论排序（最新/最早），回到第一页
const changeCommentSort = async (sort: 'newest' | 'oldest') => {
  if (commentSort.value === sort) return
  commentSort.value = sort
  commentPage.value = 1
  await loadComments()
}

// 评论翻页
const handleCommentPageChange = async (page: number) => {
  commentPage.value = page
  await loadComments()
  const section = document.querySelector('.comment-section')
  if (section) {
    section.scrollIntoView({ behavior: 'smooth' })
  }
}

// 查询文章点赞状态（登录后回显）
const fetchArticleLikeStatus = async () => {
  if (!isLoggedIn.value || !article.value) return
  try {
    const response = await getArticleLikeStatus(article.value.id)
    const data = response?.data
    if (data) {
      article.value.isLiked = !!data.liked
      if (typeof data.likeCount === 'number') {
        article.value.likeCount = data.likeCount
      }
    }
  } catch {
    // 点赞态回显失败不影响文章展示
  }
}

// 点赞文章
const handleLike = async () => {
  if (likeLoading.value) return

  try {
    if (!isLoggedIn.value) {
      ElMessage.info('请先登录后再进行点赞')
      router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
      return
    }
    likeLoading.value = true

    // 调用点赞API（支持取消，返回最新状态）
    const response = await toggleArticleLike(article.value.id)
    const data = response?.data || {}
    const liked = !!data.liked
    article.value.isLiked = liked
    if (typeof data.likeCount === 'number') {
      article.value.likeCount = data.likeCount
    }

    ElMessage.success(liked ? '点赞成功' : '已取消点赞')
  } catch (error: any) {
    logger.error('点赞失败:', error)
    ElMessage.error('操作失败')
  } finally {
    likeLoading.value = false
  }
}

// 收藏文章
const handleBookmark = async () => {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录后再进行收藏')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }

  try {
    const response = await toggleBookmark(article.value.id)
    if (response.code === 200) {
      article.value.isBookmarked = response.data.bookmarked
      ElMessage.success(article.value.isBookmarked ? '收藏成功' : '取消收藏')
    }
  } catch (error: any) {
    logger.error('收藏失败:', error)
    ElMessage.error('操作失败')
  }
}

// 回复评论（游客与登录用户均可回复；游客使用昵称/邮箱字段）
const handleReply = comment => {
  replyTarget.value = { id: comment.id, nickname: comment.nickname || '匿名' }
  // 滚动到评论表单
  const formElement = document.querySelector('.comment-input')
  if (formElement) {
    formElement.scrollIntoView({ behavior: 'smooth' })
  }
  ElMessage.info(`正在回复 ${replyTarget.value.nickname}，请输入回复内容`)
}

// 取消回复
const cancelReply = () => {
  replyTarget.value = null
  commentForm.parentId = null
}

// 点赞评论
const handleLikeComment = async (comment: any) => {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录后再给评论点赞')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return
  }
  try {
    const response = await toggleCommentLike(comment.id)
    const data = response?.data || {}
    const liked = !!data.liked
    comment.liked = liked
    if (typeof data.likeCount === 'number') {
      comment.likeCount = data.likeCount
    }
  } catch (error: any) {
    logger.error('评论点赞失败:', error)
    ElMessage.error('操作失败')
  }
}

// 评论快捷键：Ctrl/⌘ + Enter 发送
const handleCommentKeydown = (event: KeyboardEvent) => {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    submitComment()
  }
}

// 提交评论/回复（游客填写昵称/邮箱，登录用户自动使用账号信息）
const submitComment = async () => {
  try {
    await commentFormRef.value.validate()

    commentSubmitting.value = true

    const commentData: any = {
      articleId: article.value.id,
      content: commentForm.content
    }

    // 游客评论：附带昵称与可选邮箱
    if (!isLoggedIn.value) {
      commentData.nickname = commentForm.nickname.trim()
      if (commentForm.email.trim()) {
        commentData.email = commentForm.email.trim()
      }
    }

    // 如果是回复评论，添加 parentId
    const isReply = !!replyTarget.value
    if (isReply) {
      commentData.parentId = replyTarget.value.id
    }

    const response = await apiSubmitComment(commentData)
    syncCommentCount(response)

    // 开启审核时新评论不会立即出现在列表里，提示口径要和服务端一致
    const action = isReply ? '回复' : '评论'
    ElMessage.success(
      isFeatureEnabled('comment_review') ? `${action}已提交，审核通过后展示` : `${action}发表成功`
    )
    // 重置表单
    commentForm.content = ''
    commentForm.parentId = null
    replyTarget.value = null
    // 新发表的顶级评论回到第一页（最新排序下立即可见）
    if (!isReply && commentSort.value === 'newest') {
      commentPage.value = 1
    }
    await loadComments()
  } catch (error: any) {
    logger.error('提交评论失败:', error)
    if (error !== 'validation_failed') {
      ElMessage.error('评论发表失败')
    }
  } finally {
    commentSubmitting.value = false
  }
}

// 当前登录用户 ID（permission.ts 已在有 token 时调用 getInfo 填充 userStore.userId）
const currentUserId = computed(() => {
  const id = userStore.userId
  return id === undefined || id === null || id === '' ? null : Number(id)
})

// 是否为本人评论（仅登录用户且 userId 匹配才显示改删入口）
const isMyComment = (comment: any) => {
  const uid = currentUserId.value
  return isLoggedIn.value && uid !== null && !!comment && Number(comment.userId) === uid
}

// 编辑本人评论
const editMyComment = async (comment: any) => {
  try {
    const { value } = await ElMessageBox.prompt('修改评论', '编辑', {
      inputValue: comment.content,
      inputType: 'textarea',
      inputValidator: val => {
        const text = (val || '').trim()
        if (!text) return '评论内容不能为空'
        if (text.length > 500) return '评论内容长度不能超过500个字符'
        return true
      },
      confirmButtonText: '保存',
      cancelButtonText: '取消'
    })
    const response = await updateMyComment(comment.id, { content: value.trim() })
    syncCommentCount(response)
    // 编辑后若开启审核会回退为待审核并从列表暂时消失，提示口径要和服务端一致
    ElMessage.success(
      isFeatureEnabled('comment_review') ? '评论已更新，审核通过后展示' : '评论已更新'
    )
    await loadComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      logger.error('修改评论失败:', error)
      ElMessage.error('修改评论失败')
    }
  }
}

// 删除本人评论
const removeMyComment = async (comment: any) => {
  try {
    await ElMessageBox.confirm('确认删除这条评论吗？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const response = await deleteMyComment(comment.id)
    syncCommentCount(response)
    ElMessage.success('评论已删除')
    await loadComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      logger.error('删除评论失败:', error)
      ElMessage.error('删除评论失败')
    }
  }
}

// 日期格式化
const formatDate = dateString => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取站点设置
const loadBlogSettings = async () => {
  try {
    let response
    try {
      response = await getBlogSettings()
    } catch (error: any) {
      logger.warn('标准站点设置接口访问失败，尝试匿名接口:', error)
      response = await getBlogSettingsAnonymous()
    }

    let settings = {}
    if (response && response.code === 200) {
      settings = response.data || {}
    } else if (response && typeof response === 'object') {
      settings = response
    }

    // 更新 blogSettingsStore
    blogSettingsStore.updateBlogSettings(settings)
  } catch (error: any) {
    logger.error('获取站点设置失败:', error)
    // 使用默认值
  }
}

// 组件挂载时加载数据（评论在 loadArticleDetail 内随文章一起拉取）
onMounted(() => {
  loadArticleDetail()
  loadBlogSettings()
  isLoggedIn.value = !!userStore.token
})

// 监听路由参数变化，当文章ID变化时重新加载，Vue 3 会自动清理
watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId && newId !== oldId?.toString()) {
      // 滚动到顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
      // 重置文章数据
      article.value = null
      prevArticle.value = null
      nextArticle.value = null
      relatedArticles.value = []
      commentList.value = []
      totalComments.value = 0
      commentPage.value = 1
      // 重新加载文章详情（内部会重新拉取评论）
      loadArticleDetail()
    }
  },
  { immediate: false }
)
</script>

<style scoped>
.mo-article-page {
  min-height: 100vh;
  padding-top: 60px;
  background: var(--mo-n50);
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
}

/* ===== 深色模式：博文详情页 =====
   默认主题深色映射到 --el-* 变量（Tech Blue 深蓝），Mo-Blog 深色恢复棕色色阶。
   所有引用 --mo-n* 的亮色/深色覆盖规则自动适配。 */
html.dark .article-detail-container {
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

html.dark .mo-article-page {
  --mo-n900: var(--el-bg-color);
  --mo-n800: var(--el-bg-color-overlay);
  --mo-n700: var(--el-border-color);
  --mo-n600: var(--el-text-color-primary);
  --mo-n500: var(--el-text-color-regular);
  --mo-n400: var(--el-text-color-regular);
  --mo-n300: var(--el-text-color-regular);
  --mo-n200: var(--el-text-color-primary);
  --mo-n100: var(--el-text-color-primary);
  --mo-p50: rgba(0, 212, 255, 0.12);
  --mo-p100: rgba(0, 212, 255, 0.2);
  --mo-p300: var(--el-color-primary);
  --mo-p400: rgba(0, 212, 255, 0.5);
  --mo-p500: var(--el-color-primary);
  --mo-p600: var(--el-color-primary);
  --mo-p700: var(--el-color-primary);
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

/* Mo-Blog 主题深色：恢复棕色色阶 */
html.dark.theme-mo-blog .article-detail-container {
  /* 容器在 .mo-article-page 重映射作用域之外，取到的是浅色阶的原始值：
     n900 = #1c1917、n300 = #d6d3d1，与原写死值全等。 */
  color: var(--mo-n300);
}

html.dark.theme-mo-blog .mo-article-page {
  --mo-n900: #1c1917;
  --mo-n800: #292524;
  --mo-n700: #44403c;
  --mo-n600: #d6d3d1;
  --mo-n500: #a8a29e;
  /* 次要文字色，深色下必须保持浅灰（原 #78716c/#57534e 比背景还深） */
  --mo-n400: #a8a29e;
  --mo-n300: #a8a29e;
  --mo-n200: #d6d3d1;
  --mo-n100: #f5f5f4;
  --mo-p50: rgba(99, 102, 241, 0.12);
  --mo-p100: rgba(99, 102, 241, 0.2);
  --mo-p300: #a5b4fc;
  --mo-p400: rgba(129, 140, 248, 0.5);
  --mo-p500: #6366f1;
  --mo-p600: #4f46e5;
  --mo-p700: #4338ca;
  background: var(--mo-n900);
  color: var(--mo-n200);
}

/* 特殊覆盖 */
html.dark .mo-article-page .cat-badge,
html.dark .mo-article-page .content-body :deep(blockquote) {
  background: var(--mo-p50);
  color: var(--mo-p300);
}

/* 深色模式：标题/姓名/导航等文字改用浅色。
   --mo-n900/--mo-n800 在深色下被映射为背景色，不能直接用于文字。 */
html.dark .mo-article-page .mo-article-header h1,
html.dark .mo-article-page .a-name,
html.dark .mo-article-page .a-stat .num,
html.dark .mo-article-page .wt,
html.dark .mo-article-page .nav-title,
html.dark .mo-article-page .c-name,
html.dark .mo-article-page .a-meta .author,
html.dark .mo-article-page .not-found-content h2,
html.dark .mo-article-page .content-body :deep(h1),
html.dark .mo-article-page .content-body :deep(h2),
html.dark .mo-article-page .content-body :deep(h3),
html.dark .mo-article-page .content-body :deep(h4),
html.dark .mo-article-page .content-body :deep(h5),
html.dark .mo-article-page .content-body :deep(h6) {
  color: var(--mo-n100);
}

html.dark .mo-article-page .nav-title {
  color: var(--mo-n200);
}

html.dark .mo-article-page .article-actions :deep(.el-button.is-plain) {
  color: var(--mo-n200);
}

html.dark .mo-article-page .author-card,
html.dark .mo-article-page .side-widget,
html.dark .mo-article-page .nav-item,
html.dark .mo-article-page .comment-section,
html.dark .mo-article-page .comment-input,
html.dark .mo-article-page .c-reply,
html.dark .mo-article-page .article-actions,
html.dark .mo-article-page .article-navigation {
  border-color: var(--mo-n700);
  background: var(--mo-n800);
}

html.dark .mo-article-page .a-stats {
  border-top-color: var(--mo-n700);
}

html.dark .mo-article-page .comment-item {
  border-bottom-color: var(--mo-n700);
}

html.dark .mo-article-page .tc {
  background: var(--mo-n700);
  color: var(--mo-n200);
}

html.dark .mo-article-page .toc :deep(.article-toc) {
  background: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

/* 强调色文字：浅色用 p700（p300 在浅底只有 ≈1.9:1），深色回到 p300
   （默认主题=主色，Mo-Blog=#a5b4fc） */
html.dark .mo-article-page .toc :deep(.toc-item:hover),
html.dark .mo-article-page .toc :deep(.toc-item-active),
html.dark .mo-article-page .content-body :deep(code),
html.dark .mo-article-page .content-body :deep(a),
html.dark .mo-article-page .tc:hover,
html.dark .mo-article-page .related-mini:hover,
html.dark .mo-article-page .a-avatar {
  color: var(--mo-p300);
}

html.dark .mo-article-page .article-actions :deep(.el-button.is-plain) {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
  color: var(--mo-n300);
}

html.dark .mo-article-page .article-actions :deep(.el-button.is-plain:hover) {
  background: var(--mo-n700);
  color: var(--mo-n200);
}

html.dark .mo-article-page .btn-primary {
  /* 默认主题深色下 --mo-p600 被重映射成主色（#00d4ff），白字只有 1.77:1；
     --mo-on-primary 按主题给出配对前景（默认 #06272e，Mo-Blog 仍是白色）。
     浅色不动：那里 p600 = #4f46e5，白字 7:1 才是对的。 */
  color: var(--mo-on-primary);
}

html.dark .mo-article-page .article-content,
html.dark .mo-article-page .content-body,
/* li 在浅色规则里显式写了 color: var(--mo-n700)，深色下 n700 被重映射成描边色
   （#44403c，1.7:1），正文列表项几乎看不见，所以必须跟着正文一起改回文字色。
   related-mini 同理（默认主题深色下是 #3f3f46，1.6:1）。 */
html.dark .mo-article-page .related-mini,
html.dark .mo-article-page .content-body :deep(li) {
  color: var(--mo-n300);
}

.mo-article-page .mo-article-header {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--mo-sp-8) var(--mo-sp-7) var(--mo-sp-6);
  text-align: center;
}

.mo-article-page .cat-badge {
  display: inline-block;
  margin-bottom: 14px;
  padding: 4px 14px;
  border-radius: var(--mo-r-full);
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 500;
}

.mo-article-page .mo-article-header h1 {
  margin: 0 0 14px;
  color: var(--mo-n900);
  font-size: var(--mo-fs-3xl);
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0;
}

.mo-article-page .a-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--mo-sp-4);
  flex-wrap: wrap;
  color: var(--mo-n500);
  font-size: 13px;
}

.mo-article-page .a-meta .author {
  color: var(--mo-n700);
  font-weight: 500;
}

.mo-article-page .body-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 260px;
  gap: 28px;
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 var(--mo-sp-7) var(--mo-sp-8);
}

.mo-article-page .toc {
  position: sticky;
  top: 80px;
  align-self: start;
}

.mo-article-page .toc :deep(.article-toc) {
  position: static;
  width: auto;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.mo-article-page .toc :deep(.toc-header) {
  margin-bottom: var(--mo-sp-3);
  padding-bottom: var(--mo-sp-2);
  border-bottom: 1px solid var(--mo-n200);
}

.mo-article-page .toc :deep(.toc-title) {
  color: var(--mo-n700);
  font-size: 13px;
  font-weight: 600;
}

/* n700 在深色作用域被重映射成描边色（默认主题 #3f3f46 / Mo-Blog #44403c），
   拿来当文字色只有 1.6~1.7:1，「目录」标题在深色下几乎看不见。 */
html.dark .mo-article-page .toc :deep(.toc-title) {
  color: var(--mo-n100);
}

.mo-article-page .toc :deep(.toc-toggle) {
  display: none;
}

.mo-article-page .toc :deep(.toc-list) {
  padding: 0;
}

.mo-article-page .toc :deep(.toc-item) {
  padding: 5px 0 5px 14px;
  border-left: 2px solid transparent;
  color: var(--mo-n500);
  font-size: 13px;
}

.mo-article-page .toc :deep(.toc-item:hover),
.mo-article-page .toc :deep(.toc-item-active) {
  border-left-color: var(--mo-p500);
  color: var(--mo-p700);
  background: transparent;
  font-weight: 500;
}

.mo-article-page .toc :deep(.toc-dot) {
  display: none;
}

.mo-article-page .article-content {
  min-width: 0;
  max-width: 680px;
  margin: 0;
  color: var(--mo-n700);
  font-family: var(--mo-font-serif);
  font-size: 17px;
  line-height: 1.9;
}

.mo-article-page .article-cover {
  margin-bottom: 22px;
  overflow: hidden;
  border-radius: var(--mo-r-lg);
  border: 1px solid var(--mo-n200);
}

.mo-article-page .content-body {
  max-width: none;
  color: var(--mo-n700);
  font-family: var(--mo-font-serif);
  font-size: 17px;
  line-height: 1.9;
}

.mo-article-page .content-body :deep(h2) {
  margin: 32px 0 14px;
  padding-bottom: var(--mo-sp-2);
  border-bottom: 1px solid var(--mo-n200);
  color: var(--mo-n900);
  font-family: var(--mo-font-sans);
  font-size: 22px;
  font-weight: 600;
}

.mo-article-page .content-body :deep(h3) {
  margin: 22px 0 10px;
  color: var(--mo-n800);
  font-family: var(--mo-font-sans);
  font-size: var(--mo-fs-lg);
  font-weight: 600;
}

.mo-article-page .content-body :deep(p) {
  margin-bottom: 18px;
}

.mo-article-page .content-body :deep(code) {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--mo-n100);
  color: var(--mo-p700);
  font-family: var(--mo-font-mono);
  font-size: var(--mo-fs-sm);
}

.mo-article-page .content-body :deep(pre) {
  margin: var(--mo-sp-4) 0;
  padding: 18px;
  overflow-x: auto;
  border-radius: var(--mo-r-md);
  background: var(--mo-n900);
  color: var(--mo-n200);
  font-family: var(--mo-font-mono);
  font-size: 13px;
  line-height: 1.7;
}

.mo-article-page .content-body :deep(blockquote) {
  margin: 18px 0;
  padding: 14px 20px;
  border-left: 3px solid var(--mo-p400);
  border-radius: 0 var(--mo-r-md) var(--mo-r-md) 0;
  background: var(--mo-p50);
  color: var(--mo-n600);
  font-style: italic;
}

.mo-article-page .author-card,
.mo-article-page .side-widget {
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: var(--mo-n50);
}

.mo-article-page .article-side {
  min-width: 0;
}

.mo-article-page .author-card {
  padding: 22px;
  text-align: center;
}

.mo-article-page .a-avatar {
  width: 60px;
  height: 60px;
  margin: 0 auto var(--mo-sp-3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mo-p100);
  color: var(--mo-p700);
  font-size: 22px;
  font-weight: 700;
}

.mo-article-page .a-name {
  color: var(--mo-n900);
  font-size: 15px;
  font-weight: 600;
}

.mo-article-page .a-bio {
  margin: 4px 0 14px;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
  line-height: 1.6;
}

.mo-article-page .a-stats {
  display: flex;
  justify-content: center;
  gap: var(--mo-sp-5);
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--mo-n100);
}

.mo-article-page .a-stat .num {
  color: var(--mo-n800);
  font-size: var(--mo-fs-md);
  font-weight: 700;
}

.mo-article-page .a-stat .lbl {
  color: var(--mo-n500);
  font-size: 11px;
}

.mo-article-page .side-widget {
  margin-top: var(--mo-sp-4);
  padding: 18px;
}

.mo-article-page .wt {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: var(--mo-sp-3);
  color: var(--mo-n800);
  font-size: 13px;
  font-weight: 600;
}

.mo-article-page .wt::before {
  content: '';
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--mo-p500);
}

.mo-article-page .tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-2);
}

.mo-article-page .tc {
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border-radius: var(--mo-r-full);
  background: var(--mo-n100);
  color: var(--mo-n600);
  font-size: var(--mo-fs-xs);
  text-decoration: none;
}
.mo-article-page .tc:hover {
  color: var(--mo-p700);
}

.mo-article-page .related-mini-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mo-article-page .related-mini {
  color: var(--mo-n700);
  font-size: 13px;
  line-height: 1.4;
}

.mo-article-page .related-mini:hover {
  color: var(--mo-p700);
}

.mo-article-page .article-actions,
.mo-article-page .article-navigation {
  margin-top: var(--mo-sp-6);
}

.mo-article-page .article-actions {
  display: flex;
  justify-content: center;
  padding: 18px 0;
}

.mo-article-page .article-navigation {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--mo-sp-3);
}

.mo-article-page .nav-item {
  display: flex;
  flex-direction: column;
  gap: var(--mo-sp-1);
  padding: 14px;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-md);
  background: var(--mo-n50);
}

.mo-article-page .nav-label {
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.mo-article-page .nav-title {
  color: var(--mo-n800);
  font-size: 13px;
  line-height: 1.4;
}

.mo-article-page .comment-section {
  max-width: 680px;
  margin: 0 auto;
  padding: var(--mo-sp-7);
  border-top: 1px solid var(--mo-n200);
  background: var(--mo-n50);
}

.mo-article-page .comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.mo-article-page .comment-section h3 {
  margin: 0;
  font-size: var(--mo-fs-lg);
  font-weight: 600;
}

.mo-article-page .comment-sort {
  display: flex;
  gap: var(--mo-sp-1);
}

.mo-article-page .comment-sort button {
  padding: var(--mo-sp-1) var(--mo-sp-3);
  border: 1px solid transparent;
  border-radius: var(--mo-r-full);
  background: transparent;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
  cursor: pointer;
}

.mo-article-page .comment-sort button:hover {
  color: var(--mo-p700);
}

.mo-article-page .comment-sort button.active {
  background: var(--mo-p50);
  color: var(--mo-p700);
  font-weight: 500;
}

/* p700 在深色作用域不参与重映射（仍是浅色主题的深靛蓝），强调色回退到 p300 */
html.dark .mo-article-page .comment-sort button:hover,
html.dark .mo-article-page .comment-sort button.active {
  color: var(--mo-p300);
}

.mo-article-page .comment-input {
  margin-bottom: var(--mo-sp-6);
  padding: 14px;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: var(--mo-n50);
}

.mo-article-page .guest-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mo-article-page .actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--mo-sp-2);
}

.mo-article-page .md-hint {
  margin-right: auto;
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.mo-article-page .comment-item {
  display: flex;
  gap: var(--mo-sp-3);
  padding: 18px 0;
  border-bottom: 1px solid var(--mo-n100);
}

.mo-article-page .c-avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--mo-a100);
  color: var(--mo-a600);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
}

.mo-article-page .c-avatar.small {
  width: 32px;
  height: 32px;
  font-size: var(--mo-fs-xs);
}

.mo-article-page .c-body {
  min-width: 0;
  flex: 1;
}

.mo-article-page .c-head {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
}

.mo-article-page .c-name {
  color: var(--mo-n800);
  font-size: var(--mo-fs-sm);
  font-weight: 600;
}

.mo-article-page .c-time,
.mo-article-page .c-actions {
  color: var(--mo-n500);
  font-size: var(--mo-fs-xs);
}

.mo-article-page .c-text {
  margin-top: 5px;
  color: var(--mo-n600);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
}

.mo-article-page .c-actions {
  display: flex;
  gap: var(--mo-sp-4);
  margin-top: var(--mo-sp-2);
}

.mo-article-page .c-actions span {
  cursor: pointer;
}

.mo-article-page .c-actions .c-delete:hover {
  color: var(--el-color-danger);
}

.mo-article-page .c-reply {
  margin-top: var(--mo-sp-2);
  padding: 14px;
  border-radius: var(--mo-r-md);
  background: var(--mo-n50);
}

.mo-article-page .reply-item {
  display: flex;
  gap: var(--mo-sp-3);
}

.mo-article-page .reply-item + .reply-item {
  margin-top: var(--mo-sp-3);
  padding-top: var(--mo-sp-3);
  border-top: 1px solid var(--mo-n100);
}

html.dark .mo-article-page .reply-item + .reply-item {
  border-top-color: var(--mo-n700);
}

.mo-article-page .btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 18px;
  border: 1px solid transparent;
  border-radius: var(--mo-r-md);
  font-size: var(--mo-fs-sm);
  font-weight: 500;
}

.mo-article-page .btn-primary {
  background: var(--mo-p600);
  color: #fff;
}

.mo-article-page .btn-sm {
  width: 100%;
  padding: 5px 12px;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .mo-article-page .body-layout {
    grid-template-columns: minmax(0, 1fr) 260px;
  }

  .mo-article-page .toc {
    display: none;
  }
}

@media (max-width: 768px) {
  .mo-article-page .body-layout {
    grid-template-columns: 1fr;
    padding: 0 var(--mo-sp-4) var(--mo-sp-6);
  }

  .mo-article-page .article-side {
    display: none;
  }

  .mo-article-page .article-content {
    font-size: var(--mo-fs-md);
  }

  .mo-article-page .mo-article-header {
    padding: var(--mo-sp-6) var(--mo-sp-4) var(--mo-sp-4);
  }

  .mo-article-page .mo-article-header h1 {
    font-size: 22px;
  }

  .mo-article-page .comment-section {
    padding: var(--mo-sp-6) var(--mo-sp-4);
  }

  .mo-article-page .guest-fields,
  .mo-article-page .article-navigation {
    grid-template-columns: 1fr;
  }
}

.article-detail-container {
  min-height: 100vh;
  background: var(--mo-n50);
}

html.dark .article-detail-container {
  background: var(--el-bg-color);
}

html.dark.theme-mo-blog .article-detail-container {
  /* 容器继承的是浅色阶：--mo-n900 在 :root / html.theme-mo-blog 里都是 #1c1917 */
  background: var(--mo-n900);
}

.mo-article-page .article-cover img {
  display: block;
  width: 100%;
  height: auto;
}

.mo-article-page .content-body :deep(h1),
.mo-article-page .content-body :deep(h4),
.mo-article-page .content-body :deep(h5),
.mo-article-page .content-body :deep(h6) {
  margin: 28px 0 12px;
  color: var(--mo-n900);
  font-family: var(--mo-font-sans);
  font-weight: 600;
  line-height: 1.35;
}

.mo-article-page .content-body :deep(h1) {
  font-size: 26px;
}

.mo-article-page .content-body :deep(h4) {
  font-size: var(--mo-fs-md);
}

.mo-article-page .content-body :deep(h5),
.mo-article-page .content-body :deep(h6) {
  font-size: 15px;
}

.mo-article-page .content-body :deep(ul),
.mo-article-page .content-body :deep(ol) {
  margin: 0 0 18px 22px;
  padding: 0;
}

.mo-article-page .content-body :deep(li) {
  margin-bottom: var(--mo-sp-2);
  color: var(--mo-n700);
}

.mo-article-page .content-body :deep(a) {
  color: var(--mo-p700);
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 3px;
}

.mo-article-page .content-body :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 22px auto;
  border-radius: var(--mo-r-lg);
}

.mo-article-page .content-body :deep(table) {
  display: block;
  width: 100%;
  max-width: 100%;
  margin: 22px 0;
  overflow-x: auto;
  border: 1px solid var(--mo-n200);
  border-collapse: collapse;
  border-radius: var(--mo-r-md);
  font-family: var(--mo-font-sans);
  font-size: var(--mo-fs-sm);
}

.mo-article-page .content-body :deep(th),
.mo-article-page .content-body :deep(td) {
  padding: 11px 12px;
  border: 1px solid var(--mo-n200);
  text-align: left;
}

.mo-article-page .content-body :deep(th) {
  background: var(--mo-n100);
  color: var(--mo-n800);
  font-weight: 600;
}

.mo-article-page .content-body :deep(hr) {
  height: 1px;
  margin: 30px 0;
  border: 0;
  background: var(--mo-n200);
}

.loading-container {
  min-height: calc(100vh - 60px);
  padding: var(--mo-sp-12) var(--mo-sp-5);
  background: var(--mo-n50);
}

.loading-skeleton {
  max-width: 760px;
  margin: 0 auto;
}

.article-skeleton {
  width: 100%;
}

.skeleton-content {
  padding: var(--mo-sp-5) 0;
}

.not-found-container {
  min-height: calc(100vh - 60px);
  padding: var(--mo-sp-12) var(--mo-sp-5);
  background: var(--mo-n50);
  text-align: center;
}

.not-found-content {
  max-width: 420px;
  margin: 0 auto;
  padding: var(--mo-sp-7);
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-lg);
  background: var(--mo-n50);
}

.not-found-icon {
  margin-bottom: var(--mo-sp-5);
  color: var(--mo-n400);
  font-size: var(--mo-fs-7xl);
}

.not-found-content h2 {
  margin: 0 0 var(--mo-sp-3);
  color: var(--mo-n900);
  font-size: 22px;
  font-weight: 700;
}

.not-found-content p {
  margin: 0;
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  line-height: 1.7;
}

.back-home-btn {
  display: inline-block;
  margin-top: var(--mo-sp-5);
  text-decoration: none;
}

/* ---------- 企业官网模板模式：阅读进度条 + 模板皮肤微调 ----------
   颜色引用 --tpl-* 变量层（themes/site-templates.scss），行业模板切换时同步换装 */
.read-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--tpl-p);
  z-index: 200;
  transition: width 0.1s linear;
}

.article-detail-container.tpl-site .mo-article-header h1 {
  font-family: var(--tpl-serif);
  color: var(--tpl-ink-900);
}

.article-detail-container.tpl-site .cat-badge {
  background: var(--tpl-p-sub);
  color: var(--tpl-p-text);
}

.article-detail-container.tpl-site .a-meta {
  color: var(--tpl-ink-400);
}

.article-detail-container.tpl-site .article-content :deep(a),
.article-detail-container.tpl-site .content-body :deep(a) {
  color: var(--tpl-p-text);
}
</style>
