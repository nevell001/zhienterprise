<template>
  <div class="tag-category-selector">
    <!-- 分类选择 -->
    <el-form-item v-if="showCategory" label="分类" prop="categoryId">
      <el-select
        v-model="localSelectedCategory"
        placeholder="请选择分类"
        clearable
        filterable
        style="width: 100%"
        @change="handleCategoryChange"
      >
        <el-option
          v-for="category in categoryOptions"
          :key="category.id"
          :label="category.name"
          :value="category.id"
        >
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>{{ category.name }}</span>
            <el-tag v-if="category.articleCount" size="small" type="info">
              {{ category.articleCount }}篇
            </el-tag>
          </div>
        </el-option>
      </el-select>
    </el-form-item>

    <!-- 标签选择 -->
    <el-form-item v-if="showTags" label="标签" prop="tagIds">
      <div class="tag-selector-container">
        <!-- 标签选择器 -->
        <el-select
          v-model="localSelectedTags"
          multiple
          :placeholder="placeholder"
          clearable
          filterable
          collapse-tags
          collapse-tags-tooltip
          style="width: 100%; margin-bottom: 10px"
          @change="handleTagChange"
        >
          <el-option
            v-for="tag in tagOptions"
            :key="tag.id || tag.tagId"
            :label="tag.name || tag.tagName"
            :value="tag.id || tag.tagId"
          >
            <div style="display: flex; align-items: center">
              <el-tag
                :style="{
                  backgroundColor: tag.color || 'var(--el-color-primary)',
                  color: 'white',
                  border: 'none'
                }"
                size="small"
              >
                <i v-if="tag.icon" :class="tag.icon" style="margin-right: 4px"></i>
                {{ tag.name || tag.tagName }}
              </el-tag>
            </div>
          </el-option>
        </el-select>

        <!-- 已选标签展示 -->
        <div v-if="selectedTagsDisplay.length > 0" class="selected-tags">
          <div class="selected-tags-label">已选标签：</div>
          <div class="selected-tags-list">
            <el-tag
              v-for="tag in selectedTagsDisplay"
              :key="tag.id || tag.tagId"
              :style="{
                backgroundColor: tag.color || 'var(--el-color-primary)',
                color: 'white',
                border: 'none',
                margin: '2px 4px 2px 0'
              }"
              closable
              @close="removeTag(tag.id || tag.tagId)"
            >
              <i v-if="tag.icon" :class="tag.icon" style="margin-right: 4px"></i>
              {{ tag.name || tag.tagName }}
            </el-tag>
          </div>
        </div>

        <!-- 快速添加标签 -->
        <div v-if="allowQuickAdd" class="quick-add-tag">
          <el-input
            v-model="newTagName"
            placeholder="输入新标签名称"
            size="small"
            style="width: 200px; margin-right: 8px"
            @keyup.enter="addNewTag"
          />
          <el-button type="primary" size="small" :disabled="!newTagName.trim()" @click="addNewTag">
            添加标签
          </el-button>
        </div>
      </div>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listCategory } from '@/api/admin/category'
import { listTag, addTag } from '@/api/admin/tag'
import { ElMessage } from '@/plugins/element-plus-service'

// Props
const props = defineProps({
  selectedTags: {
    type: Array,
    default: () => []
  },
  selectedCategory: {
    type: [Number, String],
    default: null
  },
  showCategory: {
    type: Boolean,
    default: true
  },
  showTags: {
    type: Boolean,
    default: true
  },
  allowQuickAdd: {
    type: Boolean,
    default: true
  },
  placeholder: {
    type: String,
    default: '请选择标签'
  }
})

// Emits
const emit = defineEmits(['update:selectedTags', 'update:selectedCategory', 'change'])

// 响应式数据
const categoryOptions = ref([])
const tagOptions = ref([])
const newTagName = ref('')

// 计算属性
const localSelectedCategory = computed({
  get: () => props.selectedCategory,
  set: value => {
    emit('update:selectedCategory', value)
    emit('change', { category: value, tags: props.selectedTags })
  }
})

const localSelectedTags = computed({
  get: () => props.selectedTags,
  set: value => {
    emit('update:selectedTags', value)
    emit('change', { category: props.selectedCategory, tags: value })
  }
})

// 计算属性
const selectedTagsDisplay = computed(() => {
  return tagOptions.value.filter(tag => localSelectedTags.value.includes(tag.id || tag.tagId))
})

// 方法
const handleCategoryChange = value => {
  localSelectedCategory.value = value
}

const handleTagChange = value => {
  localSelectedTags.value = value
}

const removeTag = tagId => {
  const currentTags = [...localSelectedTags.value]
  const index = currentTags.indexOf(tagId)
  if (index > -1) {
    currentTags.splice(index, 1)
    localSelectedTags.value = currentTags
  }
}

const addNewTag = async () => {
  if (!newTagName.value.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }

  // 检查标签是否已存在
  const existingTag = tagOptions.value.find(
    tag => (tag.name || tag.tagName) === newTagName.value.trim()
  )

  if (existingTag) {
    ElMessage.warning('标签已存在')
    // 如果标签存在但未选中，则选中它
    const tagId = existingTag.id || existingTag.tagId
    if (!localSelectedTags.value.includes(tagId)) {
      localSelectedTags.value = [...localSelectedTags.value, tagId]
    }
    newTagName.value = ''
    return
  }

  try {
    // 创建新标签
    const newTag = {
      name: newTagName.value.trim(),
      color: getRandomColor(),
      description: `自动创建的标签：${newTagName.value.trim()}`
    }

    const response = await addTag(newTag)

    if (response.code === 200) {
      ElMessage.success('标签创建成功')

      // 重新加载标签列表
      await getTagList()

      // 自动选中新创建的标签
      const createdTag = tagOptions.value.find(
        tag => (tag.name || tag.tagName) === newTagName.value.trim()
      )

      if (createdTag) {
        const tagId = createdTag.id || createdTag.tagId
        if (!localSelectedTags.value.includes(tagId)) {
          localSelectedTags.value = [...localSelectedTags.value, tagId]
        }
      }

      newTagName.value = ''
    } else {
      ElMessage.error(response.msg || '创建标签失败')
    }
  } catch (error) {
    console.error('创建标签失败:', error)
    ElMessage.error('创建标签失败')
  }
}

const getRandomColor = () => {
  const colors = [
    '#FF6B6B',
    '#4ECDC4',
    '#45B7D1',
    '#96CEB4',
    '#FECA57',
    '#FF9FF3',
    '#54A0FF',
    '#5F27CD',
    '#00D2D3',
    '#FF9F43',
    '#10AC84',
    '#EE5A24'
  ]
  return colors[Math.floor(Math.random() * colors.length)]
}

const getCategoryList = async () => {
  try {
    const response = await listCategory()
    categoryOptions.value = response.rows || []
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const getTagList = async () => {
  try {
    const response = await listTag()
    tagOptions.value = response.rows || []
  } catch (error) {
    console.error('获取标签列表失败:', error)
  }
}

// 生命周期
onMounted(() => {
  if (props.showCategory) {
    getCategoryList()
  }
  if (props.showTags) {
    getTagList()
  }
})

// 暴露方法给父组件
defineExpose({
  refreshCategories: getCategoryList,
  refreshTags: getTagList,
  clearSelection: () => {
    localSelectedCategory.value = null
    localSelectedTags.value = []
  }
})
</script>

<style scoped>
.tag-category-selector {
  width: 100%;
}

.tag-selector-container {
  width: 100%;
}

.selected-tags {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.selected-tags-label {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.selected-tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.quick-add-tag {
  margin-top: 10px;
  padding: 10px;
  background-color: #fafbfc;
  border-radius: 4px;
  border: 1px dashed #d9d9d9;
  display: flex;
  align-items: center;
}

.quick-add-tag:hover {
  border-color: var(--el-color-primary);
  background-color: color-mix(in srgb, var(--el-color-primary) 6%, #fff);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .quick-add-tag {
    flex-direction: column;
    align-items: stretch;
  }

  .quick-add-tag .el-input {
    width: 100% !important;
    margin-right: 0 !important;
    margin-bottom: 8px;
  }
}

/* 深色模式：标签选择器浅色背景/边框/文字适配 */
html.dark .selected-tags {
  background-color: var(--el-bg-color-overlay, #112240);
  border-color: var(--el-border-color, #112240);
}

html.dark .selected-tags-label {
  color: var(--el-text-color-regular, #ccd6f6);
}

html.dark .quick-add-tag {
  background-color: var(--el-bg-color-overlay, #112240);
  border-color: var(--el-border-color, #112240);
}

html.dark .quick-add-tag:hover {
  border-color: var(--el-color-primary, #00d4ff);
  background-color: var(--el-color-primary-light-9, rgba(0, 212, 255, 0.1));
}
</style>
