<template>
  <div class="component-upload-image">
    <el-upload
      ref="imageUpload"
      :multiple="multiple"
      :disabled="disabled"
      :action="uploadImgUrl"
      list-type="picture-card"
      :on-success="handleUploadSuccess"
      :before-upload="handleBeforeUpload"
      :data="data"
      :limit="limit"
      :on-error="handleUploadError"
      :on-exceed="handleExceed"
      :before-remove="handleDelete"
      :show-file-list="true"
      :headers="headers"
      :file-list="fileList"
      :on-preview="handlePictureCardPreview"
      :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon">
        <plus />
      </el-icon>
    </el-upload>
    <!-- 上传提示 -->
    <div v-if="showTip && !disabled" class="el-upload__tip">
      请上传
      <template v-if="fileSize">
        大小不超过
        <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为
        <b style="color: #f56c6c">{{ fileType.join('/') }}</b>
      </template>
      的文件
    </div>

    <el-dialog v-model="dialogVisible" title="预览" width="800px" append-to-body>
      <img
        :src="dialogImageUrl"
        alt="图片预览"
        style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>

    <!-- 裁剪弹窗（crop 开启时生效） -->
    <el-dialog
      v-model="cropVisible"
      title="裁剪图片"
      width="900px"
      append-to-body
      :close-on-click-modal="false"
      @open="cropRenderVisible = true"
      @closed="onCropClosed"
    >
      <el-row :gutter="12">
        <el-col :xs="24" :md="16" :style="{ height: '380px' }">
          <vue-cropper
            v-if="cropRenderVisible"
            ref="cropperRef"
            :img="cropImg"
            :auto-crop="true"
            :fixed="true"
            :fixed-number="fixedNumberPair"
            :center-box="true"
            :info="false"
            :full="true"
            :output-type="'jpeg'"
            :output-size="0.8"
            @real-time="realTime"
          />
        </el-col>
        <el-col :xs="24" :md="8" :style="{ height: '380px' }" class="crop-preview-col">
          <div class="crop-preview-box" :style="previewBoxOuterStyle">
            <div :style="previewBoxInnerStyle">
              <img :src="previews.url" :style="previews.img" alt="裁剪预览" />
            </div>
          </div>
        </el-col>
      </el-row>
      <el-row class="crop-actions" align="middle">
        <el-upload
          action="#"
          :http-request="noopRequest"
          :show-file-list="false"
          :before-upload="handleCropReselect"
        >
          <el-button>
            重新选择
            <el-icon class="el-icon--right"><upload /></el-icon>
          </el-button>
        </el-upload>
        <el-button icon="Plus" @click="changeScale(1)" />
        <el-button icon="Minus" @click="changeScale(-1)" />
        <el-button icon="RefreshLeft" @click="rotateLeft()" />
        <el-button icon="RefreshRight" @click="rotateRight()" />
        <div class="crop-actions-right">
          <el-button :disabled="cropUploading" @click="uploadCropped(true)">
            跳过裁剪直接上传
          </el-button>
          <el-button
            type="primary"
            :loading="cropUploading"
            :disabled="cropUploading"
            @click="uploadCropped(false)"
          >
            确认裁剪并上传
          </el-button>
        </div>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import 'vue-cropper/dist/index.css'
import { VueCropper } from 'vue-cropper'
import { ref, computed, watch, getCurrentInstance, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from '@/plugins/element-plus-service'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { normalizeUploadUrl } from '@/utils/uploadUrl'
import Sortable from 'sortablejs'

const props = defineProps({
  modelValue: [String, Object, Array],
  // 上传接口地址
  action: {
    type: String,
    default: '/common/upload'
  },
  // 上传携带的参数
  data: {
    type: Object
  },
  // 图片数量限制
  limit: {
    type: Number,
    default: 5
  },
  // 大小限制(MB)
  fileSize: {
    type: Number,
    default: 5
  },
  // 文件类型, 例如['png', 'jpg', 'jpeg']
  fileType: {
    type: Array,
    default: () => ['png', 'jpg', 'jpeg']
  },
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  // 禁用组件（仅查看图片）
  disabled: {
    type: Boolean,
    default: false
  },
  // 拖动排序
  drag: {
    type: Boolean,
    default: true
  },
  // 是否允许一次选择多张（开启裁剪时应关闭，否则多个裁剪弹窗会叠加）
  multiple: {
    type: Boolean,
    default: true
  },
  // 选图后先弹裁剪框（封面对齐列表卡片比例等场景）
  crop: {
    type: Boolean,
    default: false
  },
  // 裁剪宽高比（宽/高），配合 crop 使用
  aspectRatio: {
    type: Number,
    default: 16 / 9
  }
})

const { proxy } = getCurrentInstance()
const emit = defineEmits(['update:modelValue'])
const number = ref(0)
const uploadList = ref([])
const dialogImageUrl = ref('')
const dialogVisible = ref(false)
const fileTypes = computed<string[]>(() => props.fileType as string[])
// 图片上传
const baseApi = import.meta.env?.VITE_APP_BASE_API || '/dev-api'
const uploadImgUrl = ref(baseApi + props.action) // 上传的图片服务器地址
const headers = ref({ Authorization: 'Bearer ' + getToken() })
const fileList = ref([])
const showTip = computed(() => props.isShowTip && (props.fileType || props.fileSize))

// 裁剪弹窗状态
const cropVisible = ref(false)
const cropRenderVisible = ref(false)
const cropUploading = ref(false)
const cropImg = ref('')
const cropFile = ref<File | null>(null)
const cropperRef = ref()
const previews = ref<any>({})
const fixedNumberPair = computed<[number, number]>(() => [props.aspectRatio, 1])
const previewBoxOuterStyle = computed(() => {
  const w = Number(previews.value.w) || 0
  const h = Number(previews.value.h) || 0
  if (!w || !h) return { visibility: 'hidden' as const }
  const k = Math.min(1, 240 / w, 200 / h)
  return { width: `${w * k}px`, height: `${h * k}px` }
})
const previewBoxInnerStyle = computed(() => {
  const w = Number(previews.value.w) || 0
  const h = Number(previews.value.h) || 0
  if (!w || !h) return {}
  const k = Math.min(1, 240 / w, 200 / h)
  return {
    width: `${w}px`,
    height: `${h}px`,
    overflow: 'hidden',
    transform: `scale(${k})`,
    transformOrigin: 'top left'
  }
})

// 设置 watch 监听器，Vue 3 会自动清理
watch(
  () => props.modelValue,
  val => {
    if (val) {
      // 首先将值转为数组
      const list = Array.isArray(val) ? val : String(val).split(',')
      // 然后将数组转为对象数组。
      // 历史数据里可能存了后端返回的绝对地址（如 http://zhi-admin:8080/profile/...，
      // 容器名浏览器解析不了），统一收敛成站内相对路径再渲染。
      fileList.value = list.map(item => {
        if (typeof item === 'string') {
          const path = normalizeUploadUrl(item)
          item = { name: path, url: path }
        }
        return item
      })
    } else {
      fileList.value = []
      return []
    }
  },
  { deep: true, immediate: true }
)

// 上传前loading加载
function handleBeforeUpload(file) {
  let isImg = false
  if (fileTypes.value.length) {
    let fileExtension = ''
    if (file.name.lastIndexOf('.') > -1) {
      fileExtension = file.name.slice(file.name.lastIndexOf('.') + 1)
    }
    isImg = fileTypes.value.some(type => {
      if (file.type.indexOf(type) > -1) return true
      if (fileExtension && fileExtension.indexOf(type) > -1) return true
      return false
    })
  } else {
    isImg = file.type.indexOf('image') > -1
  }
  if (!isImg) {
    ;(proxy as any).$modal.msgError(
      `文件格式不正确，请上传${fileTypes.value.join('/')}图片格式文件!`
    )
    return false
  }
  if (file.name.includes(',')) {
    ;(proxy as any).$modal.msgError('文件名不正确，不能包含英文逗号!')
    return false
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      ;(proxy as any).$modal.msgError(`上传图片大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  if (props.crop) {
    // 拦截 el-upload 自带上传，改走裁剪弹窗，确认后手动上传
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => {
      cropImg.value = String(reader.result || '')
      cropFile.value = file
      cropVisible.value = true
    }
    return false
  }
  ;(proxy as any).$modal.loading('正在上传图片，请稍候...')
  number.value++
}

// 文件个数超出
function handleExceed() {
  ;(proxy as any).$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
}

// 上传成功回调
function handleUploadSuccess(res: any, file: any) {
  if (res.code === 200) {
    applyUploadSuccess(res)
  } else {
    number.value--
    ;(proxy as any).$modal.closeLoading()
    ;(proxy as any).$modal.msgError(res.msg)
    ;(proxy.$refs.imageUpload as any).handleRemove(file)
    uploadedSuccessfully()
  }
}

// 成功响应落地：供 el-upload 回调与裁剪手动上传共用
function applyUploadSuccess(res: any) {
  // 只认服务端相对路径：后端 `url` 是「请求 Host + 路径」的绝对地址，经 Docker/Vite
  // 代理会变成 http://zhi-admin:8080/... 这类浏览器无法解析的地址（图片 404）。
  // fileName = /profile/upload/...，dev 由 Vite、prod 由 nginx 代理 /profile。
  const path = normalizeUploadUrl(res.fileName || res.url)
  uploadList.value.push({
    name: res.fileName || res.url,
    url: path
  })
  uploadedSuccessfully()
}

// 删除图片
function handleDelete(file) {
  const findex = fileList.value.map(f => f.name).indexOf(file.name)
  if (findex > -1 && uploadList.value.length === number.value) {
    fileList.value.splice(findex, 1)
    emit('update:modelValue', listToString(fileList.value, ','))
    return false
  }
}

// 上传结束处理
function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter(f => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit('update:modelValue', listToString(fileList.value, ','))
    ;(proxy as any).$modal.closeLoading()
  }
}

// 上传失败
function handleUploadError() {
  ;(proxy as any).$modal.msgError('上传图片失败')
  ;(proxy as any).$modal.closeLoading()
}

// 预览
function handlePictureCardPreview(file) {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

// ===== 裁剪弹窗 =====

function noopRequest() {}

// 弹窗内重新选图
function handleCropReselect(file: File) {
  if (file.type.indexOf('image/') === -1) {
    ;(proxy as any).$modal.msgError('文件格式错误，请上传图片类型文件。')
    return false
  }
  const reader = new FileReader()
  reader.readAsDataURL(file)
  reader.onload = () => {
    cropImg.value = String(reader.result || '')
    cropFile.value = file
  }
  return false
}

function changeScale(num: number) {
  ;(cropperRef.value as any)?.changeScale(num || 1)
}

function rotateLeft() {
  ;(cropperRef.value as any)?.rotateLeft()
  syncPreview()
}

function rotateRight() {
  ;(cropperRef.value as any)?.rotateRight()
  syncPreview()
}

// 预览更新有 16ms 节流，旋转末尾「选框被钳制」那一拍会被丢弃，延迟一拍强制刷新
function syncPreview() {
  setTimeout(() => (cropperRef.value as any)?.showPreview?.(), 40)
}

function realTime(data: any) {
  previews.value = data || {}
}

/**
 * 裁剪上传：skipped=true 跳过裁剪直接用原文件；成功后均关闭弹窗
 */
function uploadCropped(skipped: boolean) {
  if (cropUploading.value) return
  const source = skipped || !cropperRef.value ? cropFile.value : null
  if (source) {
    doCropUpload(source)
    return
  }
  cropUploading.value = true
  ;(cropperRef.value as any).getCropBlob((data: Blob) => {
    cropUploading.value = false
    if (data) doCropUpload(data)
  })
}

function doCropUpload(blob: Blob) {
  ;(proxy as any).$modal.loading('正在上传图片，请稍候...')
  number.value++
  const formData = new FormData()
  formData.append('file', blob, cropFile.value?.name || 'cover.jpg')
  request({
    url: props.action,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
    .then((res: any) => {
      applyUploadSuccess(res)
      cropVisible.value = false
    })
    .catch(() => {
      number.value = Math.max(0, number.value - 1)
      ;(proxy as any).$modal.closeLoading()
      ;(proxy as any).$modal.msgError('上传图片失败')
    })
}

function onCropClosed() {
  cropRenderVisible.value = false
  previews.value = {}
}

// 对象转成指定字符串分隔
function listToString(list, separator) {
  let strs = ''
  separator = separator || ','
  for (const i in list) {
    if (undefined !== list[i].url && list[i].url.indexOf('blob:') !== 0) {
      // 入库统一相对路径（兼容历史绝对地址/带 baseApi 前缀的旧值）
      strs += normalizeUploadUrl(list[i].url) + separator
    }
  }
  return strs !== '' ? strs.substr(0, strs.length - 1) : ''
}

// 初始化拖拽排序
onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const uploadRef = (proxy as any).$refs.imageUpload
      const element = uploadRef?.$el?.querySelector('.el-upload-list')
      Sortable.create(element, {
        onEnd: evt => {
          const movedItem = fileList.value.splice(evt.oldIndex, 1)[0]
          fileList.value.splice(evt.newIndex, 0, movedItem)
          emit('update:modelValue', listToString(fileList.value, ','))
        }
      })
    })
  }
})
</script>

<style scoped lang="scss">
// .el-upload--picture-card 控制加号部分
:deep(.hide .el-upload--picture-card) {
  display: none;
}

:deep(.el-upload.el-upload--picture-card.is-disabled) {
  display: none !important;
}

.crop-preview-col {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.crop-preview-box {
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.15);
}

.crop-actions {
  margin-top: 12px;

  :deep(.el-upload) {
    margin-right: 12px;
  }

  .crop-actions-right {
    margin-left: auto;
  }
}
</style>
