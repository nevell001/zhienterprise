<template>
  <el-popover
    v-model:visible="popoverVisible"
    placement="bottom"
    :width="200"
    trigger="click"
    :teleported="true"
    popper-class="share-popover-wrapper"
  >
    <template #reference>
      <el-button plain>
        <el-icon><Share /></el-icon>
        分享
      </el-button>
    </template>
    <div class="share-popover">
      <div class="share-item" @click="copyLink">
        <el-icon class="share-icon"><Link /></el-icon>
        <span class="share-label">复制链接</span>
      </div>
      <div class="share-item wechat-item" @click="openWechatQR">
        <el-icon class="share-icon wechat-icon"><ChatDotSquare /></el-icon>
        <span class="share-label">微信分享</span>
      </div>
      <div class="share-item weibo-item" @click="shareToWeibo">
        <el-icon class="share-icon weibo-icon"><Promotion /></el-icon>
        <span class="share-label">微博分享</span>
      </div>
      <div class="share-item qq-item" @click="shareToQQ">
        <el-icon class="share-icon qq-icon"><User /></el-icon>
        <span class="share-label">QQ 分享</span>
      </div>
    </div>
  </el-popover>

  <!-- 微信二维码弹窗 -->
  <el-dialog
    v-model="qrDialogVisible"
    title="微信扫一扫分享"
    width="320px"
    center
    :close-on-click-modal="true"
    align-center
  >
    <div class="qr-container">
      <canvas ref="qrCanvasRef" class="qr-canvas"></canvas>
      <p class="qr-tip">打开微信"扫一扫"</p>
      <p class="qr-tip">即可分享给好友或朋友圈</p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Share, Link, ChatDotSquare, Promotion, User } from '@element-plus/icons-vue'
import QRCode from 'qrcode'

interface ArticleData {
  title: string
  summary?: string
  coverUrl?: string
}

const props = defineProps<{
  article: ArticleData
}>()

const popoverVisible = ref(false)
const qrDialogVisible = ref(false)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)

// 获取完整 URL 和封面图 URL
const shareUrl = window.location.href
const shareTitle = props.article.title || ''
const shareSummary = props.article.summary || ''
const shareCover = props.article.coverUrl
  ? new URL(props.article.coverUrl, window.location.origin).href
  : ''

// 复制链接
const copyLink = async () => {
  popoverVisible.value = false
  try {
    await navigator.clipboard.writeText(shareUrl)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    try {
      // 降级方案：使用传统的 execCommand
      const textarea = document.createElement('textarea')
      textarea.value = shareUrl
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      ElMessage.success('链接已复制到剪贴板')
    } catch {
      ElMessage.warning('请手动复制链接：' + shareUrl)
    }
  }
}

// 微信分享 - 显示二维码
const openWechatQR = async () => {
  popoverVisible.value = false
  qrDialogVisible.value = true
  await nextTick()
  if (qrCanvasRef.value) {
    try {
      await QRCode.toCanvas(qrCanvasRef.value, shareUrl, {
        width: 220,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#ffffff'
        }
      })
    } catch {
      ElMessage.error('二维码生成失败')
    }
  }
}

// 微博分享
const shareToWeibo = () => {
  popoverVisible.value = false
  const params = new URLSearchParams()
  params.set('url', shareUrl)
  params.set('title', shareTitle)
  if (shareCover) {
    params.set('pic', shareCover)
  }
  window.open(
    `https://service.weibo.com/share/share.php?${params.toString()}`,
    '_blank',
    'width=615,height=505'
  )
}

// QQ 分享
const shareToQQ = () => {
  popoverVisible.value = false
  const params = new URLSearchParams()
  params.set('url', shareUrl)
  params.set('title', shareTitle)
  params.set('desc', shareSummary || shareTitle)
  if (shareCover) {
    params.set('pics', shareCover)
  }
  params.set('summary', shareSummary || '')
  params.set('site', '知企建站系统')
  window.open(
    `https://connect.qq.com/widget/shareqq/index.html?${params.toString()}`,
    '_blank',
    'width=700,height=540'
  )
}
</script>

<style scoped>
.share-popover {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.share-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--mo-r-md);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.share-item:hover {
  background: var(--mo-n100);
}

html.dark .share-item:hover {
  background: var(--mo-n700);
}

.share-icon {
  font-size: var(--mo-fs-lg);
  flex-shrink: 0;
}

.wechat-icon {
  color: #07c160;
}

.weibo-icon {
  color: #e6162d;
}

.qq-icon {
  color: #12b7f5;
}

.share-label {
  font-size: var(--mo-fs-sm);
  color: #333;
}

.qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.qr-canvas {
  border: 1px solid #e5e7eb;
  border-radius: var(--mo-r-md);
}

.qr-tip {
  margin: var(--mo-sp-2) 0 0;
  font-size: 13px;
  color: #78716c;
  text-align: center;
}

html.dark .share-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

/* 弹层 teleport 到 body，落在 .site-layout 的深色重映射作用域之外，
   所以这里的 --mo-* 取 :root 浅色阶的值：n100 #f5f5f4、n400 #a8a29e。 */
html.dark .share-label {
  color: var(--mo-n100);
}

html.dark .qr-tip {
  color: var(--mo-n400);
}
</style>
