<template>
  <div class="article-card" :style="{ animationDelay: `${(index ?? 0) * 0.1}s` }">
    <div v-if="article.coverUrl" class="article-card-cover">
      <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
      <div v-if="article.categoryName" class="article-card-badge">
        {{ article.categoryName }}
      </div>
    </div>
    <div class="article-card-content">
      <div
        v-if="Number(article.isTop) === 1 || Number(article.isRecommend) === 1"
        class="article-card-flags"
      >
        <span v-if="Number(article.isTop) === 1" class="card-flag flag-top">置顶</span>
        <span v-if="Number(article.isRecommend) === 1" class="card-flag flag-rec">推荐</span>
      </div>
      <h2 class="article-card-title">
        <router-link :to="`/article/${article.id}`" :title="article.title">
          {{ article.title }}
        </router-link>
      </h2>
      <div class="article-card-meta">
        <span class="article-card-meta-item">
          <el-icon><Calendar /></el-icon>
          {{ formatDate(article.createTime) }}
        </span>
        <span class="article-card-meta-item">
          <el-icon><View /></el-icon>
          {{ article.viewCount || 0 }} 阅读
        </span>
        <span v-if="article.likeCount" class="article-card-meta-item">
          <el-icon><Star /></el-icon>
          {{ article.likeCount }} 点赞
        </span>
        <span v-if="article.commentCount" class="article-card-meta-item">
          <el-icon><ChatDotRound /></el-icon>
          {{ article.commentCount }} 评论
        </span>
      </div>
      <p class="article-card-summary">
        {{ article.summary || stripHtmlTags(article.content).substring(0, 150) + '...' }}
      </p>
      <div v-if="article.tags && article.tags.length" class="article-card-tags">
        <span v-for="tag in article.tags.slice(0, 3)" :key="tag.id" class="article-card-tag">
          {{ tag.name }}
        </span>
      </div>
      <div class="article-card-footer">
        <router-link :to="`/article/${article.id}`" class="article-card-more">
          阅读全文
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight, Calendar, ChatDotRound, Star, View } from '@element-plus/icons-vue'

interface ArticleCardTag {
  id: number | string
  name: string
}

export interface ArticleCardItem {
  id: number | string
  title: string
  coverUrl?: string
  categoryName?: string
  createTime?: string
  isTop?: number
  isRecommend?: number
  viewCount?: number
  likeCount?: number
  commentCount?: number
  summary?: string
  content?: string
  tags?: ArticleCardTag[]
}

withDefaults(defineProps<{ article: ArticleCardItem; index?: number }>(), { index: 0 })

const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const stripHtmlTags = (html?: string) => (html ? html.replace(/<[^>]*>/g, '') : '')
</script>

<style lang="scss" scoped>
.article-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: var(--mo-r-md);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: articleCardIn 0.6s ease forwards;
}

.article-card:hover {
  border-color: rgba(79, 70, 229, 0.1);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
}

.article-card-cover {
  position: relative;
  /* 与封面上传裁剪比例（3:1）一致：裁什么显示什么 */
  aspect-ratio: 3 / 1;
  overflow: hidden;
  background: linear-gradient(45deg, var(--mo-n100), var(--mo-n200));
}

.article-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-card-cover:hover img {
  transform: scale(1.08);
}

.article-card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 10px;
  color: var(--mo-p700);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
  background: rgba(238, 242, 255, 0.94);
  border: 1px solid var(--mo-p200);
  border-radius: var(--mo-r-sm);
  box-shadow: 0 2px 8px rgba(28, 25, 23, 0.08);
  backdrop-filter: blur(10px);
}

.article-card-content {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 25px;
}

/* 置顶/推荐标识（颜色用 EP 令牌，深色模式自动跟随） */
.article-card-flags {
  display: flex;
  gap: var(--mo-sp-1);
  margin-bottom: var(--mo-sp-2);
}

.card-flag {
  padding: var(--mo-sp-1) var(--mo-sp-2);
  border-radius: var(--mo-r-full);
  font-size: var(--mo-fs-xs);
  font-weight: 600;
  line-height: 1.6;
}

.card-flag.flag-top {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  border: 1px solid var(--el-color-danger-light-5);
}

.card-flag.flag-rec {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-5);
}

/* 亮色模式：EP 的 warning/danger 基色 + light-9 浅底只有 2.0~2.6:1。
   改用 dark-4 色阶（≈5.1/6.2:1）；深色模式下 EP 会把 dark-N 反向提亮，同样安全。 */
html:not(.dark) .card-flag.flag-top {
  color: var(--el-color-danger-dark-4);
}

html:not(.dark) .card-flag.flag-rec {
  color: var(--el-color-warning-dark-4);
}

.article-card-title {
  margin: 0 0 15px 0;
  font-size: var(--mo-fs-2xl);
  font-weight: 600;
  line-height: 1.4;
}

.article-card-title a {
  display: block;
  overflow: hidden;
  color: var(--mo-n900);
  text-overflow: ellipsis;
  white-space: nowrap;
  background: linear-gradient(135deg, var(--mo-n900), var(--mo-n800));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-decoration: none;
  transition: all 0.3s ease;
}

.article-card-title a:hover {
  color: var(--mo-p600);
  transform: translateX(4px);
}

.article-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-5);
  margin-bottom: 15px;
  color: var(--mo-n600);
  font-size: 0.9rem;
}

.article-card-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: var(--mo-sp-1) var(--mo-sp-2);
  background: rgba(102, 102, 102, 0.05);
  border-radius: var(--mo-r-sm);
  transition: background-color 0.3s ease;
}

.article-card-meta-item:hover {
  color: var(--mo-p600);
  background: rgba(79, 70, 229, 0.1);
}

.article-card-meta-item .el-icon {
  font-size: var(--mo-fs-md);
  opacity: 0.8;
}

.article-card-summary {
  display: -webkit-box;
  flex: 1;
  margin-bottom: 18px;
  overflow: hidden;
  color: var(--mo-n600);
  font-size: 0.95rem;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.article-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--mo-sp-2);
  margin-top: auto;
  margin-bottom: 18px;
}

.article-card-tag {
  position: relative;
  overflow: hidden;
  padding: 6px 16px;
  color: var(--mo-p700);
  font-size: 0.85rem;
  font-weight: 600;
  background: var(--mo-p50);
  border-radius: var(--mo-r-md);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-card-tag::before {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
  content: '';
}

.article-card-tag:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.article-card-tag:hover::before {
  left: 100%;
}

.article-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 15px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.article-card-more {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--mo-p600);
  font-size: 0.9rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s ease;
}

.article-card-more::after {
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--mo-p600), var(--mo-p800));
  transition: width 0.3s ease;
  content: '';
}

.article-card-more:hover {
  color: var(--mo-p800);
  transform: translateX(4px);
}

.article-card-more:hover::after {
  width: 100%;
}

.article-card-more .el-icon {
  transition: transform 0.3s ease;
}

.article-card-more:hover .el-icon {
  transform: translateX(3px);
}

@media (max-width: 768px) {
  .article-card {
    margin-bottom: var(--mo-sp-5);
  }

  .article-card-content {
    padding: var(--mo-sp-5);
  }

  .article-card-title {
    font-size: 1.4rem;
  }

  .article-card-meta {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 15px;
    font-size: 0.85rem;
  }

  .article-card-summary {
    font-size: 0.9rem;
    line-height: 1.5;
  }

  .article-card-tags {
    gap: 6px;
  }

  .article-card-tag {
    padding: 3px 8px;
    font-size: var(--mo-fs-xs);
  }

  .article-card-footer {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

@media (max-width: 480px) {
  .article-card {
    border-radius: var(--mo-r-sm);
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.08);
  }

  .article-card-content {
    padding: 18px;
  }

  .article-card-title {
    margin-bottom: var(--mo-sp-3);
    font-size: var(--mo-fs-xl);
  }

  .article-card-summary {
    margin-bottom: var(--mo-sp-3);
    font-size: 0.9rem;
    -webkit-line-clamp: 2;
  }
}

html.dark .article-card {
  background: var(--mo-n800);
  border-color: var(--mo-n800);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

html.dark .article-card:hover {
  border-color: rgba(79, 70, 229, 0.2);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

html.dark .article-card-badge {
  color: var(--mo-p300);
  background: rgba(79, 70, 229, 0.2);
  border-color: var(--mo-p600);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

html.dark .article-card-title a {
  background: linear-gradient(135deg, var(--mo-n100), var(--mo-n200));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

html.dark .article-card-title a:hover {
  color: var(--mo-p300);
}

html.dark .article-card-meta {
  color: var(--mo-n400);
}

html.dark .article-card-meta-item {
  color: var(--mo-n400);
  background: rgba(255, 255, 255, 0.05);
}

html.dark .article-card-meta-item:hover {
  color: var(--mo-p300);
  background: rgba(79, 70, 229, 0.15);
}

html.dark .article-card-summary {
  color: var(--mo-n400);
}

html.dark .article-card-footer {
  border-top-color: var(--mo-n800);
}

html.dark .article-card-more {
  color: var(--mo-p300);
}

html.dark .article-card-more:hover {
  color: var(--mo-p300);
}

/* p600/p700 不参与深色重映射，深色下强调色文字统一改用 p300 */
html.dark .article-card-tag {
  color: var(--mo-p300);
}

@keyframes articleCardIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
