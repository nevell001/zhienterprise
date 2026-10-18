<template>
  <div class="app-container">
    <el-card shadow="never" class="site-setting-card">
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

      <el-tabs v-model="activeTab" class="site-setting-tabs">
        <!-- 站点信息 -->
        <el-tab-pane label="站点信息" name="basic">
          <el-form ref="basicForm" :model="settingsMap" label-width="120px">
            <el-alert
              title="站点信息将在站点首页、关于页面等位置显示"
              type="info"
              :closable="false"
              class="tab-alert"
            />
            <el-form-item label="站点名称" prop="blog_name">
              <el-input
                v-model="settingsMap.blog_name"
                placeholder="请输入站点名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="站点描述" prop="blog_desc">
              <el-input
                v-model="settingsMap.blog_desc"
                type="textarea"
                :rows="3"
                placeholder="请输入站点描述"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            <el-divider content-position="left">品牌图（自定义 Logo / 首页 Banner）</el-divider>
            <el-form-item label="站点 Logo" prop="site_logo">
              <div class="brand-upload">
                <ImageUpload
                  v-model="settingsMap.site_logo"
                  action="/common/upload"
                  :limit="1"
                  :file-size="2"
                  :is-show-tip="false"
                />
                <p class="brand-upload-tip">
                  显示在前台导航、登录/注册/找回密码页与结构化数据中；建议透明背景
                  PNG/SVG，留空则回退上方站点头像与站名首字。
                </p>
              </div>
            </el-form-item>
            <el-form-item label="首页 Banner" prop="site_banner">
              <div class="brand-upload">
                <ImageUpload
                  v-model="settingsMap.site_banner"
                  action="/common/upload"
                  crop
                  :aspect-ratio="16 / 9"
                  :limit="5"
                  :multiple="false"
                  :file-size="5"
                  :is-show-tip="false"
                />
                <p class="brand-upload-tip">
                  用于「企业官网模板」首页 Hero 视觉区（替换默认母题插画）：最多 5 张、建议
                  16:9，按上传顺序自动轮播（悬停暂停、可拖动排序）；只传 1
                  张时直接展示不轮播，留空则显示默认母题。
                </p>
              </div>
            </el-form-item>
            <el-form-item label="版权信息" prop="blog_copyright">
              <el-input
                v-model="settingsMap.blog_copyright"
                placeholder="请输入版权信息，如：© 2025 My Site"
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
            <el-divider content-position="left">联系信息（企业官网模板展示）</el-divider>
            <el-form-item label="联系邮箱" prop="contact_email">
              <el-input
                v-model="settingsMap.contact_email"
                placeholder="如：contact@example.com"
                maxlength="100"
                clearable
                style="max-width: 260px"
              />
            </el-form-item>
            <el-form-item label="联系电话" prop="contact_phone">
              <el-input
                v-model="settingsMap.contact_phone"
                placeholder="如：400-000-0000"
                maxlength="30"
                clearable
                style="max-width: 260px"
              />
            </el-form-item>
            <el-form-item label="联系地址" prop="contact_address">
              <el-input
                v-model="settingsMap.contact_address"
                placeholder="企业办公地址"
                maxlength="120"
                clearable
              />
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
              title="用于前台页面 title/description/keywords 与搜索引擎收录"
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
              <div class="setting-tip">留空时前台默认使用“站点名称”作为页面标题</div>
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
              title="外观模式记忆在当前浏览器；主题色与官网首页设置保存后写入站点设置并全站生效。"
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
                <span class="setting-tip">
                  影响后台与弹层；企业官网前台的按钮/链接/高亮以「行业模板」主色为准
                </span>
              </div>
            </el-form-item>

            <el-divider content-position="left">官网首页（企业官网模板）</el-divider>
            <el-form-item label="首页显示" prop="home_page_slug">
              <el-select
                v-model="settingsMap.home_page_slug"
                clearable
                placeholder="企业官网模板首页（默认）"
                style="width: 260px"
              >
                <el-option label="企业官网模板首页（默认）" value="" />
                <el-option
                  v-for="opt in homePageOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <div class="setting-tip">
                选择自定义页面后首页整体展示该页面（如落地页/活动页）；留空则首页为企业官网
                区块化模板（Hero / 核心业务 / 新闻动态等区块）
              </div>
            </el-form-item>
            <el-form-item label="栏目名称" prop="business_label">
              <el-input
                v-model="settingsMap.business_label"
                placeholder="核心业务"
                maxlength="20"
                show-word-limit
                style="max-width: 260px"
              />
              <div class="setting-tip">
                导航与首页区块的核心业务栏目名称，可按企业形态改为：产品中心 / 服务项目 / 解决方案
              </div>
            </el-form-item>
            <el-form-item label="行业模板" prop="site_template">
              <el-select v-model="settingsMap.site_template" style="width: 320px">
                <el-option v-for="t in siteTemplates" :key="t.id" :value="t.id" :label="t.name">
                  <span style="display: inline-flex; align-items: center; gap: 8px">
                    <span
                      :style="{
                        width: '10px',
                        height: '10px',
                        borderRadius: '999px',
                        background: t.color,
                        display: 'inline-block',
                        flex: 'none'
                      }"
                    ></span>
                    {{ t.name }} · {{ t.industry }}
                  </span>
                </el-option>
              </el-select>
              <div class="setting-tip">
                企业官网的行业形态：主色 / 圆角 / 母题主视觉 / 文案随模板整体切换（管理咨询 /
                信息技术 / 制造业 / 商贸 / 数码产品），保存后前台全部页面（含表单、分页等 Element
                组件）即时生效，后台不受影响
              </div>
              <div class="cfg-row" style="margin-top: 8px">
                <el-button
                  v-hasPermi="['site:nav:edit']"
                  type="primary"
                  plain
                  :loading="packApplying"
                  @click="applyTemplatePack"
                >
                  应用模板包（覆盖首页内容与导航）
                </el-button>
                <span class="setting-tip" style="margin: 0">
                  将把该行业的首页模块内容、头部/页脚导航与栏目名称导入为站点数据（覆盖当前值）
                </span>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 官网内容（结构化维护企业官网各页面的展示数据） -->
        <el-tab-pane label="官网内容" name="content">
          <el-form :model="settingsMap" label-width="90px">
            <el-alert
              title="以下内容维护企业官网各页面的展示数据。按标签分组维护：首页内容 / 荣誉与历程 / 联系与问答 / 页面文案。修改后请保存；清空全部行时前台显示内置示例，也可按分区「恢复默认」。"
              type="info"
              :closable="false"
              class="tab-alert"
            />

            <el-tabs v-model="contentTab" class="content-tabs">
              <el-tab-pane label="首页内容" name="home">
                <div class="content-source-tip">
                  <el-icon><InfoFilled /></el-icon>
                  <span>
                    首页 Hero 的眉题 / 标题 / 副标题 / 按钮在
                    <router-link to="/admin/page">页面管理 → 首页</router-link>
                    维护；本页维护指标、成果、方案等内容数据。年限数字只写在「信任指标」里，
                    模板默认文案不再写死，避免两边不一致。
                  </span>
                </div>

                <ContentSectionCard
                  title="首页 · 信任指标条"
                  desc="首页 Hero 下方的数字指标，建议 2–4 项"
                  :icon="TrendCharts"
                  :count="settingsMap.site_metrics?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('site_metrics')"
                    >
                      恢复默认
                    </el-button>
                  </template>
                  <ContentFieldsEditor
                    :items="settingsMap.site_metrics"
                    :fields="METRIC_FIELDS"
                    add-label="添加指标"
                    @add="addContentRow('site_metrics', { value: '', label: '' })"
                    @remove="i => removeContentRow('site_metrics', i)"
                    @move="(i, d) => moveContentRow('site_metrics', i, d)"
                  />
                </ContentSectionCard>

                <ContentSectionCard
                  title="首页 · 数据成果"
                  desc="深色数据区块里的成果数字，留空则前台回退内置示例"
                  :icon="DataAnalysis"
                  :count="settingsMap.site_outcomes?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('site_outcomes')"
                    >
                      恢复默认
                    </el-button>
                  </template>
                  <ContentFieldsEditor
                    :items="settingsMap.site_outcomes"
                    :fields="OUTCOME_FIELDS"
                    add-label="添加成果"
                    @add="addContentRow('site_outcomes', { mark: '', suffix: '', label: '' })"
                    @remove="i => removeContentRow('site_outcomes', i)"
                    @move="(i, d) => moveContentRow('site_outcomes', i, d)"
                  />
                </ContentSectionCard>

                <ContentSectionCard
                  title="首页 · 解决方案精选"
                  desc="首页推荐的核心业务 / 解决方案卡片"
                  :icon="Grid"
                  :count="settingsMap.home_solutions?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('home_solutions')"
                    >
                      恢复默认
                    </el-button>
                  </template>
                  <ContentFieldsEditor
                    :items="settingsMap.home_solutions"
                    :fields="SOLUTION_FIELDS"
                    add-label="添加方案"
                    @add="addContentRow('home_solutions', { title: '', desc: '' })"
                    @remove="i => removeContentRow('home_solutions', i)"
                    @move="(i, d) => moveContentRow('home_solutions', i, d)"
                  />
                </ContentSectionCard>
              </el-tab-pane>

              <el-tab-pane label="荣誉与历程" name="trust">
                <ContentSectionCard
                  title="荣誉资质墙"
                  desc="关于页 / 荣誉页展示的证书与奖项"
                  :icon="Trophy"
                  :count="settingsMap.site_honors?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('site_honors')"
                    >
                      恢复默认
                    </el-button>
                  </template>
                  <ContentFieldsEditor
                    :items="settingsMap.site_honors"
                    :fields="HONOR_FIELDS"
                    add-label="添加荣誉"
                    @add="
                      addContentRow('site_honors', {
                        name: '',
                        issuer: '',
                        year: '',
                        cat: '资质证书'
                      })
                    "
                    @remove="i => removeContentRow('site_honors', i)"
                    @move="(i, d) => moveContentRow('site_honors', i, d)"
                  />
                </ContentSectionCard>

                <ContentSectionCard
                  title="发展历程时间轴"
                  desc="关于页时间轴：阶段 + 事件，按时间顺序排列"
                  :icon="Clock"
                  :count="settingsMap.site_history?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('site_history')"
                    >
                      恢复默认
                    </el-button>
                  </template>

                  <div
                    v-for="(st, si) in settingsMap.site_history || []"
                    :key="`stage-${si}`"
                    class="history-stage"
                  >
                    <div class="history-stage__head">
                      <div class="history-stage__fields">
                        <label class="content-field">
                          <span class="content-field__label">年份区间</span>
                          <el-input v-model="st.range" placeholder="如 2011 - 2014" />
                        </label>
                        <label class="content-field">
                          <span class="content-field__label">阶段名</span>
                          <el-input v-model="st.name" placeholder="如 初创期" />
                        </label>
                        <label class="content-field content-field--grow">
                          <span class="content-field__label">阶段主题</span>
                          <el-input v-model="st.theme" placeholder="一句话概括这一阶段" />
                        </label>
                      </div>
                      <div class="content-ops">
                        <el-button
                          link
                          :disabled="si === 0"
                          title="上移阶段"
                          aria-label="上移阶段"
                          @click="moveContentRow('site_history', si, -1)"
                        >
                          <el-icon><Top /></el-icon>
                        </el-button>
                        <el-button
                          link
                          :disabled="si === (settingsMap.site_history?.length ?? 1) - 1"
                          title="下移阶段"
                          aria-label="下移阶段"
                          @click="moveContentRow('site_history', si, 1)"
                        >
                          <el-icon><Bottom /></el-icon>
                        </el-button>
                        <el-button
                          link
                          type="danger"
                          title="删除阶段"
                          aria-label="删除阶段"
                          @click="removeContentRow('site_history', si)"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                    </div>

                    <div class="history-events">
                      <div
                        v-for="(ev, ei) in st.events"
                        :key="`event-${si}-${ei}`"
                        class="history-event"
                      >
                        <label class="content-field history-event__year">
                          <span class="content-field__label">年份</span>
                          <el-input v-model="ev.year" placeholder="如 2013" />
                        </label>
                        <label class="content-field history-event__title">
                          <span class="content-field__label">事件标题</span>
                          <el-input v-model="ev.title" placeholder="关键里程碑" />
                        </label>
                        <label class="content-field content-field--grow">
                          <span class="content-field__label">事件描述</span>
                          <el-input v-model="ev.desc" placeholder="一句话说明" />
                        </label>
                        <el-checkbox v-model="ev.honor" class="history-event__honor">
                          荣誉节点
                        </el-checkbox>
                        <el-button
                          link
                          type="danger"
                          title="删除事件"
                          aria-label="删除事件"
                          @click="removeHistoryEvent(si, ei)"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </div>
                      <el-button size="small" text type="primary" @click="addHistoryEvent(si)">
                        + 添加事件
                      </el-button>
                    </div>
                  </div>

                  <p v-if="!settingsMap.site_history?.length" class="content-empty">
                    暂无阶段，前台会显示内置示例。
                  </p>
                  <el-button plain :icon="Plus" @click="addHistoryStage()">添加阶段</el-button>
                </ContentSectionCard>
              </el-tab-pane>

              <el-tab-pane label="联系与问答" name="contact">
                <ContentSectionCard
                  title="联系我们 · 分渠道咨询"
                  desc="联系页的电话 / 微信 / 邮箱等咨询入口"
                  :icon="ChatDotRound"
                  :count="settingsMap.site_channels?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('site_channels')"
                    >
                      恢复默认
                    </el-button>
                  </template>
                  <ContentFieldsEditor
                    :items="settingsMap.site_channels"
                    :fields="CHANNEL_FIELDS"
                    add-label="添加渠道"
                    @add="addContentRow('site_channels', { name: '', desc: '' })"
                    @remove="i => removeContentRow('site_channels', i)"
                    @move="(i, d) => moveContentRow('site_channels', i, d)"
                  />
                </ContentSectionCard>

                <ContentSectionCard
                  title="联系我们 · 常见问题"
                  desc="同步输出 FAQPage 结构化数据，利于搜索收录"
                  :icon="ChatLineSquare"
                  :count="settingsMap.site_faq?.length ?? 0"
                >
                  <template #actions>
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="resetContentSection('site_faq')"
                    >
                      恢复默认
                    </el-button>
                  </template>
                  <ContentFieldsEditor
                    :items="settingsMap.site_faq"
                    :fields="FAQ_FIELDS"
                    add-label="添加问题"
                    @add="addContentRow('site_faq', { q: '', a: '' })"
                    @remove="i => removeContentRow('site_faq', i)"
                    @move="(i, d) => moveContentRow('site_faq', i, d)"
                  />
                </ContentSectionCard>
              </el-tab-pane>

              <el-tab-pane label="页面文案" name="copy">
                <ContentSectionCard
                  title="模板页头文案"
                  desc="各页面的眉题 / 标题 / 副题；留空跟随默认（核心业务页跟随栏目名称，关于页跟随站点名称/描述）"
                  :icon="Document"
                  :count="Object.keys(settingsMap.site_page_copy || {}).length"
                >
                  <template #actions>
                    <el-button link type="primary" size="small" @click="resetPageCopy">
                      恢复默认
                    </el-button>
                  </template>
                  <div
                    v-for="(pc, pk) in settingsMap.site_page_copy || {}"
                    :key="pk"
                    class="page-copy-row"
                  >
                    <span class="page-copy-row__name">{{ PAGE_COPY_LABELS[pk] || pk }}</span>
                    <div class="page-copy-row__fields">
                      <label class="content-field">
                        <span class="content-field__label">眉题</span>
                        <el-input
                          v-model="pc.eyebrow"
                          :placeholder="`默认：${DEFAULT_PAGE_COPY[pk]?.eyebrow ?? '跟随页面'}`"
                        />
                      </label>
                      <label class="content-field">
                        <span class="content-field__label">标题</span>
                        <el-input
                          v-model="pc.title"
                          :placeholder="`默认：${DEFAULT_PAGE_COPY[pk]?.title ?? '跟随页面'}`"
                        />
                      </label>
                      <label class="content-field content-field--grow">
                        <span class="content-field__label">副题</span>
                        <el-input v-model="pc.subtitle" placeholder="留空使用默认文案" />
                      </label>
                    </div>
                  </div>
                </ContentSectionCard>
              </el-tab-pane>
            </el-tabs>
          </el-form>
        </el-tab-pane>

        <!-- 功能设置 -->
        <el-tab-pane label="功能设置" name="features">
          <div class="feature-groups">
            <section v-for="group in visibleFeatureGroups" :key="group.title" class="feature-group">
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

        <!-- 协议页面 -->
        <el-tab-pane label="协议页面" name="legal">
          <el-form :model="settingsMap" label-width="120px">
            <el-alert
              title="用户协议与隐私政策在注册页勾选处与前台全文页面（/agreement、/privacy）显示，支持富文本编辑"
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

<script setup lang="ts" name="SiteSetting">
import { isSwitchOn } from '@/utils/featureSwitch'
import { ref, computed, onMounted, getCurrentInstance, watch } from 'vue'
import { useSettingsStore, type ThemeMode } from '@/stores/settings'
import { handleThemeStyle } from '@/utils/theme'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import {
  Link as LinkIcon,
  ChatDotRound,
  Search as SearchIcon,
  Grid,
  Message,
  User,
  TrendCharts,
  DataAnalysis,
  Trophy,
  Clock,
  ChatLineSquare,
  Document,
  Bottom,
  Delete,
  Plus,
  Top,
  InfoFilled
} from '@element-plus/icons-vue'
import {
  listSetting,
  updateSettingValueByKey,
  getRegistrationSwitch,
  getMailConfig,
  saveMailConfig,
  testMailConfig
} from '@/api/admin/setting'
import ImageUpload from '@/components/ImageUpload'
import ContentSectionCard from './components/ContentSectionCard.vue'
import ContentFieldsEditor from './components/ContentFieldsEditor.vue'
import {
  METRIC_FIELDS,
  OUTCOME_FIELDS,
  SOLUTION_FIELDS,
  HONOR_FIELDS,
  CHANNEL_FIELDS,
  FAQ_FIELDS
} from './contentFields'
import { normalizeUploadUrl } from '@/utils/uploadUrl'
import { clearSiteCache } from '@/api/site/setting'
import { getPublishedPages } from '@/api/site/page'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import {
  SITE_TEMPLATES,
  SITE_METRICS,
  SITE_OUTCOMES,
  SOLUTION_SHOWCASE
} from '@/config/siteTemplates'
import { getTemplatePack } from '@/config/templatePacks'
import { listPage, updatePage } from '@/api/admin/page'
import { importNav } from '@/api/admin/nav'
import {
  DEFAULT_PAGE_COPY,
  DEFAULT_CHANNELS,
  DEFAULT_FAQS,
  DEFAULT_HONORS,
  DEFAULT_STAGES
} from '@/utils/siteContent'

const siteTemplates = SITE_TEMPLATES
const packApplying = ref(false)

/** 应用行业模板包：覆盖首页模块内容 + 头部/页脚导航 + 栏目名（确认后执行） */
async function applyTemplatePack() {
  const pack = getTemplatePack(settingsMap.value.site_template)
  try {
    await ElMessageBox.confirm(
      `应用「${pack.name}」模板包将覆盖首页模块内容、头部/页脚导航与栏目名称（当前内容不保留），确定继续？`,
      '应用模板包',
      { confirmButtonText: '应用', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  packApplying.value = true
  try {
    // 1. 首页 builder 页：覆盖 sections
    const res = await listPage({ pageNum: 1, pageSize: 50, slug: 'home' })
    const homeRow = (res.rows || res.data || []).find((p: any) => p.slug === 'home')
    if (homeRow?.id) {
      await updatePage({
        ...homeRow,
        sections: JSON.stringify(pack.homeSections),
        pageType: 'builder',
        status: '1'
      } as any)
    } else {
      proxy?.$modal?.msgWarning('未找到首页（slug=home），请先重跑初始化脚本')
      return
    }
    // 2. 导航（头部/页脚，事务内重建）
    await Promise.all([importNav('header', pack.headerNav), importNav('footer', pack.footerNav)])
    // 3. 栏目名称
    await updateSettingValueByKey('business_label', pack.settings.business_label)

    siteSettingsStore.updateSiteSettings({
      business_label: pack.settings.business_label
    })
    proxy?.$modal?.msgSuccess(`已应用「${pack.name}」模板包`)
  } finally {
    packApplying.value = false
  }
}

/** 官网内容各键的默认结构化数据（JSON 键损坏/缺失时的还原源） */
const defaultSiteContent: Record<string, any[]> = {
  site_metrics: SITE_METRICS.map(m => ({ ...m })),
  site_outcomes: SITE_OUTCOMES.map(o => ({ ...o })),
  home_solutions: SOLUTION_SHOWCASE.map(s => ({ ...s })),
  site_channels: DEFAULT_CHANNELS.map(c => ({ ...c })),
  site_faq: DEFAULT_FAQS.map(f => ({ ...f })),
  site_honors: DEFAULT_HONORS.map(h => ({ ...h })),
  site_history: DEFAULT_STAGES.map(s => ({ ...s, events: s.events.map(e => ({ ...e })) }))
}

/** 页面文案编辑态结构：空串 = 未设置（前台跟随站点设置与内置文案） */
function defaultSitePageCopy(): Record<
  string,
  { eyebrow: string; title: string; subtitle: string }
> {
  return Object.fromEntries(
    Object.keys(DEFAULT_PAGE_COPY).map(key => [key, { eyebrow: '', title: '', subtitle: '' }])
  )
}

/** 页面文案编辑器各页中文名 */
const PAGE_COPY_LABELS: Record<string, string> = {
  business: '核心业务页',
  contact: '联系页',
  about: '关于页',
  history: '历程页',
  honors: '荣誉页'
}

const { proxy } = getCurrentInstance()

const siteSettingsStore = useSiteSettingsStore()
const settingsStore = useSettingsStore()

const loading = ref(false)
const activeTab = ref('basic')
/** 官网内容子标签：首页内容 / 荣誉与历程 / 联系与问答 / 页面文案 */
const contentTab = ref('home')
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
      {
        key: 'comment_enabled',
        label: '评论功能',
        desc: '允许访客在文章下方发表评论（企业官网默认关闭）'
      },
      {
        key: 'comment_review',
        label: '评论审核',
        desc: '新评论需要管理员审核后才能显示',
        dependsOn: 'comment_enabled'
      },
      {
        key: 'guestbook_enabled',
        label: '留言板',
        desc: '允许访客在留言板发言；关闭后前台导航隐藏入口，且留言接口也会拒绝'
      },
      {
        key: 'contact_form_enabled',
        label: '咨询表单',
        desc: '联系我们页的咨询表单（写入「留言管理」，与留言板共用审核与提交冷却）'
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
      { key: 'footer_enabled', label: '显示底部', desc: '在站点页面底部显示页脚信息' },
      { key: 'copyright_enabled', label: '显示版权', desc: '在底部显示版权信息' },
      {
        key: 'beian_enabled',
        label: '显示备案',
        desc: '在底部显示 ICP 备案号（站点信息里的备案号非空才显示）'
      },
      {
        key: 'nav_category_enabled',
        label: '导航显示分类',
        desc: '关闭后顶部导航与页脚隐藏「分类」入口'
      },
      {
        key: 'nav_tag_enabled',
        label: '导航显示标签',
        desc: '关闭后前台导航与页脚隐藏「标签」入口'
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

/** 企业官网模式（与 SiteLayout 的 isBusinessNavEnabled 同口径）：导航/页脚改由「菜单管理」表驱动 */
const isEnterpriseMode = computed(() => String(settingsMap.value.home_layout || '') === 'business')

/** 这两个开关只被经典模式的 buildNavMenus / 经典页脚消费，企业模式下完全不参与 */
const ENTERPRISE_INERT_KEYS = new Set(['nav_category_enabled', 'nav_tag_enabled'])

/** 企业模式下隐藏失效开关，避免出现「关了分类，前台导航纹丝不动」的假编辑 */
const visibleFeatureGroups = computed(() =>
  isEnterpriseMode.value
    ? featureGroups
        .map(group => ({
          ...group,
          items: group.items.filter(item => !ENTERPRISE_INERT_KEYS.has(item.key))
        }))
        .filter(group => group.items.length > 0)
    : featureGroups
)

const defaultSettings: Record<string, any> = {
  // 站点信息
  blog_name: '知企建站系统',
  blog_desc: '欢迎来到我的站点',
  site_logo: '',
  site_banner: '',
  blog_copyright: '',
  blog_beian: '',
  blog_url: '',
  home_page_slug: '',
  home_layout: 'business',
  site_metrics: structuredClone(defaultSiteContent.site_metrics),
  site_outcomes: structuredClone(defaultSiteContent.site_outcomes),
  home_solutions: structuredClone(defaultSiteContent.home_solutions),
  site_channels: structuredClone(defaultSiteContent.site_channels),
  site_faq: structuredClone(defaultSiteContent.site_faq),
  site_honors: structuredClone(defaultSiteContent.site_honors),
  site_history: structuredClone(defaultSiteContent.site_history),
  site_page_copy: defaultSitePageCopy(),
  business_label: '核心业务',
  contact_email: '',
  contact_phone: '',
  contact_address: '',
  site_template: 'consult',
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
  comment_enabled: false,
  comment_review: true,
  guestbook_enabled: false,
  contact_form_enabled: true,
  view_count_enabled: true,
  search_enabled: true,
  footer_enabled: true,
  copyright_enabled: true,
  beian_enabled: false,
  nav_category_enabled: true,
  nav_tag_enabled: true,
  friend_link_enabled: true,
  friend_link_apply_enabled: false,
  email_notify_enabled: true,
  // 用户注册（真实值读自 sys_config，默认关闭与 SQL 种子一致）
  [REGISTER_KEY]: false,
  // 协议页面
  user_agreement_content: '',
  privacy_policy_content: ''
}

// 本页管理的 blog_setting 键集合（与 sql/00_init_database.sql 的种子一一对应，
// 由 settings-seeds.test.ts 守卫一致性；加载时也只合并这些键）
const managedKeys = [
  'blog_name',
  'blog_desc',
  'site_logo',
  'site_banner',
  'blog_copyright',
  'blog_beian',
  'blog_url',
  'home_page_slug',
  'home_layout',
  'business_label',
  'contact_email',
  'contact_phone',
  'contact_address',
  'site_template',
  'site_metrics',
  'site_outcomes',
  'home_solutions',
  'site_channels',
  'site_faq',
  'site_honors',
  'site_history',
  'site_page_copy',
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
  'guestbook_enabled',
  'contact_form_enabled',
  'view_count_enabled',
  'search_enabled',
  'footer_enabled',
  'copyright_enabled',
  'beian_enabled',
  'nav_category_enabled',
  'nav_tag_enabled',
  'friend_link_enabled',
  'friend_link_apply_enabled',
  'email_notify_enabled',
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
  return titleMap[activeTab.value] || '站点设置管理'
})

watch(tabTitle, newVal => {
  settingsStore.setTitle(newVal)
})

function normalizeThemeColor(value: unknown): string {
  if (typeof value === 'string' && value.trim()) {
    return value.trim()
  }
  // Mo-Blog 已移除，默认主题回退 Element 默认蓝
  return '#409EFF'
}

function handleThemeColorChange(value: unknown) {
  const nextColor = normalizeThemeColor(value)
  settingsMap.value.theme_color = nextColor
  settingsStore.changeSetting({ key: 'theme', value: nextColor })
  handleThemeStyle(nextColor)
}

function handleThemeModeChange(mode: unknown) {
  const nextMode: ThemeMode = mode === 'light' || mode === 'dark' ? mode : 'system'
  settingsStore.setThemeMode(nextMode)
}

/** 官网内容行操作：添加/删除（清空全部行时前台回退内置示例） */
function addContentRow(key: string, tpl: Record<string, any>) {
  const list = Array.isArray(settingsMap.value[key]) ? [...settingsMap.value[key]] : []
  list.push({ ...tpl })
  settingsMap.value[key] = list
}

function removeContentRow(key: string, index: number | string) {
  const idx = Number(index)
  settingsMap.value[key] = (settingsMap.value[key] || []).filter((_: any, i: number) => i !== idx)
}

/** 上移/下移一行（保持数组顺序 = 前台展示顺序） */
function moveContentRow(key: string, index: number | string, delta: number) {
  const list = settingsMap.value[key]
  if (!Array.isArray(list)) return
  const from = Number(index)
  const target = from + delta
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(from, 1)
  list.splice(target, 0, item)
}

/** 恢复某个内容分区为内置默认数据（只改本地编辑态，保存后生效） */
function resetContentSection(key: string) {
  const defaults = defaultSiteContent[key]
  if (!defaults) return
  settingsMap.value[key] = structuredClone(defaults)
}

/** 恢复页面文案编辑态：全部清空 = 前台跟随站点设置与内置文案 */
function resetPageCopy() {
  settingsMap.value.site_page_copy = defaultSitePageCopy()
}

function addHistoryStage() {
  addContentRow('site_history', { range: '', name: '', theme: '', events: [] })
}

function addHistoryEvent(si: number | string) {
  const stageIdx = Number(si)
  const stages = [...settingsMap.value.site_history]
  stages[stageIdx] = {
    ...stages[stageIdx],
    events: [...(stages[stageIdx].events || []), { year: '', title: '', desc: '', honor: false }]
  }
  settingsMap.value.site_history = stages
}

function removeHistoryEvent(si: number | string, ei: number | string) {
  const stageIdx = Number(si)
  const eventIdx = Number(ei)
  const stages = [...settingsMap.value.site_history]
  stages[stageIdx] = {
    ...stages[stageIdx],
    events: (stages[stageIdx].events || []).filter((_: any, i: number) => i !== eventIdx)
  }
  settingsMap.value.site_history = stages
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
  // 官网内容结构化数组（site_metrics 等）序列化为 JSON 存储
  if (Array.isArray(value) || typeof value === 'object') {
    return JSON.stringify(value)
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
/** 官网内容 JSON 键（编辑态为结构化数组，存储态为 JSON 字符串） */
const CONTENT_JSON_KEYS = new Set([
  'site_metrics',
  'site_outcomes',
  'home_solutions',
  'site_channels',
  'site_faq',
  'site_honors',
  'site_history'
])

function coerceValue(key: string, raw: any): any {
  if (CONTENT_JSON_KEYS.has(key)) {
    const fallback = defaultSiteContent[key] ?? []
    if (typeof raw !== 'string' || !raw.trim()) {
      return structuredClone(fallback)
    }
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : structuredClone(fallback)
    } catch {
      return structuredClone(fallback)
    }
  }
  // 页面文案：对象键（页面 → eyebrow/title/subtitle），字段缺失/损坏时归为空串（跟随回退）
  if (key === 'site_page_copy') {
    const empty = defaultSitePageCopy()
    if (typeof raw !== 'string' || !raw.trim()) return empty
    try {
      const parsed = JSON.parse(raw)
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return empty
      Object.keys(empty).forEach(pageKey => {
        const page = (parsed as Record<string, any>)[pageKey]
        if (page && typeof page === 'object') {
          empty[pageKey] = {
            eyebrow: typeof page.eyebrow === 'string' ? page.eyebrow : '',
            title: typeof page.title === 'string' ? page.title : '',
            subtitle: typeof page.subtitle === 'string' ? page.subtitle : ''
          }
        }
      })
      return empty
    } catch {
      return defaultSitePageCopy()
    }
  }
  if (raw === 'true') return true
  if (raw === 'false') return false
  return raw
}

async function fetchSettingRows(): Promise<any[]> {
  const res = await listSetting({ pageSize: 500, pageNum: 1 })
  return res.rows || res.data || []
}

/** 获取所有站点设置 */
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

    const merged: Record<string, any> = structuredClone(defaultSettings)
    Object.keys(loaded).forEach(key => {
      if ((managedKeySet.has(key) || sysConfigKeySet.has(key)) && loaded[key] !== undefined) {
        merged[key] = loaded[key]
      }
    })

    // 注册开关以 sys_config 的真实生效值为准：站点设置页其余项读 blog_setting，
    // 而 SiteAuthController / SysRegisterController 拦的是 sys.account.registerUser，
    // 只读本地镜像就会出现"卡片显示已开启、注册仍被拒"
    try {
      const res = await getRegistrationSwitch()
      merged[REGISTER_KEY] = !!res.data
    } catch {
      // 读不到就保留镜像值，不让整页回退到默认设置
    }

    settingsMap.value = structuredClone(merged)
    // 原始值保持未规范化：这样「表单里的相对路径」与「库里的绝对地址」会产生差异，
    // 用户点保存时顺带把历史脏值写回为相对路径（自愈），不必手工清理数据库
    originalSettings.value = toStoredMap(merged)

    // 品牌图历史值可能是后端返回的绝对地址（http://zhi-admin:8080/...，浏览器
    // 无法解析）：展示与提交统一收敛为站内相对路径
    settingsMap.value.site_logo = normalizeUploadUrl(settingsMap.value.site_logo)
    settingsMap.value.site_banner = normalizeUploadUrl(settingsMap.value.site_banner)
  } catch (error) {
    settingsMap.value = structuredClone(defaultSettings)
    originalSettings.value = toStoredMap(settingsMap.value)
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
      siteSettingsStore.updateSiteSettings({ [key]: settingsMap.value[key] })
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
    clearSiteCache().catch(() => {})
    const latest = toStoredMap(settingsMap.value)
    siteSettingsStore.updateSiteSettings(latest)
    window.dispatchEvent(new CustomEvent('siteSettingsUpdated', { detail: latest }))

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
  settingsStore.setTitle('站点设置')

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

.site-setting-card {
  margin-bottom: 20px;
  background-color: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
}

html.dark .site-setting-card {
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

.site-setting-tabs {
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

/* ---------- 官网内容编辑器 ---------- */
.content-tabs {
  margin-top: 12px;
}

.content-source-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.content-source-tip .el-icon {
  flex: none;
  margin-top: 3px;
  color: var(--el-color-primary);
}

.content-source-tip a {
  color: var(--el-color-primary);
  font-weight: 600;
}

.content-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.content-tabs :deep(.el-tab-pane > .content-section:first-child) {
  margin-top: 0;
}

.content-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.content-field__label {
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.content-field--grow {
  flex: 1;
  min-width: 0;
}

.content-ops {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 1px;
}

.content-empty {
  margin: 4px 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* ---------- 发展历程 ---------- */
.history-stage {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.history-stage + .history-stage {
  margin-top: 10px;
}

.history-stage__head {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.history-stage__fields {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 8px 12px;
}

.history-stage__fields > .content-field:nth-child(1) {
  grid-column: span 3;
}

.history-stage__fields > .content-field:nth-child(2) {
  grid-column: span 3;
}

.history-stage__fields > .content-field:nth-child(3) {
  grid-column: span 6;
}

.history-events {
  margin-top: 10px;
  padding-left: 12px;
  border-left: 2px solid var(--el-border-color-lighter);
}

.history-event {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 8px;
}

.history-event__year {
  width: 96px;
  flex: none;
}

.history-event__title {
  width: 180px;
  flex: none;
}

.history-event__honor {
  flex: none;
  margin-bottom: 2px;
}

/* ---------- 页面文案 ---------- */
.page-copy-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.page-copy-row + .page-copy-row {
  margin-top: 8px;
}

.page-copy-row__name {
  flex: none;
  width: 84px;
  padding-top: 22px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.page-copy-row__fields {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 10px;
}

.page-copy-row__fields > .content-field {
  flex: 1;
}

@media (max-width: 900px) {
  .history-stage__fields {
    grid-template-columns: 1fr;
  }

  .history-stage__fields > .content-field {
    grid-column: 1 / -1 !important;
  }

  .history-event,
  .page-copy-row,
  .page-copy-row__fields {
    flex-direction: column;
    align-items: stretch;
  }

  .history-event__year,
  .history-event__title,
  .page-copy-row__name {
    width: auto;
    padding-top: 0;
  }
}

/* 品牌图上传：控件与说明上下排布，说明文字限制宽度避免撑满整行 */
.brand-upload {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.brand-upload-tip {
  max-width: 460px;
  margin: 0;
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
  line-height: 1.6;
}
</style>
