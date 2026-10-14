// 站点API统一导出文件
// 统一管理所有站点相关的API接口

// 文章相关API
export * from './article'

// 分类相关API
export * from './category'

// 标签相关API
export * from './tag'

// 评论相关API
export * from './comment'

// 友链相关API
export * from './friendLink'

// 设置相关API
export * from './setting'

// 站点API使用示例：
// import {
//   getArticleList,
//   getCategoryList,
//   getTagList,
//   getSiteSettings
// } from '@/api/site'

// 站点API模块说明：
// 1. 每个API文件包含后台管理接口和前台展示接口
// 2. 后台接口使用 '/system' 前缀，需要权限验证
// 3. 前台接口使用 '/api/api' 前缀，公开访问，避免和前台页面路由冲突
// 4. 统一错误处理和响应格式

// 站点API功能模块：
// - 文章管理：增删改查、浏览量统计、搜索、归档等
// - 分类管理：分类增删改查、文章分类关联
// - 标签管理：标签增删改查、标签云、文章标签关联
// - 评论管理：评论审核、前台评论提交
// - 友链管理：友情链接管理
// - 系统设置：站点配置、个性化设置
