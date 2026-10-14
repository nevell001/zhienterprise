<template>
  <div class="avatar-upload">
    <!-- 头像预览 -->
    <div class="avatar-preview" :class="{ 'avatar-uploading': uploading }">
      <img
        :src="processedAvatarUrl"
        :alt="alt || '用户头像'"
        :style="{ width: previewSize + 'px', height: previewSize + 'px' }"
        @error="handleAvatarError"
        @load="handleAvatarLoad"
      />
      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-overlay">
        <el-progress
          type="circle"
          :percentage="uploadProgress"
          :width="previewSize * 0.6"
          :stroke-width="4"
        />
      </div>
      <!-- 上传按钮 -->
      <div v-if="!uploading && editable" class="upload-button" @click="triggerUpload">
        <el-icon><Camera /></el-icon>
        <span>更换头像</span>
      </div>
    </div>

    <!-- 文件输入 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />

    <!-- 头像信息 -->
    <div v-if="showInfo && avatarInfo" class="avatar-info">
      <div class="info-item">
        <span class="label">文件名:</span>
        <span class="value">{{ avatarInfo.fileName }}</span>
      </div>
      <div v-if="avatarInfo.originalSize" class="info-item">
        <span class="label">原始大小:</span>
        <span class="value">{{ formatFileSize(avatarInfo.originalSize) }}</span>
      </div>
      <div v-if="avatarInfo.compressedSize" class="info-item">
        <span class="label">压缩后:</span>
        <span class="value">{{ formatFileSize(avatarInfo.compressedSize) }}</span>
      </div>
      <div v-if="avatarInfo.compressionRatio" class="info-item">
        <span class="label">压缩率:</span>
        <span class="value">{{ avatarInfo.compressionRatio }}%</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="error-message">
      <el-icon><Warning /></el-icon>
      <span>{{ errorMessage }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from '@/plugins/element-plus-service'
import { Camera, Warning } from '@element-plus/icons-vue'
import { uploadAvatar, processAvatarUrl, checkAvatarExists } from '@/api/site/avatar'
import { formatFileSize } from '@/utils/imageUtils'

// Props
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  alt: {
    type: String,
    default: '用户头像'
  },
  previewSize: {
    type: Number,
    default: 80
  },
  editable: {
    type: Boolean,
    default: true
  },
  showInfo: {
    type: Boolean,
    default: false
  },
  autoCheck: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['update:modelValue', 'upload-success', 'upload-error', 'change'])

// Refs
const fileInput = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const errorMessage = ref('')

interface AvatarInfo {
  fileName: string
  originalSize?: number
  compressedSize?: number
  compressionRatio?: number
}

const avatarInfo = ref<AvatarInfo | null>(null)

// Computed
const processedAvatarUrl = computed(() => {
  return processAvatarUrl(props.modelValue)
})

// Watchers
// 设置 watch 监听器，Vue 3 会自动清理
watch(
  () => props.modelValue,
  newVal => {
    if (props.autoCheck && newVal && !newVal.startsWith('data:')) {
      checkAvatarExists(newVal).then(exists => {
        if (!exists) {
          console.warn('头像文件不存在:', newVal)
        }
      })
    }
  }
)

// Methods
const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async event => {
  const file = event.target.files[0]
  if (!file) return

  // 清除之前的错误
  errorMessage.value = ''

  try {
    // 上传头像
    uploading.value = true
    uploadProgress.value = 0

    const result = await uploadAvatar(file, {
      onProgress: progress => {
        uploadProgress.value = Math.round(progress * 100)
      }
    })

    if (result.success) {
      // 更新头像URL
      emit('update:modelValue', result.data.url)
      emit('upload-success', result.data)
      emit('change', result.data.url)

      // 保存头像信息
      avatarInfo.value = {
        fileName: file.name,
        originalSize: result.data.originalSize,
        compressedSize: result.data.compressedSize,
        compressionRatio: result.data.compressionRatio
      }

      ElMessage.success('头像上传成功')
    } else {
      throw new Error(result.error)
    }
  } catch (error: any) {
    console.error('头像上传失败:', error)
    errorMessage.value = error.message
    emit('upload-error', error)
    ElMessage.error('头像上传失败: ' + error.message)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    // 清空文件输入
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

const handleAvatarError = e => {
  console.warn('头像加载失败，使用默认头像')
  const defaultAvatar = processAvatarUrl('')

  if (e.target.src !== defaultAvatar) {
    e.target.src = defaultAvatar
  }
  e.target.onerror = null
}

const handleAvatarLoad = () => {
  // 头像加载成功
  errorMessage.value = ''
}

const validateFile = file => {
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    throw new Error('请选择图片文件')
  }

  // 检查文件大小（5MB限制）
  if (file.size > 5 * 1024 * 1024) {
    throw new Error('头像文件大小不能超过5MB')
  }

  // 检查图片尺寸（这里可以在前端检查，但更准确的是在后端检查）
  return new Promise<void>((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      const { width, height } = img

      if (width < 50 || height < 50) {
        reject(new Error('头像尺寸不能小于50x50像素'))
      } else if (width > 2000 || height > 2000) {
        reject(new Error('头像尺寸不能大于2000x2000像素'))
      } else {
        resolve()
      }
    }
    img.onerror = () => reject(new Error('无法读取图片文件'))
    img.src = URL.createObjectURL(file)
  })
}

// 暴露方法
defineExpose({
  triggerUpload,
  validateFile
})
</script>

<style scoped>
.avatar-upload {
  display: inline-block;
}

.avatar-preview {
  position: relative;
  display: inline-block;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e0e0e0;
  transition: all 0.3s ease;
  cursor: pointer;
}

.avatar-preview:hover {
  border-color: #409eff;
  transform: scale(1.05);
}

.avatar-preview.avatar-uploading {
  border-color: #409eff;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(64, 158, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

.avatar-preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.upload-button {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 0 0 50% 50%;
}

.avatar-preview:hover .upload-button {
  opacity: 1;
}

.upload-button .el-icon {
  font-size: 16px;
  margin-bottom: 2px;
}

.avatar-info {
  margin-top: 10px;
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
}

.info-item {
  display: flex;
  margin-bottom: 4px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item .label {
  font-weight: 500;
  margin-right: 8px;
  min-width: 60px;
}

.info-item .value {
  color: #333;
}

.error-message {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #f56c6c;
  font-size: 12px;
  background: #fef0f0;
  padding: 4px 8px;
  border-radius: 4px;
}

.error-message .el-icon {
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .upload-button {
    opacity: 1;
    background: rgba(0, 0, 0, 0.5);
  }

  .avatar-info {
    font-size: 11px;
  }
}
</style>
