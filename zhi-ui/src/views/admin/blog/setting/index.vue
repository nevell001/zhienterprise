<template>
  <div class="app-container">
    <el-card shadow="never" class="blog-setting-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">{{ tabTitle }}</span>
          <div class="card-extra">
            <el-button type="primary" size="small" :loading="loading" @click="saveAllSettings">
              保存所有设置
            </el-button>
            <el-button size="small" :loading="loading" @click="resetSettings">重置设置</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="blog-setting-tabs">
        <!-- 站点信息 -->
        <el-tab-pane label="站点信息" name="basic">
          <el-form ref="basicForm" :model="settingsMap" label-width="120px">
            <el-alert
              title="站点信息将在博客首页、关于页面等位置显示"
              type="info"
              :closable="false"
              class="tab-alert"
            />
            <el-form-item label="博客名称" prop="blog_name">
              <el-input
                v-model="settingsMap.blog_name"
                placeholder="请输入博客名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="博客描述" prop="blog_desc">
              <el-input
                v-model="settingsMap.blog_desc"
                type="textarea"
                :rows="3"
                placeholder="请输入博客描述"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="博客作者" prop="blog_author">
              <el-input
                v-model="settingsMap.blog_author"
                placeholder="请输入博客作者"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="版权信息" prop="blog_copyright">
              <el-input
                v-model="settingsMap.blog_copyright"
                placeholder="请输入版权信息，如：© 2025 My Blog"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="备案信息" prop="blog_beian">
              <el-input
                v-model="settingsMap.blog_beian"
                placeholder="请输入ICP备案信息，如：京ICP备12345678号"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="首页显示" prop="home_page_slug">
              <el-select
                v-model="settingsMap.home_page_slug"
                clearable
                placeholder="文章列表（默认）"
                style="width: 260px"
              >
                <el-option label="文章列表（默认）" value="" />
                <el-option
                  v-for="opt in homePageOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <div class="setting-tip">
                选择自定义页面后首页展示该页面（企业官网模式），文章列表移到 /blog/articles，
                导航出现「文章」入口；留空则首页为文章列表
              </div>
            </el-form-item>

            <el-form-item label="站点访问地址" prop="blog_url">
              <el-input
                v-model="settingsMap.blog_url"
                placeholder="https://example.com"
                maxlength="200"
                clearable
              />
              <div class="setting-tip">
                用于 RSS 订阅等生成文章绝对链接；留空时使用配置文件默认值
              </div>
            </el-form-item>

            <el-divider content-position="left">防盗链（域名白名单）</el-divider>
            <el-form-item label="防盗链" prop="referer_enabled">
              <el-switch
                v-model="settingsMap.referer_enabled"
                active-text="启用"
                inactive-text="关闭"
                @change="applySwitch('referer_enabled')"
              />
              <div class="setting-tip">
                开启后仅允许白名单域名引用 /profile 下的上传资源（图片等），防止被其他网站盗链
              </div>
            </el-form-item>
            <el-form-item label="允许域名" prop="referer_allowed_domains">
              <el-input
                v-model="settingsMap.referer_allowed_domains"
                type="textarea"
                :rows="4"
                placeholder="每行一个域名，例如：example.com&#10;blog.example.com&#10;localhost"
                maxlength="1000"
                show-word-limit
              />
              <div class="setting-tip">
                支持逗号、分号或换行分隔；留空回退到默认值 localhost,127.0.0.1；域名无需带
                http(s)://。按主机名精确匹配（填 example.com 同时覆盖其子域名），同源访问与 不带
                Referer 的请求（爬虫/直接访问）一律放行
              </div>
            </el-form-item>

            <el-divider content-position="left">监控入口</el-divider>
            <el-form-item label="Prometheus 地址" prop="prometheus_url">
              <el-input
                v-model="settingsMap.prometheus_url"
                placeholder="留空 = 站点访问地址 + :9090，例如 https://blog.example.com:9090"
                maxlength="255"
              />
            </el-form-item>
            <el-form-item label="Grafana 地址" prop="grafana_url">
              <el-input
                v-model="settingsMap.grafana_url"
                placeholder="留空 = 站点访问地址 + :3001；也可以填其它域名/反代路径"
                maxlength="255"
              />
            </el-form-item>
            <el-form-item label="Actuator 地址" prop="actuator_url">
              <el-input
                v-model="settingsMap.actuator_url"
                placeholder="留空 = 同源 /manage/actuator（跟随当前域名与反代）"
                maxlength="255"
              />
              <div class="setting-tip">
                监控页里的「访问 Prometheus / 打开 Grafana / 查看指标」都会跳转到这里配置的地址；
                留空时按站点访问地址自动推导，因此换域名无需重新构建前端。生产环境默认只把
                Prometheus / Grafana 绑定在 127.0.0.1，如需外网访问请自行反代或使用隧道。
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- SEO 优化 -->
        <el-tab-pane label="SEO优化" name="seo">
          <el-form :model="settingsMap" label-width="120px">
            <el-alert
              title="用于博客前台页面 title/description/keywords 与搜索引擎收录"
              type="info"
              :closable="false"
              class="tab-alert"
            />
            <el-form-item label="站点标题" prop="seo_title">
              <el-input
                v-model="settingsMap.seo_title"
                placeholder="如：知企建站系统"
                maxlength="100"
                show-word-limit
              />
              <div class="setting-tip">留空时前台默认使用“博客名称”作为页面标题</div>
            </el-form-item>
            <el-form-item label="站点描述" prop="seo_description">
              <el-input
                v-model="settingsMap.seo_description"
                type="textarea"
                :rows="3"
                placeholder="站点描述（meta description）"
                maxlength="300"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="关键词" prop="blog_keywords">
              <el-input
                v-model="settingsMap.blog_keywords"
                placeholder="多个关键词用英文逗号分隔"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="规范链接" prop="seo_canonical_url">
              <el-input
                v-model="settingsMap.seo_canonical_url"
                placeholder="https://example.com"
                maxlength="200"
              />
              <div class="setting-tip">规范 URL（可选），文章页 canonical 会自动生成</div>
              <div class="setting-tip">
                留空=自动使用当前访问域名；填对外域名可让 canonical/og:url 固定指向主域名
                （localhost/127.0.0.1 会被忽略）
              </div>
            </el-form-item>
            <el-form-item label="Robots规则" prop="seo_robots">
              <el-select v-model="settingsMap.seo_robots" style="width: 220px">
                <el-option v-for="r in robotsOptions" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
            <el-form-item label="站点图标" prop="seo_favicon">
              <el-input
                v-model="settingsMap.seo_favicon"
                placeholder="/favicon.ico 或图片 URL"
                maxlength="200"
              />
              <div class="setting-tip">用于前台浏览器标签页图标（rel=icon）</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 界面主题 -->
        <el-tab-pane label="界面主题" name="theme">
          <el-form ref="themeForm" :model="settingsMap" label-width="120px">
            <el-alert
              title="外观模式与布局主题记忆在当前浏览器；主题色保存后写入博客设置并作用于管理后台。"
              type="info"
              :closable="false"
              class="tab-alert"
            />

            <el-form-item label="外观模式">
              <el-radio-group
                :model-value="settingsStore.themeMode"
                class="theme-mode-group"
                @change="handleThemeModeChange"
              >
                <el-radio-button
                  v-for="option in themeModeOptions"
                  :key="option.value"
                  :data-theme-mode="option.value"
                  :label="option.value"
                >
                  {{ option.label }}
                </el-radio-button>
              </el-radio-group>
              <span class="setting-tip">「跟随系统」会随操作系统的深/浅色偏好自动切换</span>
            </el-form-item>

            <el-form-item label="应用主题">
              <el-radio-group :model-value="settingsStore.appTheme" @change="handleAppThemeChange">
                <el-radio-button
                  v-for="option in appThemeOptions"
                  :key="option.value"
                  :data-app-theme="option.value"
                  :label="option.value"
                >
                  {{ option.label }}
                </el-radio-button>
              </el-radio-group>
              <span class="setting-tip">切换后自动记住当前浏览器的后台布局主题</span>
            </el-form-item>

            <el-form-item label="主题颜色" prop="theme_color">
              <div class="theme-color-control">
                <el-color-picker
                  v-model="settingsMap.theme_color"
                  :predefine="themeColorOptions"
                  @change="handleThemeColorChange"
                />
                <el-input
                  v-model="settingsMap.theme_color"
                  class="theme-color-input"
                  placeholder="#4f46e5"
                  maxlength="20"
                  @change="handleThemeColorChange"
                />
                <span class="setting-tip">影响按钮、链接、标签页和高亮状态</span>
              </div>
            </el-form-item>

            <div class="theme-preview-grid">
              <button
                v-for="option in appThemeOptions"
                :key="option.value"
                type="button"
                class="theme-preview-card"
                :class="[{ active: settingsStore.appTheme === option.value }, option.value]"
                @click="handleAppThemeChange(option.value)"
              >
                <span class="theme-preview-title">{{ option.label }}</span>
                <span class="theme-preview-desc">{{ option.description }}</span>
                <span class="theme-preview-surface">
                  <span class="theme-preview-sidebar"></span>
                  <span class="theme-preview-content">
                    <span></span>
                    <span></span>
                    <span></span>
                  </span>
                </span>
              </button>
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 功能设置 -->
        <el-tab-pane label="功能设置" name="features">
          <div class="feature-groups">
            <section v-for="group in featureGroups" :key="group.title" class="feature-group">
              <header class="feature-group__head">
                <el-icon class="feature-group__icon"><component :is="group.icon" /></el-icon>
                <div class="feature-group__heading">
                  <h4 class="feature-group__title">{{ group.title }}</h4>
                  <p class="feature-group__desc">{{ group.desc }}</p>
                </div>
              </header>
              <div class="feature-grid">
                <div
                  v-for="item in group.items"
                  :key="item.key"
                  class="feature-card"
                  :class="{ 'is-disabled': isFeatureDisabled(item) }"
                >
                  <div class="feature-card__body">
                    <span class="feature-card__label">{{ item.label }}</span>
                    <span class="feature-card__desc">{{ item.desc }}</span>
                    <span v-if="item.instant" class="feature-card__badge">即时生效</span>
                  </div>
                  <el-switch
                    v-model="settingsMap[item.key]"
                    :data-feature="item.key"
                    :disabled="isFeatureDisabled(item)"
                    @change="applySwitch(item.key)"
                  />
                </div>
              </div>
            </section>
          </div>
        </el-tab-pane>

        <!-- 个人信息 -->
        <el-tab-pane label="个人信息" name="author">
          <el-form ref="authorForm" :model="settingsMap" label-width="120px">
            <el-alert
              title="个人信息将在关于页面和博客侧边栏显示"
              type="info"
              :closable="false"
              class="tab-alert"
            />
            <el-divider content-position="left">基本信息</el-divider>
            <el-form-item label="作者职位" prop="author_title">
              <el-input
                v-model="settingsMap.author_title"
                placeholder="请输入作者职位，如：全栈开发工程师"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="个人简介" prop="author_bio">
              <el-input
                v-model="settingsMap.author_bio"
                type="textarea"
                :rows="3"
                placeholder="请输入个人简介"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="位置信息" prop="author_location">
              <el-input
                v-model="settingsMap.author_location"
                placeholder="请输入位置信息，如：中国·北京"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <el-divider content-position="left">联系方式</el-divider>
            <el-form-item label="联系邮箱" prop="blog_email">
              <el-input
                v-model="settingsMap.blog_email"
                placeholder="请输入联系邮箱"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>

            <el-divider content-position="left">社交媒体</el-divider>
            <el-form-item label="GitHub地址" prop="github_url">
              <el-input
                v-model="settingsMap.github_url"
                placeholder="请输入GitHub地址"
                maxlength="200"
                show-word-limit
              >
                <template #prepend>
                  <el-icon><LinkIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="微博地址" prop="weibo_url">
              <el-input
                v-model="settingsMap.weibo_url"
                placeholder="请输入微博地址"
                maxlength="200"
                show-word-limit
              >
                <template #prepend>
                  <el-icon><LinkIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="个人网站" prop="personal_website">
              <el-input
                v-model="settingsMap.personal_website"
                placeholder="请输入个人网站地址"
                maxlength="200"
                show-word-limit
              >
                <template #prepend>
                  <el-icon><LinkIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 协议页面 -->
        <el-tab-pane label="协议页面" name="legal">
          <el-form :model="settingsMap" label-width="120px">
            <el-alert
              title="用户协议与隐私政策在注册页勾选处与前台全文页面（/blog/agreement、/blog/privacy）显示，支持富文本编辑"
              type="info"
              :closable="false"
              class="tab-alert"
            />
            <el-form-item label="用户协议" prop="user_agreement_content">
              <editor v-model="settingsMap.user_agreement_content" :min-height="300" />
            </el-form-item>
            <el-form-item label="隐私政策" prop="privacy_policy_content">
              <editor v-model="settingsMap.privacy_policy_content" :min-height="300" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 邮件服务 -->
        <el-tab-pane label="邮件服务" name="mail">
          <el-form :model="mailForm" label-width="140px">
            <el-alert
              title="用于注册/找回密码验证码与评论通知邮件。此处填写后覆盖环境变量配置并立即生效，无需重启；环境变量仍作为未填写时的默认值。"
              type="info"
              :closable="false"
              class="tab-alert"
            />
            <el-alert
              v-if="mailForm.devPrintCode"
              title="当前 email-code.dev-print-code=true：验证码只打印到后端控制台，不会真实发信。要收信请把该环境变量改为 false 并重启后端。"
              type="warning"
              :closable="false"
              class="tab-alert"
              data-mail="dev-print"
            />
            <el-form-item label="启用邮件服务">
              <el-switch v-model="mailForm.enabled" active-text="启用" inactive-text="关闭" />
              <div class="setting-tip">
                关闭后验证码/通知邮件不会发出（开发环境仍可能打印验证码到控制台）
              </div>
            </el-form-item>
            <el-form-item label="SMTP 主机">
              <el-input
                v-model="mailForm.host"
                placeholder="如 smtp.qq.com / smtp.gmail.com"
                maxlength="100"
                clearable
              />
            </el-form-item>
            <el-form-item label="SMTP 端口">
              <el-input-number
                v-model="mailForm.port"
                :min="1"
                :max="65535"
                controls-position="right"
              />
              <div class="setting-tip">SSL 常用 465，STARTTLS/TLS 常用 587，明文 25</div>
            </el-form-item>
            <el-form-item label="发件邮箱">
              <el-input
                v-model="mailForm.username"
                placeholder="发件邮箱账号"
                maxlength="100"
                clearable
              />
            </el-form-item>
            <el-form-item label="邮箱密码/授权码">
              <el-input
                v-model="mailForm.password"
                type="password"
                show-password
                :placeholder="mailForm.hasPassword ? '已配置，留空则不修改' : '请输入密码或授权码'"
                maxlength="200"
              />
              <div class="setting-tip">
                QQ/163 等邮箱需使用「授权码」而非登录密码；出于安全考虑密码不会回显
              </div>
            </el-form-item>
            <el-form-item label="SSL">
              <el-switch
                v-model="mailForm.ssl"
                data-mail="ssl"
                active-text="开启"
                inactive-text="关闭"
                @change="onMailSslChange"
              />
            </el-form-item>
            <el-form-item label="STARTTLS">
              <el-switch
                v-model="mailForm.starttls"
                data-mail="starttls"
                active-text="开启"
                inactive-text="关闭"
                @change="onMailStarttlsChange"
              />
              <div class="setting-tip">SSL 用于 465 端口，STARTTLS 用于 587 端口，两者互斥</div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="mailSaving" @click="handleSaveMail">
                保存并生效
              </el-button>
              <el-button :loading="mailTesting" @click="handleTestMail">测试连接</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="BlogSetting">
import { isSwitchOn } from '@/utils/featureSwitch'
import { ref, computed, onMounted, getCurrentInstance, watch } from 'vue'
import { useSettingsStore, type ThemeMode } from '@/stores/settings'
import { handleThemeStyle, type AppTheme } from '@/utils/theme'
import { ElMessage } from '@/plugins/element-plus-service'
import {
  Link as LinkIcon,
  ChatDotRound,
  Search as SearchIcon,
  Grid,
  Message,
  User
} from '@element-plus/icons-vue'
import {
  listSetting,
  updateSettingValueByKey,
  getRegistrationSwitch,
  getMailConfig,
  saveMailConfig,
  testMailConfig
} from '@/api/admin/blog/setting'
import { clearBlogCache } from '@/api/blog/setting'
import { getPublishedPages } from '@/api/blog/page'
import { useBlogSettingsStore } from '@/stores/blogSettings'

const { proxy } = getCurrentInstance()

const blogSettingsStore = useBlogSettingsStore()
const settingsStore = useSettingsStore()

const loading = ref(false)
const activeTab = ref('basic')
const settingsMap = ref<Record<string, any>>({})
const originalSettings = ref<Record<string, any>>({})

// 邮件服务（SMTP）配置：独立于通用设置，走专用接口，只写 blog_setting、不进 sys_config
const mailForm = ref({
  host: '',
  port: 465,
  username: '',
  password: '',
  ssl: true,
  starttls: false,
  enabled: true,
  hasPassword: false,
  devPrintCode: false
})
const mailSaving = ref(false)
const mailTesting = ref(false)

const appThemeOptions: { label: string; value: AppTheme; description: string }[] = [
  { label: '默认主题', value: 'default', description: '保留管理后台的默认布局和交互习惯' },
  { label: 'Mo-Blog', value: 'mo-blog', description: '与前台博客一致的低饱和靛蓝、石色背景风格' }
]

const themeModeOptions: { label: string; value: ThemeMode }[] = [
  { label: '跟随系统', value: 'system' },
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' }
]

const themeColorOptions = ['#4f46e5', '#334155', '#0f766e', '#7c3aed', '#9a3412', '#409EFF']

const robotsOptions = ['index,follow', 'noindex,follow', 'index,nofollow', 'noindex,nofollow']

/** 「首页显示」下拉：已发布的自定义页面（slug 即前台路由参数），留空 = 文章列表 */
const homePageOptions = ref<{ label: string; value: string }[]>([])

async function fetchHomePageOptions() {
  try {
    const res: any = await getPublishedPages()
    const list = Array.isArray(res?.data) ? res.data : []
    homePageOptions.value = list
      .filter((page: any) => page?.slug)
      .map((page: any) => ({ label: page.title || page.slug, value: page.slug }))
  } catch {
    homePageOptions.value = []
  }
}

/** 注册开关的参数键：真实值在 sys_config（两个注册入口都读它），blog_setting 只是镜像 */
const REGISTER_KEY = 'sys.account.registerUser'

/** 功能设置分组：每项对应一个 blog_setting 开关键，改动即落库 */
interface FeatureItem {
  key: string
  label: string
  desc: string
  dependsOn?: string
  instant?: boolean
}
const featureGroups: { title: string; desc: string; icon: any; items: FeatureItem[] }[] = [
  {
    title: '互动',
    desc: '读者与内容的互动能力',
    icon: ChatDotRound,
    items: [
      { key: 'comment_enabled', label: '评论功能', desc: '允许访客在文章下方发表评论' },
      {
        key: 'comment_review',
        label: '评论审核',
        desc: '新评论需要管理员审核后才能显示',
        dependsOn: 'comment_enabled'
      },
      { key: 'like_enabled', label: '点赞功能', desc: '允许访客为文章点赞' },
      { key: 'share_enabled', label: '分享功能', desc: '允许访客分享文章到社交媒体' },
      {
        key: 'guestbook_enabled',
        label: '留言板',
        desc: '允许访客在留言板发言；关闭后前台导航隐藏入口，且留言接口也会拒绝'
      }
    ]
  },
  {
    title: '账号',
    desc: '访客注册入口',
    icon: User,
    items: [
      {
        key: REGISTER_KEY,
        label: '用户注册',
        desc: '允许前台注册账号，同时控制后台登录页的自助注册；关闭时注册接口一律拒绝',
        instant: true
      }
    ]
  },
  {
    title: '内容与检索',
    desc: '浏览统计与站内搜索',
    icon: SearchIcon,
    items: [
      { key: 'view_count_enabled', label: '浏览统计', desc: '统计文章浏览次数' },
      { key: 'search_enabled', label: '搜索功能', desc: '启用文章搜索功能' }
    ]
  },
  {
    title: '布局与外观',
    desc: '前台页面的结构展示',
    icon: Grid,
    items: [
      { key: 'sidebar_enabled', label: '显示侧边栏', desc: '在博客首页显示侧边栏' },
      { key: 'footer_enabled', label: '显示底部', desc: '在博客页面底部显示页脚信息' },
      { key: 'copyright_enabled', label: '显示版权', desc: '在底部显示版权信息' },
      {
        key: 'beian_enabled',
        label: '显示备案',
        desc: '在底部显示 ICP 备案号（站点信息里的备案号非空才显示）'
      },
      {
        key: 'nav_category_enabled',
        label: '导航显示分类',
        desc: '关闭后前台导航与页脚隐藏「分类」入口（企业官网模式可裁剪）'
      },
      {
        key: 'nav_tag_enabled',
        label: '导航显示标签',
        desc: '关闭后前台导航与页脚隐藏「标签」入口'
      },
      {
        key: 'nav_archive_enabled',
        label: '导航显示归档',
        desc: '关闭后前台导航与页脚隐藏「归档」入口'
      }
    ]
  },
  {
    title: '友情链接',
    desc: '页脚友链列表与申请入口',
    icon: Message,
    items: [
      {
        key: 'friend_link_enabled',
        label: '页脚友链列表',
        desc: '控制页脚「友情链接」列表（已通过的友链）是否展示'
      },
      {
        key: 'friend_link_apply_enabled',
        label: '友链申请入口',
        desc: '控制页脚「友链申请」链接与申请页是否开放，关闭后提交接口也会拒绝',
        instant: true
      }
    ]
  },
  {
    title: '通知',
    desc: '评论与审核的邮件提醒',
    icon: Message,
    items: [
      {
        key: 'email_notify_enabled',
        label: '邮件通知',
        desc: '评论/回复/审核结果通过邮件通知（需已配置邮件服务）'
      }
    ]
  }
]

function isFeatureDisabled(item: FeatureItem): boolean {
  return !!item.dependsOn && !isSwitchOn(settingsMap.value[item.dependsOn])
}

const defaultSettings: Record<string, any> = {
  // 站点信息
  blog_name: '知企建站系统',
  blog_desc: '欢迎来到我的博客',
  blog_author: '博主',
  blog_copyright: '',
  blog_beian: '',
  blog_url: '',
  home_page_slug: '',
  prometheus_url: '',
  grafana_url: '',
  actuator_url: '',
  referer_enabled: false,
  referer_allowed_domains: 'localhost,127.0.0.1',
  // SEO
  seo_title: '',
  seo_description: '',
  blog_keywords: '',
  seo_canonical_url: '',
  seo_robots: 'index,follow',
  seo_favicon: '',
  // 主题
  theme_color: '#4f46e5',
  // 功能开关
  comment_enabled: true,
  comment_review: true,
  like_enabled: true,
  guestbook_enabled: false,
  view_count_enabled: true,
  share_enabled: true,
  search_enabled: true,
  sidebar_enabled: true,
  footer_enabled: true,
  copyright_enabled: true,
  beian_enabled: false,
  nav_category_enabled: true,
  nav_tag_enabled: true,
  nav_archive_enabled: true,
  friend_link_enabled: true,
  friend_link_apply_enabled: false,
  email_notify_enabled: true,
  // 用户注册（真实值读自 sys_config，默认关闭与 SQL 种子一致）
  [REGISTER_KEY]: false,
  // 个人信息
  blog_email: '',
  author_title: '',
  author_bio: '',
  author_location: '',
  github_url: '',
  weibo_url: '',
  personal_website: '',
  // 协议页面
  user_agreement_content: '',
  privacy_policy_content: ''
}

// 本页管理的 blog_setting 键集合（与 sql/00_init_database.sql 的种子一一对应，
// 由 settings-seeds.test.ts 守卫一致性；加载时也只合并这些键）
const managedKeys = [
  'blog_name',
  'blog_desc',
  'blog_author',
  'blog_copyright',
  'blog_beian',
  'blog_url',
  'home_page_slug',
  'prometheus_url',
  'grafana_url',
  'actuator_url',
  'referer_enabled',
  'referer_allowed_domains',
  'seo_title',
  'seo_description',
  'blog_keywords',
  'seo_canonical_url',
  'seo_robots',
  'seo_favicon',
  'theme_color',
  'comment_enabled',
  'comment_review',
  'like_enabled',
  'guestbook_enabled',
  'view_count_enabled',
  'share_enabled',
  'search_enabled',
  'sidebar_enabled',
  'footer_enabled',
  'copyright_enabled',
  'beian_enabled',
  'nav_category_enabled',
  'nav_tag_enabled',
  'nav_archive_enabled',
  'friend_link_enabled',
  'friend_link_apply_enabled',
  'email_notify_enabled',
  'blog_email',
  'author_title',
  'author_bio',
  'author_location',
  'github_url',
  'weibo_url',
  'personal_website',
  'user_agreement_content',
  'privacy_policy_content'
]
const managedKeySet = new Set(managedKeys)

/** 数据源在 sys_config 的开关：blog_setting 里只有它的镜像行，加载时一并合并进来 */
const sysConfigKeySet = new Set([REGISTER_KEY])

const tabTitle = computed(() => {
  const titleMap: Record<string, string> = {
    basic: '站点信息',
    theme: '界面主题',
    features: '功能设置',
    author: '个人信息',
    legal: '协议页面',
    mail: '邮件服务',
    seo: 'SEO优化'
  }
  return titleMap[activeTab.value] || '博客设置管理'
})

watch(tabTitle, newVal => {
  settingsStore.setTitle(newVal)
})

function normalizeThemeColor(value: unknown): string {
  if (typeof value === 'string' && value.trim()) {
    return value.trim()
  }
  return settingsStore.appTheme === 'mo-blog' ? '#4f46e5' : '#409EFF'
}

function handleThemeColorChange(value: unknown) {
  const nextColor = normalizeThemeColor(value)
  settingsMap.value.theme_color = nextColor
  settingsStore.changeSetting({ key: 'theme', value: nextColor })
  handleThemeStyle(nextColor)
}

function handleAppThemeChange(value: unknown) {
  const nextTheme: AppTheme = value === 'mo-blog' ? 'mo-blog' : 'default'
  settingsStore.setAppTheme(nextTheme)

  if (
    nextTheme === 'mo-blog' &&
    (!settingsMap.value.theme_color ||
      settingsMap.value.theme_color === '#409EFF' ||
      settingsMap.value.theme_color === '#409eff')
  ) {
    settingsMap.value.theme_color = '#4f46e5'
  }

  handleThemeColorChange(settingsMap.value.theme_color)
}

function handleThemeModeChange(mode: unknown) {
  const nextMode: ThemeMode = mode === 'light' || mode === 'dark' ? mode : 'system'
  settingsStore.setThemeMode(nextMode)
}

/** 将设置值转换为数据库存储用的字符串形式 */
function toStoredValue(value: any): string {
  if (value instanceof Date) {
    return value.toISOString().split('T')[0]
  }
  if (typeof value === 'boolean' || typeof value === 'number') {
    return value.toString()
  }
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
}

function toStoredMap(source: Record<string, any>): Record<string, string> {
  const result: Record<string, string> = {}
  Object.keys(source).forEach(key => {
    result[key] = toStoredValue(source[key])
  })
  return result
}

/** 将数据库字符串还原为表单需要的类型 */
function coerceValue(key: string, raw: any): any {
  if (raw === 'true') return true
  if (raw === 'false') return false
  return raw
}

async function fetchSettingRows(): Promise<any[]> {
  const res = await listSetting({ pageSize: 500, pageNum: 1 })
  return res.rows || res.data || []
}

/** 获取所有博客设置 */
async function getAllSettings() {
  loading.value = true
  try {
    let rows = await fetchSettingRows()
    if (rows.length === 0) {
      // 缓存尚未建立时重试一次
      await new Promise(resolve => setTimeout(resolve, 300))
      rows = await fetchSettingRows()
    }

    const loaded: Record<string, any> = {}
    rows.forEach((setting: any) => {
      if (setting.configKey) {
        loaded[setting.configKey] = coerceValue(setting.configKey, setting.configValue)
      }
    })

    const merged: Record<string, any> = { ...defaultSettings }
    Object.keys(loaded).forEach(key => {
      if ((managedKeySet.has(key) || sysConfigKeySet.has(key)) && loaded[key] !== undefined) {
        merged[key] = loaded[key]
      }
    })

    // 注册开关以 sys_config 的真实生效值为准：博客设置页其余项读 blog_setting，
    // 而 BlogAuthController / SysRegisterController 拦的是 sys.account.registerUser，
    // 只读本地镜像就会出现"卡片显示已开启、注册仍被拒"
    try {
      const res = await getRegistrationSwitch()
      merged[REGISTER_KEY] = !!res.data
    } catch {
      // 读不到就保留镜像值，不让整页回退到默认设置
    }

    settingsMap.value = merged
    originalSettings.value = toStoredMap(merged)
  } catch (error) {
    settingsMap.value = { ...defaultSettings }
    originalSettings.value = toStoredMap(defaultSettings)
    ElMessage.warning('获取设置失败，已使用默认设置')
  } finally {
    loading.value = false
  }
}

/** 开关类设置改动即生效：立即写入并刷新前台缓存，失败则回滚显示 */
async function applySwitch(key: string) {
  const storedValue = toStoredValue(settingsMap.value[key])
  try {
    const response = await updateSettingValueByKey(key, storedValue)
    if (response?.code === 200) {
      originalSettings.value[key] = storedValue
      ElMessage.success('设置已生效')
      blogSettingsStore.updateBlogSettings({ [key]: settingsMap.value[key] })
    } else {
      throw new Error(response?.msg || '保存失败')
    }
  } catch (error: any) {
    settingsMap.value[key] = isSwitchOn(originalSettings.value[key])
    ElMessage.error(error?.msg || error?.message || '设置保存失败')
  }
}

async function saveAllSettings() {
  loading.value = true
  try {
    const modified: { key: string; value: string }[] = []
    for (const key of Object.keys(settingsMap.value)) {
      const comparable = toStoredValue(settingsMap.value[key])
      if (comparable !== originalSettings.value[key]) {
        modified.push({ key, value: comparable })
      }
    }

    if (modified.length === 0) {
      ElMessage.success('没有修改任何设置')
      return
    }

    const failed: string[] = []
    for (const item of modified) {
      try {
        const response = await updateSettingValueByKey(item.key, item.value)
        if (response?.code !== 200) {
          failed.push(item.key)
        }
      } catch {
        failed.push(item.key)
      }
    }

    if (failed.length > 0) {
      throw new Error(`以下设置保存失败: ${failed.join(', ')}`)
    }

    // 清除前台缓存并同步全局状态
    clearBlogCache().catch(() => {})
    const latest = toStoredMap(settingsMap.value)
    blogSettingsStore.updateBlogSettings(latest)
    window.dispatchEvent(new CustomEvent('blogSettingsUpdated', { detail: latest }))

    await getAllSettings()
    ElMessage.success(`成功保存 ${modified.length} 项设置`)
  } catch (error: any) {
    ElMessage.error(`保存设置失败: ${error?.message || '请稍后重试'}`)
  } finally {
    loading.value = false
  }
}

async function resetSettings() {
  try {
    await proxy.$modal.confirm('确定要重置所有设置吗？将放弃当前未保存的修改。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    loading.value = true
    await getAllSettings()
    proxy.$modal.msgSuccess('设置已重置')
  } catch {
    // 用户取消
  } finally {
    loading.value = false
  }
}

/** 读取邮件服务配置（密码脱敏，仅告知是否已配置） */
async function fetchMailConfig() {
  try {
    const res: any = await getMailConfig()
    const view = res?.data
    if (view && typeof view === 'object') {
      mailForm.value.host = view.host ?? ''
      mailForm.value.port = Number(view.port) || 465
      mailForm.value.username = view.username ?? ''
      mailForm.value.ssl = !!view.ssl
      mailForm.value.starttls = !!view.starttls && !view.ssl
      mailForm.value.enabled = view.enabled !== false
      mailForm.value.hasPassword = !!view.hasPassword
      mailForm.value.devPrintCode = !!view.devPrintCode
      mailForm.value.password = ''
    }
  } catch {
    ElMessage.warning('获取邮件服务配置失败')
  }
}

/** SSL(465) 与 STARTTLS(587) 互斥：同时开启会导致 SMTP 握手失败，勾选一个即取消另一个 */
function onMailSslChange(value: unknown) {
  if (value) mailForm.value.starttls = false
}

function onMailStarttlsChange(value: unknown) {
  if (value) mailForm.value.ssl = false
}

async function handleSaveMail() {
  mailSaving.value = true
  try {
    const res: any = await saveMailConfig({
      host: mailForm.value.host,
      port: mailForm.value.port,
      username: mailForm.value.username,
      password: mailForm.value.password || undefined,
      ssl: mailForm.value.ssl,
      starttls: mailForm.value.starttls,
      enabled: mailForm.value.enabled
    })
    if (res?.code === 200) {
      const view = res.data || {}
      mailForm.value.hasPassword = !!view.hasPassword
      mailForm.value.password = ''
      ElMessage.success('邮件配置已保存并生效')
    } else {
      throw new Error(res?.msg || '保存失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.msg || error?.message || '邮件配置保存失败')
  } finally {
    mailSaving.value = false
  }
}

async function handleTestMail() {
  mailTesting.value = true
  try {
    const res: any = await testMailConfig({
      host: mailForm.value.host,
      port: mailForm.value.port,
      username: mailForm.value.username,
      password: mailForm.value.password || undefined,
      ssl: mailForm.value.ssl,
      starttls: mailForm.value.starttls,
      enabled: mailForm.value.enabled
    })
    if (res?.code === 200) {
      ElMessage.success(res?.msg || '连接成功')
    } else {
      ElMessage.error(res?.msg || '连接失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.msg || error?.message || '连接失败')
  } finally {
    mailTesting.value = false
  }
}

onMounted(() => {
  settingsStore.setTitle('博客设置')

  fetchMailConfig()

  fetchHomePageOptions()

  getAllSettings().then(() => {
    if (settingsMap.value.theme_color) {
      handleThemeStyle(settingsMap.value.theme_color)
    }
  })
})
</script>

<style scoped>
.app-container {
  background-color: var(--el-bg-color-page);
  min-height: calc(100vh - 84px);
}

html.dark .app-container {
  background-color: var(--el-bg-color-page, #141414);
}

.blog-setting-card {
  margin-bottom: 20px;
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
}

html.dark .blog-setting-card {
  background-color: var(--el-bg-color-overlay, #1d1e1f);
  border: 1px solid var(--el-border-color-light, #434343);
}

:deep(.el-card) {
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
}

:deep(.el-card__header) {
  background-color: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-light);
}

:deep(.el-card__body) {
  background-color: var(--el-bg-color-overlay);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: var(--el-text-color-primary, #303133);
}

.card-extra {
  display: flex;
  gap: 10px;
}

.tab-alert {
  margin-bottom: 20px;
}

.blog-setting-tabs {
  margin-top: 20px;
}

:deep(.el-tabs__header) {
  background-color: var(--el-bg-color-page, #f5f7fa);
  border-radius: 8px 8px 0 0;
  padding: 0 16px;
  margin: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  color: var(--el-text-color-regular, #606266);
  background-color: transparent;
  border: none;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 4px;
  margin-right: 4px;
  transition: all 0.3s ease;
}

:deep(.el-tabs__item:hover) {
  color: var(--el-color-primary, #409eff);
  background-color: var(--el-bg-color-overlay, #ffffff);
}

:deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary, #409eff);
  background-color: var(--el-bg-color-overlay, #ffffff);
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  display: none;
}

:deep(.el-tabs__content) {
  background-color: var(--el-bg-color-overlay, #ffffff);
  padding: 24px;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* 功能设置卡片 */
.feature-groups {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.feature-group__head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}

.feature-group__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  font-size: 18px;
  color: var(--el-color-primary, #409eff);
  background: var(--el-color-primary-light-9, #ecf5ff);
}

.feature-group__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary, #303133);
}

.feature-group__desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.feature-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light, #e4e7ed);
  border-radius: 10px;
  background: var(--el-bg-color-overlay, #ffffff);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.feature-card:hover {
  border-color: var(--el-color-primary-light-5, #a0cfff);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.feature-card.is-disabled {
  opacity: 0.6;
}

.feature-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature-card__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
}

.feature-card__desc {
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary, #909399);
}

.feature-card__badge {
  align-self: flex-start;
  margin-top: 4px;
  padding: 1px 8px;
  font-size: 11px;
  border-radius: 10px;
  color: var(--el-color-success, #67c23a);
  background: var(--el-color-success-light-9, #f0f9eb);
}

/* 主题控制 */
.theme-mode-group {
  margin-right: 12px;
}

.theme-color-control {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.theme-color-input {
  width: 140px;
}

.setting-tip {
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
  line-height: 1.5;
}

.theme-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 8px;
}

.theme-preview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  background: var(--el-bg-color-overlay, #ffffff);
  border: 1px solid var(--el-border-color-light, #e5e7eb);
  border-radius: 8px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.theme-preview-card:hover,
.theme-preview-card.active {
  border-color: var(--el-color-primary, #409eff);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.theme-preview-title {
  color: var(--el-text-color-primary, #303133);
  font-size: 14px;
  font-weight: 700;
}

.theme-preview-desc {
  min-height: 36px;
  color: var(--el-text-color-secondary, #606266);
  font-size: 12px;
  line-height: 1.5;
}

.theme-preview-surface {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 10px;
  height: 86px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.theme-preview-sidebar {
  background: #1f2937;
  border-radius: 4px;
}

.theme-preview-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.theme-preview-content span {
  display: block;
  height: 12px;
  background: #e2e8f0;
  border-radius: 4px;
}

.theme-preview-content span:first-child {
  width: 64%;
  background: var(--el-color-primary-light-7, #c7d2fe);
}

.theme-preview-card.mo-blog .theme-preview-surface {
  background: #f8f7f4;
  border-color: #dedbd2;
}

.theme-preview-card.mo-blog .theme-preview-sidebar {
  background: #2f3a4a;
}

.theme-preview-card.mo-blog .theme-preview-content span:first-child {
  background: #c7d2fe;
}

/* 表单 */
.el-form {
  padding: 0;
  background-color: transparent;
}

:deep(.el-form-item__label) {
  color: var(--el-text-color-primary, #303133);
  font-weight: 700;
}

.el-form-item {
  margin-bottom: 20px;
}

/* 暗色主题 */
html.dark .theme-preview-card {
  background: var(--el-bg-color-overlay, #1d1e1f);
  border-color: var(--el-border-color-light, #3f3f46);
}

html.dark .theme-preview-card:hover,
html.dark .theme-preview-card.active {
  border-color: var(--el-color-primary, #4f46e5);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

html.dark .theme-preview-title {
  color: var(--el-text-color-primary, #e5e8eb);
}

html.dark .theme-preview-desc {
  color: var(--el-text-color-secondary, #a3a8ad);
}

/* 默认深色主题预览：zinc 色阶不在 --mo-* 色板内，保留字面量 */
html.dark .theme-preview-surface {
  background: #27272a;
  border-color: #3f3f46;
}

/* 默认深色侧边栏预览 */
html.dark .theme-preview-sidebar {
  background: #1f2937;
}

/* 默认深色内容条预览 */
html.dark .theme-preview-content span {
  background: #3f3f46;
}

html.dark .theme-preview-content span:first-child {
  background: rgba(79, 70, 229, 0.3);
}

/* Mo-Blog 深色主题预览：用 --mo-n* 令牌（:root 作用域下解析为棕色色阶原值） */
html.dark .theme-preview-card.mo-blog .theme-preview-surface {
  background: var(--mo-n800);
  border-color: var(--mo-n700);
}

html.dark .theme-preview-card.mo-blog .theme-preview-sidebar {
  background: var(--mo-n900);
}

html.dark .theme-preview-card.mo-blog .theme-preview-content span {
  background: var(--mo-n700);
}

html.dark .theme-preview-card.mo-blog .theme-preview-content span:first-child {
  background: rgba(99, 102, 241, 0.3);
}

html.dark .feature-card {
  background: var(--el-bg-color-overlay, #1d1e1f);
  border-color: var(--el-border-color-light, #434346);
}

html.dark .feature-group__icon {
  background: rgba(79, 70, 229, 0.16);
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  :deep(.el-tabs__content) {
    padding: 16px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
