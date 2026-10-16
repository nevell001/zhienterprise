/**
 * API 模块类型定义
 */

/**
 * 分页查询参数
 */
export interface PageParams {
  pageNum?: number
  pageSize?: number
  [key: string]: any
}

/**
 * 删除参数
 */
export interface DeleteParams {
  id?: number | number[]
}

/**
 * 状态变更参数
 */
export interface StatusParams {
  id?: number | number[]
  status: number
}

/**
 * 文章查询参数
 */
export interface ArticleParams extends PageParams {
  title?: string
  categoryId?: number
  status?: number
  isTop?: number
  tagId?: number
  articleType?: string
  /** 所属行业（solution 筛选） */
  industry?: string
  /** 能力场景（solution 筛选，模糊匹配） */
  scenario?: string
}

/**
 * 用户查询参数
 */
export interface UserParams extends PageParams {
  userName?: string
  phonenumber?: string
  status?: string
  deptId?: number
  roleId?: number
}

/**
 * 角色查询参数
 */
export interface RoleParams extends PageParams {
  roleName?: string
  roleKey?: string
  status?: string
}

/**
 * 部门查询参数
 */
export interface DeptParams extends PageParams {
  deptName?: string
  status?: string
  parentId?: number
}

/**
 * 菜单查询参数
 */
export interface MenuParams extends PageParams {
  menuName?: string
  status?: string
  menuType?: string
}

/**
 * 字典查询参数
 */
export interface DictParams extends PageParams {
  dictName?: string
  dictType?: string
  status?: string
}

/**
 * 岗位查询参数
 */
export interface PostParams extends PageParams {
  postCode?: string
  postName?: string
  status?: string
}

/**
 * 标签查询参数
 */
export interface TagParams extends PageParams {
  name?: string
  status?: string
}

/**
 * 通用查询响应
 */
export interface QueryResult<T = any> {
  code: number
  msg: string
  rows: T[]
  data?: T[]
  total: number
}

export interface DataResult<T = any> {
  code?: number
  msg?: string
  data: T
}

/**
 * 通用操作响应
 */
export interface OperResult {
  code: number
  msg: string
}

/**
 * 文章详情响应（包含上下篇文章）
 */
export interface ArticleDetailResponse {
  article: Article
  prevArticle?: Article | null
  nextArticle?: Article | null
  extraInfo?: {
    prevArticle?: Article | null
    nextArticle?: Article | null
    category?: {
      id?: number
      name?: string
    } | null
  } | null
}

/**
 * 文章归档响应
 */
export interface ArticleArchive {
  date: string // YYYY-MM 格式
  count: number
  articles?: Article[]
}

/**
 * 浏览量更新响应
 */
export interface ViewCountResponse {
  success: boolean
  viewCount?: number
}

/**
 * 评论提交响应
 */
export interface CommentSubmitResponse {
  code: number
  msg: string
  data?: {
    id?: number
    status?: number
  }
}

/**
 * 导入 Article 类型避免重复
 */
import type { Article } from '../types'

// 重新导出 Article 以便其他模块使用
export type { Article } from '../types'
