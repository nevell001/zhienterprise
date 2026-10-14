<template>
  <div class="category-overview-page">
    <BlogLayout>
      <div class="category-overview-container">
        <header class="ov-header">
          <h1>文章分类</h1>
          <p>浏览不同主题的文章，找到感兴趣的内容</p>
        </header>

        <div v-if="loading" v-loading="loading" class="ov-loading"></div>

        <template v-else-if="categories.length > 0">
          <div class="cat-grid">
            <router-link
              v-for="category in categories"
              :key="category.id"
              :to="`/category/${category.alias || category.id}`"
              class="cat-card"
            >
              <div class="cat-card-head">
                <span class="cat-icon">📂</span>
                <h3 class="cat-name">{{ category.name }}</h3>
              </div>
              <p v-if="category.description" class="cat-desc">{{ category.description }}</p>
              <div class="cat-meta">
                <span>{{ category.articleCount || category.count || 0 }} 篇文章</span>
              </div>
            </router-link>
          </div>
        </template>

        <el-empty v-else description="暂无分类">
          <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </el-empty>
      </div>
    </BlogLayout>
  </div>
</template>

<script setup lang="ts" name="BlogCategoryOverview">
import { onMounted, ref } from 'vue'
import BlogLayout from '@/components/BlogLayout.vue'
import { getCategoryList } from '@/api/blog/category'

interface CategoryItem {
  // 与共享类型 @/types 的 Category 对齐（接口返回的 id 允许为空）
  id?: number
  name: string
  alias?: string
  description?: string
  articleCount?: number
  count?: number
}

const loading = ref(false)
const categories = ref<CategoryItem[]>([])

const loadCategories = async () => {
  loading.value = true
  try {
    const response = await getCategoryList({ pageSize: 100 })
    // 后端 /blog/category/list 不分页，分类数组放在 data 里（不是 rows），
    // 这里同时兼容 data / rows / 裸数组三种形态，避免总览页误判为空。
    const list = Array.isArray(response)
      ? response
      : Array.isArray(response?.data)
        ? response.data
        : response?.rows || []
    categories.value = Array.isArray(list) ? list : []
  } catch {
    categories.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.category-overview-page {
  min-height: 100vh;
}
.category-overview-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: var(--mo-sp-12) var(--mo-sp-5) var(--mo-sp-9);
}
.ov-header {
  margin-bottom: var(--mo-sp-6);
}
.ov-header h1 {
  margin: 0 0 var(--mo-sp-2);
  font-size: 26px;
  font-weight: 700;
  color: var(--el-text-color-primary, #303133);
}
.ov-header p {
  margin: 0;
  color: var(--el-text-color-secondary, #606266);
  font-size: var(--mo-fs-sm);
}
.ov-loading {
  min-height: 160px;
}
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--mo-sp-4);
}
.cat-card {
  display: block;
  padding: 18px;
  background: var(--el-bg-color-overlay, #fff);
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  text-decoration: none;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.cat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}
.cat-card-head {
  display: flex;
  align-items: center;
  gap: var(--mo-sp-2);
}
.cat-icon {
  font-size: var(--mo-fs-xl);
}
.cat-name {
  margin: 0;
  font-size: 17px;
  color: var(--el-text-color-primary, #303133);
}
.cat-desc {
  margin: 10px 0 12px;
  min-height: 20px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-regular, #606266);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.cat-meta {
  font-size: var(--mo-fs-xs);
  color: var(--mo-primary-text, #409eff);
}
</style>
