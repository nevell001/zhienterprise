/**
 * 全局类型定义
 *
 * 为知企建站系统提供通用类型定义
 */

// 导出 API 模块类型
export * from './api'

/**
 * Ajax 响应结果
 */
export interface AjaxResult {
  code: number
  msg: string
  data?: any
}

/**
 * 分页信息
 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  orderByColumn?: string
  isAsc?: string | boolean
}

/**
 * 表格数据信息
 */
export interface TableDataInfo<T = any> {
  total: number
  rows: T[]
  code: number
  msg: string
}

/**
 * 树形表格数据
 */
export interface TreeSelectNode {
  id: number | string
  label: string
  children?: TreeSelectNode[]
}

/**
 * 字典数据
 */
export interface DictData {
  dictCode: string
  dictValue: string
  dictLabel: string
  dictType: string
  cssClass?: string
  listClass?: string
  isDefault?: boolean
}

/**
 * 字典类型
 */
export interface DictType {
  dictId?: number
  dictName: string
  dictType: string
  status?: string
  remark?: string
}

/**
 * 登录用户信息
 */
export interface LoginUser {
  username: string
  password: string
  code?: string
  uuid?: string
}

/**
 * 上传文件响应
 */
export interface UploadResult {
  url: string
  fileName: string
  newFileName: string
  originalFilename: string
}

/**
 * 文章数据
 */
export interface Article {
  id?: number
  title: string
  summary?: string
  content: string
  coverUrl?: string
  categoryId?: number
  categoryName?: string
  authorId?: number
  authorName?: string
  /** 内容类型：news 资讯（默认）business 核心业务 case 客户案例 */
  articleType?: string
  /** SEO 标题（留空回退文章标题） */
  seoTitle?: string
  /** SEO 关键词（英文逗号分隔） */
  seoKeywords?: string
  /** SEO 描述（留空回退文章摘要） */
  seoDescription?: string
  status?: number
  isTop?: number
  isRecommend?: number
  tagIds?: number[]
  tags?: Tag[]
  createTime?: string
  updateTime?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
}

/**
 * 标签数据
 */
export interface Tag {
  id?: number
  name: string
  description?: string
  color?: string
  icon?: string
  createTime?: string
  articleCount?: number
}

/**
 * 分类数据
 */
export interface Category {
  id?: number
  name: string
  alias?: string
  description?: string
  parentId?: number
  sortOrder?: number
  sort?: number
  status?: number
  articleCount?: number
}

/**
 * 评论数据
 */
export interface Comment {
  id?: number
  content: string
  articleId?: number
  userId?: number
  nickname?: string
  status?: number
  parentId?: number
}

/**
 * 友情链接
 */
export interface FriendLink {
  id?: number
  name: string
  url: string
  logo?: string
  description?: string
  sort?: number
  status?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
  delFlag?: string
}

/**
 * 站点设置
 */
export interface BlogSetting {
  id?: number
  settingKey: string
  settingValue: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  userId?: number
  userName: string
  nickName?: string
  email?: string
  phonenumber?: string
  sex?: string
  avatar?: string
  deptId?: number
  roles?: Role[]
  roleIds?: number[]
  postIds?: number[]
}

/**
 * 角色信息
 */
export interface Role {
  roleId?: number
  roleName: string
  roleKey?: string
  roleSort?: number
  status?: string
  remark?: string
}

/**
 * 部门信息
 */
export interface Dept {
  deptId?: number
  parentId?: number
  deptName?: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: string
  children?: Dept[]
}

/**
 * 菜单信息
 */
export interface Menu {
  menuId?: number
  menuName?: string
  parentId?: number
  orderNum?: number
  path?: string
  component?: string
  query?: string
  isFrame?: number
  isCache?: number
  menuType?: string
  visible?: string
  status?: string
  icon?: string
  children?: Menu[]
}

/**
 * 岗位信息
 */
export interface Post {
  postId?: number
  postCode?: string
  postName?: string
  postSort?: number
  status?: string
  remark?: string
}

/**
 * CPU 信息
 */
export interface CpuInfo {
  cpuNum: number
  total: number
  sys: number
  used: number
  wait: number
  free: number
}

/**
 * 内存信息
 */
export interface MemInfo {
  total: number
  used: number
  free: number
  usage: number
}

/**
 * JVM 信息
 */
export interface JvmInfo {
  total: number
  max: number
  used: number
  free: number
  usage: number
  version: string
  home: string
  name: string
  startTime: string
  runTime: string
  inputArgs: string
}

/**
 * 系统信息
 */
export interface SysInfo {
  computerName: string
  computerIp: string
  osName: string
  osArch: string
  userDir: string
}

/**
 * 系统文件信息
 */
export interface SysFileInfo {
  dirName: string
  sysTypeName: string
  typeName: string
  total: string
  free: string
  used: string
  usage: number
}

/**
 * 服务器监控信息
 */
export interface ServerInfo {
  cpu: CpuInfo
  mem: MemInfo
  jvm: JvmInfo
  sys: SysInfo
  sysFiles: SysFileInfo[]
}
