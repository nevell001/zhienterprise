import request from '@/utils/request'

interface UploadOptions {
  onProgress?: (progress: number) => void
}

interface UploadResult {
  success: boolean
  data?: {
    url: string
    fileName: string
    size: number
    originalSize?: number
    compressedSize?: number
    compressionRatio?: number
  }
  error?: string
}

/**
 * 处理头像URL
 * @param avatarUrl - 头像URL
 * @returns 处理后的URL
 */
export function processAvatarUrl(avatarUrl: string | null | undefined): string {
  // 如果没有头像URL，返回空字符串
  if (!avatarUrl || !avatarUrl.trim()) {
    return ''
  }

  // 如果是完整的URL（http/https），直接返回
  if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://')) {
    return avatarUrl
  }

  // 如果是相对路径，添加基础路径
  const baseURL = import.meta.env?.VITE_APP_BASE_API || ''
  if (avatarUrl.startsWith('/')) {
    return baseURL + avatarUrl
  }

  // 其他情况，添加 / 前缀
  return `${baseURL}/${avatarUrl}`
}

/**
 * 上传头像
 * @param file - 头像文件
 * @param options - 上传选项
 * @returns 上传结果
 */
export async function uploadAvatar(file: File, options: UploadOptions = {}): Promise<UploadResult> {
  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = (await request({
      url: '/common/upload/avatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: progressEvent => {
        if (progressEvent.total && options.onProgress) {
          const progress = progressEvent.loaded / progressEvent.total
          options.onProgress(progress)
        }
      }
    })) as any

    if (response.code === 200) {
      return {
        success: true,
        data: {
          url: response.url || response.fileName,
          fileName: response.fileName || file.name,
          size: file.size
        }
      }
    } else {
      return {
        success: false,
        error: response.msg || '上传失败'
      }
    }
  } catch (error: any) {
    return {
      success: false,
      error: error.message || '上传失败'
    }
  }
}

/**
 * 检查头像是否存在
 * @param avatarUrl - 头像URL
 * @returns 是否存在
 */
export async function checkAvatarExists(avatarUrl: string): Promise<boolean> {
  // 如果是空的或默认头像，返回 true
  if (!avatarUrl || avatarUrl.startsWith('data:')) {
    return true
  }

  try {
    const url = processAvatarUrl(avatarUrl)
    const response = await fetch(url, { method: 'HEAD' })
    return response.ok
  } catch {
    return false
  }
}
