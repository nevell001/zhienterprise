<template>
  <!-- 新闻/内容详情：企业官网风格（博客元素已移除） -->
  <div class="article-detail-container tpl-site">
    <!-- 站点导航 -->
    <SiteLayout>
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

      <div v-else class="mo-article-page news-article">
        <header class="na-head">
          <!-- 面包屑（方案文档 §3.4：面包屑 → 分类 → 标题） -->
          <SiteBreadcrumb class="article-crumb" :items="breadcrumbItems" />
          <div class="na-kicker">
            <span v-if="article.categoryName" class="cat-badge">{{ article.categoryName }}</span>
            <time :datetime="article.createTime">{{ formatDate(article.createTime) }}</time>
            <span v-if="article.updateTime && article.updateTime !== article.createTime">
              更新于 {{ formatDate(article.updateTime) }}
            </span>
            <span v-if="isFeatureEnabled('view_count_enabled')">
              {{ article.viewCount || 0 }} 阅读
            </span>
          </div>
          <h1>{{ article.title }}</h1>
          <p v-if="article.summary" class="na-lead">{{ article.summary }}</p>
        </header>

        <div class="na-body">
          <figure v-if="article.coverUrl" class="article-cover">
            <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
          </figure>
          <div class="content-body" v-html="processedContent"></div>

          <div v-if="article.tags && article.tags.length" class="na-tags">
            <span class="na-tags-label">关键词</span>
            <router-link
              v-for="tag in article.tags"
              :key="tag.id"
              :to="`/tag/${tag.id}`"
              class="na-tag"
            >
              {{ tag.name }}
            </router-link>
          </div>
        </div>

        <!-- 相关阅读：企业站用卡片而不是博客侧栏迷你列表 -->
        <section v-if="relatedArticles.length > 0" class="na-related">
          <div class="na-sec-head">
            <h2>相关阅读</h2>
            <p>继续了解行业实践与我们的解决思路</p>
          </div>
          <div class="na-related-grid">
            <router-link
              v-for="related in relatedArticles.slice(0, 3)"
              :key="related.id"
              class="na-related-card"
              :to="{
                name: 'PublicSiteArticleDetail',
                params: { id: related.id ?? related.articleId ?? related.uuid }
              }"
            >
              <span v-if="related.categoryName" class="na-related-cat">
                {{ related.categoryName }}
              </span>
              <h3>{{ related.title }}</h3>
              <p v-if="related.summary">{{ related.summary }}</p>
              <span class="na-related-more">阅读全文 →</span>
            </router-link>
          </div>
        </section>

        <!-- 转化入口：文章读完给出下一步动作 -->
        <section class="na-cta">
          <div class="na-cta-text">
            <h2>需要进一步了解？</h2>
            <p>告诉我们你的业务场景，我们会给出可落地的建议。</p>
          </div>
          <router-link class="na-cta-btn" to="/contact">预约咨询</router-link>
        </section>

        <!-- 上/下一篇：保留新闻式顺序浏览 -->
        <nav
          v-if="prevArticle || nextArticle"
          class="na-prevnext"
          :class="{ 'is-single': !prevArticle || !nextArticle }"
        >
          <router-link
            v-if="prevArticle"
            class="na-prevnext-item"
            :to="{
              name: 'PublicSiteArticleDetail',
              params: { id: prevArticle.id ?? prevArticle.articleId ?? prevArticle.uuid }
            }"
          >
            <span class="na-prevnext-label">上一篇</span>
            <span class="na-prevnext-title">{{ prevArticle.title }}</span>
          </router-link>
          <span v-else class="na-prevnext-item is-empty"></span>
          <router-link
            v-if="nextArticle"
            class="na-prevnext-item is-next"
            :to="{
              name: 'PublicSiteArticleDetail',
              params: { id: nextArticle.id ?? nextArticle.articleId ?? nextArticle.uuid }
            }"
          >
            <span class="na-prevnext-label">下一篇</span>
            <span class="na-prevnext-title">{{ nextArticle.title }}</span>
          </router-link>
        </nav>

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

          <SitePager
            :total="totalComments"
            :page-size="COMMENT_PAGE_SIZE"
            :page-num="commentPage"
            @page-change="handleCommentPageChange"
          />
        </section>
      </div>

      <!-- 站点底部 -->
    </SiteLayout>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import SiteLayout from '@/components/SiteLayout.vue'
import SiteBreadcrumb from '@/components/SiteBreadcrumb.vue'
import SitePager from '@/components/SitePager.vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import { getArticleDetail, getRelatedArticles } from '@/api/site/article'
import { toggleCommentLike, getCommentLikedStatuses } from '@/api/site/like'

import {
  getArticleComments,
  addSiteComment as apiSubmitComment,
  updateMyComment,
  deleteMyComment
} from '@/api/site/comment'
import { getSiteSettings, getSiteSettingsAnonymous } from '@/api/site/setting'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { createRequestGuard } from '@/utils/requestGuard'
import { applySeo, canonicalUrl } from '@/utils/seo'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'

// JSON-LD 结构化数据随路由卸载移除，避免残留到下一页
onUnmounted(() => {
  removeJsonLd('zh-jsonld-article')
})

import { logger } from '@/utils/logger'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteSettingsStore = useSiteSettingsStore()

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
const commentSubmitting = ref(false)
const isLoggedIn = ref(false)
const siteSettings = computed(() => siteSettingsStore.siteSettings)
const isFeatureEnabled = (feature: string) => siteSettingsStore.isFeatureEnabled(feature)

// 处理文章内容，为标题添加ID并消毒内容
const processedContent = computed(() => {
  if (!article.value || !article.value.content) return ''

  // 首先消毒HTML内容，防止XSS攻击
  const sanitizedContent = sanitizeArticleContent(article.value.content)

  // 然后为标题添加 ID（保留锚点能力，便于外链直达小节）
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

// 面包屑链路（≤3 级）：首页 → 分类 → 当前文章；分类优先数字 ID（路由兼容别名）
const breadcrumbItems = computed(() => {
  const items: Array<{ label: string; to?: string }> = [{ label: '首页', to: '/' }]
  if (article.value?.categoryName) {
    items.push({
      label: article.value.categoryName,
      to: article.value.categoryId ? `/category/${article.value.categoryId}` : '/news'
    })
  }
  items.push({ label: article.value?.title || '文章' })
  return items
})

// 评论总数（含回复的已发布数）：以文章详情/评论增删改返回的 commentCount 为准
const articleCommentCount = computed(() => {
  const count = Number(article.value?.commentCount)
  return Number.isFinite(count) ? count : totalComments.value
})

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

      // 应用文章级 SEO（TDK 优先用文章级字段，回退标题/摘要）
      const siteName = (siteSettings.value as any)?.blog_name || '知企建站系统'
      applySeo({
        title: article.value.seoTitle || `${article.value.title} - ${siteName}`,
        description: article.value.seoDescription || article.value.summary || '',
        keywords: article.value.seoKeywords || '',
        canonical: canonicalUrl((siteSettings.value as any)?.seo_canonical_url)
      })
      // JSON-LD 结构化数据（方案文档 §6.2「新闻详情」：NewsArticle/BlogPosting，
      // 关键字段 标题、发布日期、作者、图片、publisher；案例类内容按 Article 输出）
      setJsonLd('zh-jsonld-article', {
        '@context': 'https://schema.org',
        '@type': article.value.articleType === 'case' ? 'Article' : 'NewsArticle',
        headline: article.value.title,
        description: article.value.seoDescription || article.value.summary || undefined,
        image: article.value.coverUrl || undefined,
        datePublished: article.value.createTime || undefined,
        dateModified: article.value.updateTime || undefined,
        // 企业官网不再暴露个人作者：署名统一用站点机构
        author: { '@type': 'Organization', name: siteName },
        publisher: {
          '@type': 'Organization',
          name: siteName,
          logo: (() => {
            const logo = (siteSettings.value as any).site_logo || siteSettings.value.blog_avatar
            return logo ? { '@type': 'ImageObject', url: logo } : undefined
          })()
        }
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
const loadSiteSettings = async () => {
  try {
    let response
    try {
      response = await getSiteSettings()
    } catch (error: any) {
      logger.warn('标准站点设置接口访问失败，尝试匿名接口:', error)
      response = await getSiteSettingsAnonymous()
    }

    let settings = {}
    if (response && response.code === 200) {
      settings = response.data || {}
    } else if (response && typeof response === 'object') {
      settings = response
    }

    // 更新 siteSettingsStore
    siteSettingsStore.updateSiteSettings(settings)
  } catch (error: any) {
    logger.error('获取站点设置失败:', error)
    // 使用默认值
  }
}

// 组件挂载时加载数据（评论在 loadArticleDetail 内随文章一起拉取）
onMounted(() => {
  loadArticleDetail()
  loadSiteSettings()
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
/* ==================== 新闻 / 内容详情（企业官网风格） ====================
   已移除的博客元素：阅读进度条、文章目录（TOC）、作者卡（头像/昵称/简介/关注/
   获赞/评论数）、收藏按钮、阅读时长、标签云侧栏、侧栏迷你相关推荐。
   新结构对齐企业官网：面包屑 → 分类与日期 → 标题 → 摘要 → 封面 → 正文 →
   关键词 → 相关阅读卡片 → 咨询 CTA → 上/下一篇 →（评论，开关默认关闭）。
   配色统一走 --tpl-* 变量层；评论区保留 --mo-* 色阶（下方保留其深色重映射）。 */

.mo-article-page {
  min-height: 100vh;
  padding-top: 60px;
  background: var(--tpl-bg, #fff);
  color: var(--tpl-ink-700, #1f2937);
  font-family: var(--mo-font-sans);
}

/* 企业模式：SiteLayout 的 .tpl-nav-offset 已为 fixed 导航留 64px，页面不重复让位 */
:global(.site-layout.tpl-nav-offset) .mo-article-page {
  padding-top: 0;
}

/* ---------- 内容列（收敛到 860px，利于扫读） ---------- */
.na-head,
.na-body,
.na-related,
.na-cta,
.na-prevnext {
  box-sizing: border-box;
  max-width: 860px;
  margin: 0 auto;
  padding-right: var(--mo-sp-6, 24px);
  padding-left: var(--mo-sp-6, 24px);
}

.na-head {
  padding-top: var(--mo-sp-9, 48px);
}

.article-crumb {
  margin-bottom: var(--mo-sp-4, 16px);
}

.na-kicker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: var(--mo-sp-3, 12px);
  color: var(--tpl-ink-400, #6b7280);
  font-size: var(--mo-fs-sm, 14px);
}

.cat-badge {
  display: inline-block;
  padding: 3px 10px;
  color: var(--tpl-p-text, #1a56db);
  font-size: var(--mo-fs-xs, 12px);
  font-weight: 600;
  background: var(--tpl-p-sub, #eef3fe);
  border-radius: 999px;
}

.na-head h1 {
  margin: 0 0 var(--mo-sp-4, 16px);
  color: var(--tpl-ink-900, #0b0f19);
  font-size: clamp(26px, 3.2vw, var(--mo-fs-4xl, 32px));
  font-weight: 700;
  line-height: 1.32;
  letter-spacing: -0.01em;
}

.na-lead {
  margin: 0 0 var(--mo-sp-5, 20px);
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-lg, 18px);
  line-height: 1.75;
}

.na-body {
  padding-bottom: var(--mo-sp-8, 40px);
}

.article-cover {
  margin: var(--mo-sp-5, 20px) 0 var(--mo-sp-7, 32px);
  overflow: hidden;
  border-radius: var(--tpl-r-lg, 16px);
}

.article-cover img {
  display: block;
  width: 100%;
  height: auto;
}

/* ---------- 正文排版 ---------- */
.mo-article-page .content-body {
  min-width: 0;
  max-width: none;
  color: var(--tpl-ink-700, #1f2937);
  font-size: var(--mo-fs-md, 16px);
  line-height: 1.9;
}

.mo-article-page .content-body :deep(h1) {
  margin: var(--mo-sp-7, 32px) 0 var(--mo-sp-3, 12px);
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-3xl, 28px);
  font-weight: 700;
}

.mo-article-page .content-body :deep(h2) {
  margin: var(--mo-sp-7, 32px) 0 var(--mo-sp-3, 12px);
  padding-bottom: var(--mo-sp-2, 8px);
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-2xl, 24px);
  font-weight: 700;
  border-bottom: 1px solid var(--tpl-border, #e5e7eb);
}

.mo-article-page .content-body :deep(h3) {
  margin: var(--mo-sp-6, 24px) 0 var(--mo-sp-2, 8px);
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-lg, 18px);
  font-weight: 600;
}

.mo-article-page .content-body :deep(p) {
  margin-bottom: var(--mo-sp-4, 16px);
}

.mo-article-page .content-body :deep(ul),
.mo-article-page .content-body :deep(ol) {
  margin: 0 0 var(--mo-sp-4, 16px) 22px;
  padding: 0;
}

.mo-article-page .content-body :deep(li) {
  margin-bottom: var(--mo-sp-2, 8px);
  color: var(--tpl-ink-700, #1f2937);
}

.mo-article-page .content-body :deep(a) {
  color: var(--tpl-p-text, #1a56db);
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-underline-offset: 3px;
}

.mo-article-page .content-body :deep(strong) {
  color: var(--tpl-ink-900, #0b0f19);
  font-weight: 600;
}

.mo-article-page .content-body :deep(code) {
  padding: 2px 6px;
  color: var(--tpl-p-text, #1a56db);
  font-family: var(--mo-font-mono);
  font-size: var(--mo-fs-sm, 14px);
  background: var(--tpl-surface, #f7f8fa);
  border-radius: 4px;
}

.mo-article-page .content-body :deep(pre) {
  padding: var(--mo-sp-4, 16px);
  margin: var(--mo-sp-4, 16px) 0;
  overflow-x: auto;
  color: #e5e7eb;
  font-family: var(--mo-font-mono);
  font-size: 13px;
  line-height: 1.7;
  background: #0b0f19;
  border-radius: var(--tpl-r-md, 10px);
}

.mo-article-page .content-body :deep(blockquote) {
  padding: var(--mo-sp-4, 16px) var(--mo-sp-5, 20px);
  margin: var(--mo-sp-4, 16px) 0;
  color: var(--tpl-ink-500, #4b5563);
  font-style: italic;
  background: var(--tpl-p-sub, #eef3fe);
  border-left: 3px solid var(--tpl-p, #1a56db);
  border-radius: 0 var(--tpl-r-md, 10px) var(--tpl-r-md, 10px) 0;
}

.mo-article-page .content-body :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: var(--mo-sp-5, 20px) auto;
  border-radius: var(--tpl-r-lg, 16px);
}

.mo-article-page .content-body :deep(table) {
  display: block;
  width: 100%;
  max-width: 100%;
  margin: var(--mo-sp-5, 20px) 0;
  overflow-x: auto;
  font-family: var(--mo-font-sans);
  font-size: var(--mo-fs-sm, 14px);
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-collapse: collapse;
  border-radius: var(--tpl-r-md, 10px);
}

.mo-article-page .content-body :deep(th),
.mo-article-page .content-body :deep(td) {
  padding: 11px 12px;
  text-align: left;
  border: 1px solid var(--tpl-border, #e5e7eb);
}

.mo-article-page .content-body :deep(th) {
  font-weight: 600;
  color: var(--tpl-ink-900, #0b0f19);
  background: var(--tpl-surface, #f7f8fa);
}

.mo-article-page .content-body :deep(hr) {
  height: 1px;
  margin: var(--mo-sp-7, 32px) 0;
  background: var(--tpl-border, #e5e7eb);
  border: 0;
}

/* ---------- 关键词 ---------- */
.na-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding-top: var(--mo-sp-5, 20px);
  margin-top: var(--mo-sp-5, 20px);
  border-top: 1px solid var(--tpl-border, #e5e7eb);
}

.na-tags-label {
  margin-right: 4px;
  color: var(--tpl-ink-400, #6b7280);
  font-size: var(--mo-fs-xs, 12px);
}

.na-tag {
  padding: 4px 12px;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-xs, 12px);
  text-decoration: none;
  background: var(--tpl-surface, #f7f8fa);
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-radius: 999px;
  transition:
    color 0.2s,
    border-color 0.2s;
}

.na-tag:hover {
  color: var(--tpl-p-text, #1a56db);
  border-color: var(--tpl-p, #1a56db);
}

/* ---------- 相关阅读 ---------- */
.na-related {
  padding-top: var(--mo-sp-8, 40px);
  padding-bottom: var(--mo-sp-8, 40px);
  border-top: 1px solid var(--tpl-border, #e5e7eb);
  background: var(--tpl-surface, #f7f8fa);
  max-width: none;
}

.na-sec-head {
  max-width: 860px;
  padding: 0 var(--mo-sp-6, 24px);
  margin: 0 auto var(--mo-sp-5, 20px);
}

.na-sec-head h2 {
  margin: 0 0 var(--mo-sp-2, 8px);
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-2xl, 24px);
  font-weight: 700;
}

.na-sec-head p {
  margin: 0;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-sm, 14px);
}

.na-related-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-4, 16px);
  max-width: 860px;
  padding: 0 var(--mo-sp-6, 24px);
  margin: 0 auto;
}

.na-related-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: var(--mo-sp-5, 20px);
  text-decoration: none;
  background: var(--tpl-bg, #fff);
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-radius: var(--tpl-r-lg, 16px);
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.na-related-card:hover {
  border-color: var(--tpl-p, #1a56db);
  box-shadow: var(--tpl-sh-md, 0 8px 24px rgba(11, 15, 25, 0.1));
  transform: translateY(-2px);
}

.na-related-cat {
  align-self: flex-start;
  padding: 2px 8px;
  color: var(--tpl-p-text, #1a56db);
  font-size: var(--mo-fs-xs, 12px);
  background: var(--tpl-p-sub, #eef3fe);
  border-radius: 999px;
}

.na-related-card h3 {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-md, 16px);
  font-weight: 600;
  line-height: 1.5;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.na-related-card p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-sm, 14px);
  line-height: 1.6;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.na-related-more {
  margin-top: auto;
  color: var(--tpl-p-text, #1a56db);
  font-size: var(--mo-fs-xs, 12px);
  font-weight: 600;
}

/* ---------- 咨询 CTA ---------- */
.na-cta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-4, 16px);
  align-items: center;
  justify-content: space-between;
  padding-top: var(--mo-sp-8, 40px);
  padding-bottom: var(--mo-sp-8, 40px);
}

.na-cta h2 {
  margin: 0 0 var(--mo-sp-1, 4px);
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-xl, 20px);
  font-weight: 700;
}

.na-cta p {
  margin: 0;
  color: var(--tpl-ink-500, #4b5563);
  font-size: var(--mo-fs-sm, 14px);
}

.na-cta-btn {
  padding: 12px 26px;
  color: var(--tpl-white, #fff);
  font-size: var(--mo-fs-sm, 14px);
  font-weight: 600;
  text-decoration: none;
  background: var(--tpl-p, #1a56db);
  border-radius: var(--tpl-r-md, 10px);
  transition: background 0.2s;
}

.na-cta-btn:hover {
  background: var(--tpl-p-h, #1544b0);
}

/* ---------- 上/下一篇 ---------- */
.na-prevnext {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mo-sp-4, 16px);
  padding-top: var(--mo-sp-5, 20px);
  padding-bottom: var(--mo-sp-9, 48px);
  border-top: 1px solid var(--tpl-border, #e5e7eb);
}

.na-prevnext-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: var(--mo-sp-4, 16px);
  text-decoration: none;
  background: var(--tpl-surface, #f7f8fa);
  border: 1px solid var(--tpl-border, #e5e7eb);
  border-radius: var(--tpl-r-md, 10px);
  transition: border-color 0.2s;
}

.na-prevnext.is-single {
  grid-template-columns: 1fr;
}

.na-prevnext-item.is-next {
  text-align: right;
}

.na-prevnext-item.is-empty {
  visibility: hidden;
}

.na-prevnext-item:hover {
  border-color: var(--tpl-p, #1a56db);
}

.na-prevnext-label {
  color: var(--tpl-ink-400, #6b7280);
  font-size: var(--mo-fs-xs, 12px);
}

.na-prevnext-title {
  overflow: hidden;
  color: var(--tpl-ink-900, #0b0f19);
  font-size: var(--mo-fs-sm, 14px);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 评论区色阶：评论用 --mo-* 变量，保留其深色重映射 ===== */
/* ===== 深色模式：评论区色阶 =====
   默认主题深色映射到 --el-* 变量（Tech Blue 深蓝），Mo-Blog 深色恢复棕色色阶。
   所有引用 --mo-n* 的亮色/深色覆盖规则自动适配。 */
html.dark .mo-article-page .comment-section {
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
}

/* Mo-Blog 主题深色：恢复棕色色阶（仅评论区） */
html.dark.theme-mo-blog .mo-article-page .comment-section {
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
}

/* ===== 评论区（开关默认关闭，comment_enabled） ===== */
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

/* ===== 加载态 / 404 ===== */
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

html.dark .loading-container {
  background: var(--el-bg-color);
}

html.dark .not-found-content h2 {
  color: var(--el-text-color-primary);
}

html.dark .not-found-content p {
  color: var(--el-text-color-regular);
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .na-related-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .na-head,
  .na-body,
  .na-cta,
  .na-prevnext {
    padding-right: var(--mo-sp-4, 16px);
    padding-left: var(--mo-sp-4, 16px);
  }

  .na-head {
    padding-top: var(--mo-sp-6, 24px);
  }

  .na-head h1 {
    font-size: var(--mo-fs-2xl, 24px);
  }

  .na-lead {
    font-size: var(--mo-fs-md, 16px);
  }

  .na-related-grid {
    grid-template-columns: 1fr;
    padding-right: var(--mo-sp-4, 16px);
    padding-left: var(--mo-sp-4, 16px);
  }

  .na-sec-head {
    padding-right: var(--mo-sp-4, 16px);
    padding-left: var(--mo-sp-4, 16px);
  }

  .na-prevnext {
    grid-template-columns: 1fr;
  }

  .na-prevnext-item.is-empty {
    display: none;
  }

  .na-prevnext-item.is-next {
    text-align: left;
  }
}
</style>
