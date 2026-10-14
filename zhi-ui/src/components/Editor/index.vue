<template>
  <div>
    <el-upload
      v-if="type === 'url'"
      :action="uploadUrl"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      name="file"
      :show-file-list="false"
      :headers="headers"
      class="editor-img-uploader"
    >
      <i ref="uploadRef" class="editor-img-uploader"></i>
    </el-upload>
  </div>
  <div class="editor">
    <quill-editor
      ref="quillEditorRef"
      v-model:content="content"
      content-type="html"
      :options="options"
      :style="styles"
      @text-change="() => $emit('update:modelValue', content)"
    />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable no-alert */
import { ref, computed, watch, onMounted, onUnmounted, toRaw } from 'vue'
import { getCurrentInstance } from 'vue'
import axios from 'axios'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { getToken } from '@/utils/auth'
import { compressImage } from '@/utils/imageUtils'

defineOptions({
  name: 'RichTextEditor'
})

const { proxy } = getCurrentInstance()

const quillEditorRef = ref<any>()
// 添加安全检查，防止环境变量未定义
const baseApi = import.meta.env?.VITE_APP_BASE_API || '/dev-api'
const uploadUrl = ref(baseApi + '/common/upload/compressed') // 使用压缩上传接口，与头像上传一致的 Thumbnailator 方案
const headers = ref({
  Authorization: 'Bearer ' + getToken()
})

const props = defineProps({
  /* 编辑器的内容 */
  modelValue: {
    type: String
  },
  /* 高度 */
  height: {
    type: Number,
    default: null
  },
  /* 最小高度 */
  minHeight: {
    type: Number,
    default: null
  },
  /* 只读 */
  readOnly: {
    type: Boolean,
    default: false
  },
  /* 上传文件大小限制(MB) */
  fileSize: {
    type: Number,
    default: 5
  },
  /* 类型（base64格式、url格式） */
  type: {
    type: String,
    default: 'url'
  }
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const options = ref({
  theme: 'snow',
  bounds: document.body,
  debug: 'warn' as const,
  modules: {
    // 工具栏配置
    toolbar: {
      container: [
        [{ font: [] }, { size: ['small', false, 'large', 'huge'] }], // 字体和大小
        [{ header: [1, 2, 3, 4, 5, 6, false] }], // 标题
        ['bold', 'italic', 'underline', 'strike'], // 加粗 斜体 下划线 删除线
        [{ color: [] }, { background: [] }], // 字体颜色、背景色
        [{ script: 'sub' }, { script: 'super' }], // 上标/下标
        [{ list: 'ordered' }, { list: 'bullet' }], // 有序、无序列表
        [{ indent: '-1' }, { indent: '+1' }], // 缩进
        [{ direction: 'rtl' }], // 文本方向
        [{ align: [] }], // 对齐方式
        ['blockquote', 'code-block'], // 引用、代码块
        ['link', 'image', 'video', 'file'], // 链接、图片、视频、文件
        [{ table: true }], // 表格
        ['clean'] // 清除格式
      ],
      handlers: {
        image: function () {
          // 图片上传处理
          triggerUpload()
        },
        link: function (value) {
          // 增强链接功能
          const quill = getQuill()
          if (!quill) return
          if (value) {
            const href = prompt('请输入链接URL:')
            if (href) {
              // 检查是否选中了文本
              const range = getSafeRange(quill)
              if (range && range.length > 0) {
                // 如果选中了文本，则添加链接到选中文本
                quill.format('link', href)
              } else {
                // 如果没有选中文本，则插入链接
                const text = prompt('请输入链接文本:', href)
                if (text) {
                  quill.insertText(range.index, text, 'link', href)
                  quill.setSelection(range.index + text.length)
                  emitContent()
                }
              }
            }
          } else {
            quill.format('link', false)
          }
        },
        video: function () {
          // 增强视频功能
          const quill = getQuill()
          if (!quill) return
          const range = getSafeRange(quill)

          // 提供选项：上传本地视频或插入在线视频
          const choice = prompt(
            '请选择视频插入方式:\n1. 上传本地视频\n2. 插入在线视频\n请输入1或2:',
            '2'
          )

          if (choice === '1') {
            // 上传本地视频
            triggerUpload()
          } else if (choice === '2') {
            // 插入在线视频
            const url = prompt('请输入视频URL (支持YouTube、Bilibili等平台):')
            if (url) {
              // 检查是否为YouTube或Bilibili链接，生成嵌入代码
              let embedUrl = url
              if (url.includes('youtube.com') || url.includes('youtu.be')) {
                // YouTube链接处理
                const videoId = url.match(
                  /(?:youtube\.com\/(?:[^/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?/\s]{11})/
                )
                if (videoId && videoId[1]) {
                  embedUrl = `https://www.youtube.com/embed/${videoId[1]}`
                }
              } else if (url.includes('bilibili.com')) {
                // Bilibili链接处理
                const videoId = url.match(/bilibili\.com\/video\/([a-zA-Z0-9]+)/)
                if (videoId && videoId[1]) {
                  embedUrl = `https://player.bilibili.com/player.html?bvid=${videoId[1]}`
                }
              }

              quill.insertEmbed(range.index, 'video', embedUrl)
              quill.setSelection(range.index + 1)
              emitContent()
            }
          }
        },
        file: function () {
          // 文件链接插入功能
          const quill = getQuill()
          if (!quill) return
          const range = getSafeRange(quill)

          const choice = prompt(
            '请选择文件插入方式:\n1. 上传本地文件\n2. 插入文件链接\n请输入1或2:',
            '1'
          )

          if (choice === '1') {
            // 上传本地文件
            triggerUpload()
          } else if (choice === '2') {
            // 插入文件链接
            const url = prompt('请输入文件链接URL:')
            if (url) {
              const fileName = prompt('请输入文件显示名称:', '下载文件')
              if (fileName) {
                // 插入文件链接
                quill.insertText(range.index, fileName, 'link', url)
                quill.setSelection(range.index + fileName.length)
                emitContent()
              }
            }
          }
        }
      }
    },
    syntax: false,
    table: true, // 启用表格功能
    clipboard: {
      // 增强粘贴功能
      matchers: [['img', handleImagePaste]]
    }
  },
  placeholder: '请输入内容...支持插入超链接、图片、视频和文件链接',
  readOnly: props.readOnly
})

const styles = computed(() => {
  const style: Record<string, string> = {}
  if (props.minHeight) {
    style.minHeight = `${props.minHeight}px`
  }
  if (props.height) {
    style.height = `${props.height}px`
  }
  return style
})

const content = ref('')

function getQuill() {
  return quillEditorRef.value?.getQuill?.()
}

function getSafeRange(quill: any) {
  return (
    quill.getSelection?.(true) ||
    quill.selection?.savedRange || {
      index: Math.max(0, (quill.getLength?.() || 1) - 1),
      length: 0
    }
  )
}

function emitContent() {
  const quill = getQuill()
  if (quill) {
    content.value = quill.root?.innerHTML || quill.getText?.() || content.value
    emit('update:modelValue', content.value)
  }
}

function triggerUpload() {
  ;(proxy?.$refs.uploadRef as HTMLElement | undefined)?.click?.()
}

function focusEditor() {
  getQuill()?.focus?.()
}

function format(formatName: string, value: string | number | boolean = true) {
  const quill = getQuill()
  if (!quill) return
  quill.focus?.()
  quill.format(formatName, value, 'user')
  emitContent()
}

function insertLink(href: string, text?: string) {
  const quill = getQuill()
  if (!quill || !href) return
  quill.focus?.()
  const range = getSafeRange(quill)
  if (range.length > 0) {
    quill.format('link', href, 'user')
  } else {
    const label = text || href
    quill.insertText(range.index, label, 'link', href)
    quill.setSelection(range.index + label.length, 0)
  }
  emitContent()
}

function insertTable() {
  const quill = getQuill()
  if (!quill) return
  quill.focus?.()
  const table = quill.getModule?.('table')
  if (table?.insertTable) {
    table.insertTable(2, 2)
  } else {
    const range = getSafeRange(quill)
    quill.clipboard?.dangerouslyPasteHTML?.(
      range.index,
      '<table><tbody><tr><td> </td><td> </td></tr><tr><td> </td><td> </td></tr></tbody></table>'
    )
  }
  emitContent()
}

defineExpose({
  format,
  focus: focusEditor,
  insertLink,
  insertTable,
  triggerUpload
})

// 设置 watch 监听器，Vue 3 会自动清理
watch(
  () => props.modelValue,
  v => {
    if (v !== content.value) {
      content.value = v === undefined ? '<p></p>' : v
    }
  },
  { immediate: true }
)

// 上传文件到本站，返回完整访问 URL；失败返回 null
async function uploadToSite(file: File): Promise<string | null> {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await axios.post(uploadUrl.value, formData, {
      headers: { 'Content-Type': 'multipart/form-data', Authorization: headers.value.Authorization }
    })
    const data = res.data
    if (data && data.code === 200 && data.fileName) {
      return baseApi + data.fileName
    }
  } catch (error) {
    console.warn('图片上传失败:', error)
  }
  return null
}

// 向编辑器当前光标处插入图片/视频
function insertEmbed(type: string, url: string) {
  const quill = getQuill()
  if (!quill) return
  const length = getSafeRange(quill).index
  if (type === 'image' || type.startsWith('image/')) {
    quill.insertEmbed(length, 'image', url)
  } else if (type === 'video' || type.startsWith('video/')) {
    quill.insertEmbed(length, 'video', url)
  }
  quill.setSelection(length + 1)
  emitContent()
}

// 图片粘贴处理函数：同步返回原始 delta，不干预粘贴流程
function handleImagePaste(_node: any, delta: any) {
  return delta
}

// 如果设置了上传地址则自定义图片上传事件
onMounted(() => {
  if (props.type === 'url' && quillEditorRef.value) {
    try {
      const quill = quillEditorRef.value.getQuill()
      const toolbar = quill.getModule('toolbar')
      if (toolbar) {
        toolbar.addHandler('image', value => {
          if (value) {
            ;(proxy.$refs.uploadRef as any)?.click()
          } else {
            quill.format('image', false)
          }
        })
      }
      quill.root.addEventListener('paste', handlePasteCapture, true)
    } catch (error) {
      console.error('初始化编辑器上传处理失败:', error)
    }
  }
})

onUnmounted(() => {
  if (props.type === 'url' && quillEditorRef.value) {
    try {
      const quill = quillEditorRef.value.getQuill()
      quill.root.removeEventListener('paste', handlePasteCapture, true)
    } catch (error) {
      console.error('清理编辑器上传处理失败:', error)
    }
  }
})

// 上传前校检格式和大小，并对大图片进行前端预压缩
async function handleBeforeUpload(file) {
  // 支持的文件类型
  const imageTypes = [
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/svg+xml'
  ]
  const videoTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime']
  const fileTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'text/plain',
    'application/zip',
    'application/x-rar-compressed'
  ]

  const isImage = imageTypes.includes(file.type)
  const isVideo = videoTypes.includes(file.type)
  const isFile = fileTypes.includes(file.type) || file.type.startsWith('application/')

  // 检验文件格式
  if (!isImage && !isVideo && !isFile) {
    ;(proxy as any).$modal.msgError(`文件格式不支持! 支持格式:
图片: JPG, PNG, GIF, WebP, SVG
视频: MP4, WebM, OGG, MOV
文档: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, ZIP, RAR`)
    return false
  }

  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      ;(proxy as any).$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }

  // 前端预压缩：对 >2MB 的图片，先压缩再上传，减少上传带宽消耗
  if (isImage && file.size > 2 * 1024 * 1024) {
    try {
      const compressedFile = await compressImage(file, {
        maxWidth: 1920,
        maxHeight: 1920,
        quality: 0.85
      })
      return compressedFile
    } catch (e) {
      console.warn('图片预压缩失败，使用原始文件上传', e)
    }
  }

  return true
}

// 上传成功处理
function handleUploadSuccess(res, file) {
  // 如果上传成功
  if (res.code === 200) {
    // 获取富文本实例
    insertEmbed(file.type || 'image', baseApi + res.fileName)
  } else {
    ;(proxy as any).$modal.msgError('文件插入失败')
  }
}

// 上传失败处理
function handleUploadError() {
  ;(proxy as any).$modal.msgError('图片插入失败')
}

// 复制粘贴图片处理
function handlePasteCapture(e: any) {
  const clipboard = e.clipboardData || (window as any).clipboardData
  if (clipboard && clipboard.items) {
    for (let i = 0; i < clipboard.items.length; i++) {
      const item = clipboard.items[i]
      if (item.type.indexOf('image') !== -1) {
        e.preventDefault()
        const file = item.getAsFile()
        insertImage(file)
        break
      }
    }
  }
}

async function insertImage(file: File) {
  // 前端预压缩大图片（>2MB），减少上传带宽消耗
  let upload = file
  if (file.type.startsWith('image/') && file.size > 2 * 1024 * 1024) {
    try {
      upload = await compressImage(file, { maxWidth: 1920, maxHeight: 1920, quality: 0.85 })
    } catch (e) {
      console.warn('图片预压缩失败，使用原始文件上传', e)
    }
  }
  const url = await uploadToSite(upload)
  if (!url) {
    ;(proxy as any).$modal.msgError('图片上传失败')
    return
  }
  insertEmbed('image', url)
}
</script>

<style scoped>
.editor-img-uploader {
  display: none;
}
.editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.editor:hover {
  border-color: #c0c4cc;
}
.editor:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}
.ql-toolbar {
  border-bottom: 1px solid #dcdfe6 !important;
  background-color: #f8f9fa !important;
}
.ql-container {
  border: none !important;
  font-size: 14px;
  line-height: 1.6;
}
.quill-img {
  display: none;
}
</style>

<style>
/* 全局样式 */
.ql-snow .ql-tooltip[data-mode='link']::before {
  content: '请输入链接地址:';
}
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0px;
  content: '保存';
  padding-right: 0px;
}
.ql-snow .ql-tooltip[data-mode='video']::before {
  content: '请输入视频地址:';
}

/* 字体大小选择器 */
.ql-snow .ql-picker.ql-size .ql-picker-label::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: '14px';
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='small']::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='small']::before {
  content: '10px';
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='large']::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='large']::before {
  content: '18px';
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value='huge']::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value='huge']::before {
  content: '32px';
}

/* 标题选择器 */
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: '文本';
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='1']::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='1']::before {
  content: '标题1';
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='2']::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='2']::before {
  content: '标题2';
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='3']::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='3']::before {
  content: '标题3';
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='4']::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='4']::before {
  content: '标题4';
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='5']::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='5']::before {
  content: '标题5';
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value='6']::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value='6']::before {
  content: '标题6';
}

/* 字体选择器 */
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: '标准字体';
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value='serif']::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value='serif']::before {
  content: '衬线字体';
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value='monospace']::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value='monospace']::before {
  content: '等宽字体';
}

/* 代码块样式 */
.ql-syntax {
  background-color: #f6f8fa !important;
  border-radius: 4px;
  padding: 12px !important;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace !important;
  font-size: 13px;
  line-height: 1.45;
}

/* 表格样式 */
.ql-editor table {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}
.ql-editor table td,
.ql-editor table th {
  border: 1px solid #ddd;
  padding: 8px;
}
.ql-editor table th {
  background-color: #f5f5f5;
  font-weight: bold;
}

/* 引用样式 */
.ql-editor blockquote {
  border-left: 4px solid #ddd;
  margin: 16px 0;
  padding-left: 16px;
  color: #666;
  font-style: italic;
}

/* 图片样式 */
.ql-editor img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 视频样式 */
.ql-editor iframe {
  max-width: 100%;
  border-radius: 4px;
}

/* 工具栏按钮悬停效果 */
.ql-snow .ql-toolbar .ql-formats button:hover {
  background-color: #e8f4ff !important;
  border-radius: 3px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ql-toolbar .ql-formats {
    margin-bottom: 5px;
  }
  .ql-toolbar .ql-formats button {
    margin: 2px;
  }
}
</style>
