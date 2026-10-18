<template>
  <div class="app-container">
    <el-form
      v-show="showSearch"
      ref="queryRef"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="文章标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入文章标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select
          v-model="queryParams.categoryId"
          placeholder="请选择分类"
          clearable
          class="category-select"
        >
          <el-option
            v-for="category in categoryOptions"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="status-select"
        >
          <el-option label="草稿" value="0" />
          <el-option label="发布" value="1" />
          <el-option label="定时发布" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!fixedArticleType" label="类型" prop="articleType">
        <el-select
          v-model="queryParams.articleType"
          placeholder="全部类型"
          clearable
          class="status-select"
        >
          <el-option label="资讯动态" value="news" />
          <el-option label="核心业务" value="business" />
          <el-option label="客户案例" value="case" />
          <el-option label="解决方案" value="solution" />
        </el-select>
      </el-form-item>
      <el-form-item label="">
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <template v-if="!recycleMode">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate">
            修改
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">
            删除
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['zhi:article:edit']"
            type="warning"
            plain
            icon="Top"
            :disabled="multiple"
            @click="handleBatchTop"
          >
            批量置顶
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['zhi:article:edit']"
            type="warning"
            plain
            icon="Star"
            :disabled="multiple"
            @click="handleBatchRecommend"
          >
            批量推荐
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="info" plain icon="Delete" @click="toggleRecycleMode">回收站</el-button>
        </el-col>
      </template>
      <template v-else>
        <el-col :span="1.5">
          <el-button plain icon="ArrowLeft" @click="toggleRecycleMode">返回列表</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['zhi:article:edit']"
            type="success"
            plain
            icon="RefreshLeft"
            :disabled="multiple"
            @click="handleRestore"
          >
            恢复
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            v-hasPermi="['zhi:article:remove']"
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handlePurge"
          >
            彻底删除
          </el-button>
        </el-col>
      </template>
      <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
    </el-row>

    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文章ID" align="center" prop="id" width="60" />
      <el-table-column
        label="文章标题"
        align="left"
        prop="title"
        width="200"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <div class="article-title-wrapper">
            <el-tag
              v-if="scope.row.isTop === 1 || scope.row.isTop === '1'"
              type="danger"
              size="small"
              class="mr-1 article-top-tag"
            >
              置顶
            </el-tag>
            <el-tag
              v-if="scope.row.isRecommend === 1 || scope.row.isRecommend === '1'"
              type="warning"
              size="small"
              class="mr-1 article-recommend-tag"
            >
              推荐
            </el-tag>
            <span class="article-title-text">{{ scope.row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="categoryName" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.categoryName" type="info" size="small" class="category-tag">
            {{ scope.row.categoryName }}
          </el-tag>
          <span v-else class="text-muted">未分类</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="articleType" width="90">
        <template #default="scope">
          <el-tag :type="articleTypeTag(scope.row.articleType)" size="small">
            {{ articleTypeLabel(scope.row.articleType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="作者" align="center" prop="authorName" width="80" />

      <el-table-column label="标签" align="center" prop="tags" min-width="100">
        <template #default="scope">
          <el-tag
            v-for="(tag, index) in formatTagList(scope.row.tags)"
            :key="index"
            size="small"
            class="mr-1"
          >
            {{ tag }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag
            :type="
              scope.row.status === '1' || scope.row.status === 1
                ? 'success'
                : scope.row.status === '2' || scope.row.status === 2
                  ? 'info'
                  : 'warning'
            "
            effect="dark"
            class="article-status-tag"
          >
            {{
              scope.row.status === '1' || scope.row.status === 1
                ? '已发布'
                : scope.row.status === '2' || scope.row.status === 2
                  ? '定时发布'
                  : '草稿'
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="浏览量" align="center" prop="viewCount" width="70" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="100">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="recycleMode" label="删除时间" align="center" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        align="center"
        :width="recycleMode ? 200 : 280"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <template v-if="recycleMode">
            <el-button
              v-hasPermi="['zhi:article:edit']"
              link
              type="success"
              icon="RefreshLeft"
              @click="handleRestore(scope.row)"
            >
              恢复
            </el-button>
            <el-button
              v-hasPermi="['zhi:article:remove']"
              link
              type="danger"
              icon="Delete"
              @click="handlePurge(scope.row)"
            >
              彻底删除
            </el-button>
          </template>
          <template v-else>
            <el-button
              v-hasPermi="['zhi:article:edit']"
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
            >
              修改
            </el-button>
            <el-button
              v-hasPermi="['zhi:article:edit']"
              link
              type="danger"
              icon="Top"
              @click="handleTop(scope.row)"
            >
              {{ scope.row.isTop === 1 || scope.row.isTop === '1' ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button
              v-hasPermi="['zhi:article:edit']"
              link
              type="warning"
              icon="Star"
              @click="handleRecommend(scope.row)"
            >
              {{
                scope.row.isRecommend === 1 || scope.row.isRecommend === '1' ? '取消推荐' : '推荐'
              }}
            </el-button>
            <el-button
              v-hasPermi="['zhi:article:edit']"
              link
              type="info"
              icon="Refresh"
              @click="handleStatusChange(scope.row)"
            >
              {{ scope.row.status === 1 || scope.row.status === '1' ? '转为草稿' : '发布' }}
            </el-button>
            <el-button
              v-hasPermi="['zhi:article:remove']"
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加或修改文章对话框 -->
    <el-dialog
      v-model="open"
      :title="title"
      width="min(1320px, 96vw)"
      top="4vh"
      class="mo-editor-dialog"
      append-to-body
      @close="cancel"
    >
      <el-form
        ref="articleRef"
        :model="form"
        :rules="rules"
        class="mo-editor-form"
        label-position="top"
      >
        <div class="editor-topbar">
          <button type="button" class="back-btn" @click="cancel">← 返回</button>
          <el-form-item prop="title" class="title-field">
            <el-input v-model="form.title" class="title-input" placeholder="请输入文章标题" />
          </el-form-item>
          <span class="save-status" :class="`is-${editorSaveState}`">
            <span class="dot"></span>
            {{ editorSaveStatusText }}
          </span>
          <el-button plain size="small" @click="setEditorViewMode('preview')">预览</el-button>
          <el-button plain size="small" @click="openSitePreview">站点预览</el-button>
          <el-button plain size="small" @click="submitForm(0)">存为草稿</el-button>
          <el-button type="primary" size="small" @click="submitForm(1)">发布</el-button>
        </div>

        <div class="editor-workspace">
          <div class="editor-main">
            <div class="editor-toolbar">
              <button type="button" class="tool" title="加粗" @click="runEditorCommand('bold')">
                <b>B</b>
              </button>
              <button type="button" class="tool" title="斜体" @click="runEditorCommand('italic')">
                <i>I</i>
              </button>
              <button type="button" class="tool" title="删除线" @click="runEditorCommand('strike')">
                <s>S</s>
              </button>
              <span class="sep"></span>
              <button type="button" class="tool" title="一级标题" @click="setEditorHeader(1)">
                H1
              </button>
              <button type="button" class="tool" title="二级标题" @click="setEditorHeader(2)">
                H2
              </button>
              <button type="button" class="tool" title="三级标题" @click="setEditorHeader(3)">
                H3
              </button>
              <span class="sep"></span>
              <button
                type="button"
                class="tool"
                title="引用"
                @click="runEditorCommand('blockquote')"
              >
                ❝
              </button>
              <button
                type="button"
                class="tool"
                title="代码块"
                @click="runEditorCommand('code-block')"
              >
                &lt;/&gt;
              </button>
              <button type="button" class="tool" title="行内代码" @click="runEditorCommand('code')">
                { }
              </button>
              <button type="button" class="tool" title="链接" @click="insertEditorLink">🔗</button>
              <button type="button" class="tool" title="图片" @click="insertEditorImage">🖼</button>
              <button type="button" class="tool" title="表格" @click="insertEditorTable">▦</button>
              <span class="sep"></span>
              <button
                type="button"
                class="tool"
                title="有序列表"
                @click="runEditorCommand('list', 'ordered')"
              >
                1.
              </button>
              <button
                type="button"
                class="tool"
                title="无序列表"
                @click="runEditorCommand('list', 'bullet')"
              >
                •
              </button>
              <div class="view-modes">
                <button
                  type="button"
                  class="tool"
                  :class="{ active: editorViewMode === 'split' }"
                  @click="setEditorViewMode('split')"
                >
                  分屏
                </button>
                <button
                  type="button"
                  class="tool"
                  :class="{ active: editorViewMode === 'edit' }"
                  @click="setEditorViewMode('edit')"
                >
                  仅编辑
                </button>
                <button
                  type="button"
                  class="tool"
                  :class="{ active: editorViewMode === 'preview' }"
                  @click="setEditorViewMode('preview')"
                >
                  仅预览
                </button>
              </div>
            </div>

            <div class="editor-body" :class="`is-${editorViewMode}`">
              <section v-if="editorViewMode !== 'preview'" class="editor-pane edit-pane">
                <el-form-item v-if="isMarkdownMode" prop="contentMd" class="content-field">
                  <el-input
                    v-model="form.contentMd"
                    type="textarea"
                    :rows="20"
                    placeholder="在此编写 Markdown 内容…"
                    class="markdown-editor"
                  />
                </el-form-item>
                <el-form-item v-else prop="content" class="content-field">
                  <editor
                    :key="editorKey"
                    ref="richEditorRef"
                    v-model="form.content"
                    :min-height="420"
                  />
                </el-form-item>
              </section>
              <section v-if="editorViewMode !== 'edit'" class="editor-pane preview-pane">
                <div v-if="previewHtml" class="editor-preview" v-html="previewHtml"></div>
                <div v-else class="preview-empty">文章预览会显示在这里</div>
              </section>
            </div>
          </div>

          <aside class="editor-side-panel">
            <section class="side-section">
              <div class="side-title">发布状态</div>
              <el-form-item prop="status" class="compact-form-item">
                <el-radio-group v-model="form.status" class="status-radio">
                  <el-radio-button :label="1">公开</el-radio-button>
                  <el-radio-button :label="0">草稿</el-radio-button>
                  <el-radio-button :label="2">定时发布</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <div v-if="Number(form.status) === 2" class="publish-time-picker">
                <el-date-picker
                  v-model="form.publishTime"
                  type="datetime"
                  placeholder="选择自动发布时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                />
                <div class="schedule-tip">到达设定时间后将自动发布为公开文章</div>
              </div>
            </section>

            <section class="side-section">
              <div class="side-title">内容格式</div>
              <el-form-item prop="format" class="compact-form-item">
                <el-radio-group
                  v-model="form.format"
                  class="status-radio"
                  @change="handleFormatChange"
                >
                  <el-radio-button label="html">富文本</el-radio-button>
                  <el-radio-button label="markdown">Markdown</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <div v-if="isMarkdownMode" class="schedule-tip">
                Markdown 源码将同时保存，发布时自动渲染为 HTML
              </div>
            </section>

            <section class="side-section">
              <div class="side-title">内容类型</div>
              <el-form-item v-if="!fixedArticleType" prop="articleType" class="compact-form-item">
                <el-radio-group v-model="form.articleType" class="status-radio">
                  <el-radio-button label="news">资讯动态</el-radio-button>
                  <el-radio-button label="business">核心业务</el-radio-button>
                  <el-radio-button label="case">客户案例</el-radio-button>
                  <el-radio-button label="solution">解决方案</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <div v-else class="fixed-type-hint">
                <el-tag size="small" type="primary" effect="plain">
                  {{ articleTypeLabel(fixedArticleType) }}
                </el-tag>
                <span>
                  当前栏目固定为「{{ articleTypeLabel(fixedArticleType) }}」，发布后只出现在对应板块
                </span>
              </div>
              <div v-if="!fixedArticleType" class="schedule-tip">
                核心业务与客户案例展示在官网「核心业务」板块；解决方案展示在「解决方案」板块；资讯动态展示在文章列表
              </div>
            </section>

            <section v-if="form.articleType === 'solution'" class="side-section">
              <div class="side-title">方案筛选维度</div>
              <el-form-item prop="industry" class="compact-form-item">
                <el-select
                  v-model="form.industry"
                  placeholder="所属行业（可直接输入新行业）"
                  filterable
                  allow-create
                  default-first-option
                  clearable
                >
                  <el-option v-for="ind in industryOptions" :key="ind" :label="ind" :value="ind" />
                </el-select>
              </el-form-item>
              <el-form-item prop="scenario" class="compact-form-item">
                <el-input
                  v-model="form.scenario"
                  placeholder="能力场景，逗号分隔（如：数字化转型,降本增效）"
                  :maxlength="255"
                  show-word-limit
                />
              </el-form-item>
              <div class="schedule-tip">
                方案页按行业 Tab（精确匹配）与能力场景（多选标签）筛选，决定方案出现在哪些筛选下
              </div>
            </section>

            <section class="side-section">
              <div class="side-title">URL 别名</div>
              <el-form-item prop="slug" class="compact-form-item">
                <el-input
                  v-model="form.slug"
                  placeholder="留空使用数字 ID"
                  :maxlength="100"
                  show-word-limit
                />
              </el-form-item>
              <div class="schedule-tip">
                仅限字母、数字、下划线与连字符；设置后可用 /article/别名 访问，sitemap 优先输出别名
              </div>
            </section>

            <section class="side-section">
              <div class="side-title">分类</div>
              <el-form-item prop="categoryId" class="compact-form-item">
                <el-select v-model="form.categoryId" placeholder="请选择分类">
                  <el-option
                    v-for="category in categoryOptions"
                    :key="category.id"
                    :label="category.name"
                    :value="category.id"
                  />
                </el-select>
              </el-form-item>
            </section>

            <section class="side-section">
              <div class="side-title">标签</div>
              <el-form-item prop="tagIds" class="compact-form-item">
                <TagCategorySelector
                  v-model:selected-tags="form.tagIds"
                  v-model:selected-category="form.categoryId"
                  :show-category="false"
                  placeholder="选择或创建标签"
                />
              </el-form-item>
            </section>

            <section class="side-section">
              <div class="side-title">SEO（可选）</div>
              <el-form-item prop="seoTitle" class="compact-form-item">
                <el-input
                  v-model="form.seoTitle"
                  placeholder="SEO 标题，留空回退文章标题"
                  :maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item prop="seoKeywords" class="compact-form-item">
                <el-input
                  v-model="form.seoKeywords"
                  placeholder="SEO 关键词，英文逗号分隔"
                  :maxlength="255"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item prop="seoDescription" class="compact-form-item">
                <el-input
                  v-model="form.seoDescription"
                  type="textarea"
                  :rows="3"
                  :maxlength="500"
                  show-word-limit
                  placeholder="SEO 描述，留空回退文章摘要"
                />
              </el-form-item>
            </section>

            <!-- 业务详情结构化（business/case 专属；前台详情页分区渲染） -->
            <template v-if="isBizDetailType">
              <section class="side-section">
                <div class="side-title">卖点宫格（可选）</div>
                <div v-for="(p, pi) in sellingPointsList" :key="`sp${pi}`" class="cfg-row">
                  <el-input v-model="p.title" placeholder="卖点" style="width: 140px" />
                  <el-input v-model="p.desc" placeholder="一句话说明" style="flex: 1" />
                  <el-button type="danger" link @click="sellingPointsList.splice(pi, 1)">
                    删除
                  </el-button>
                </div>
                <el-button
                  link
                  type="primary"
                  @click="sellingPointsList.push({ title: '', desc: '' })"
                >
                  + 添加卖点
                </el-button>
              </section>

              <section class="side-section">
                <div class="side-title">规格参数表（可选）</div>
                <div v-for="(s, si) in specTableList" :key="`sp${si}`" class="cfg-row">
                  <el-input v-model="s.name" placeholder="参数名" style="width: 140px" />
                  <el-input v-model="s.value" placeholder="参数值" style="flex: 1" />
                  <el-button type="danger" link @click="specTableList.splice(si, 1)">
                    删除
                  </el-button>
                </div>
                <el-button link type="primary" @click="specTableList.push({ name: '', value: '' })">
                  + 添加参数
                </el-button>
              </section>

              <section class="side-section">
                <div class="side-title">图集（可选）</div>
                <div v-for="(g, gi) in galleryList" :key="`g${gi}`" class="cfg-row">
                  <el-input
                    :model-value="g"
                    placeholder="图片地址"
                    style="flex: 1"
                    @update:model-value="galleryList.splice(gi, 1, $event)"
                  />
                  <el-button type="danger" link @click="galleryList.splice(gi, 1)">删除</el-button>
                </div>
                <el-button link type="primary" @click="galleryList.push('')">+ 添加图片</el-button>
              </section>

              <section class="side-section">
                <div class="side-title">服务/实施流程（可选）</div>
                <div v-for="(s, fi) in processStepsList" :key="`f${fi}`" class="cfg-row">
                  <el-input v-model="s.title" placeholder="步骤名" style="width: 140px" />
                  <el-input v-model="s.desc" placeholder="步骤说明" style="flex: 1" />
                  <el-button type="danger" link @click="processStepsList.splice(fi, 1)">
                    删除
                  </el-button>
                </div>
                <el-button
                  link
                  type="primary"
                  @click="processStepsList.push({ title: '', desc: '' })"
                >
                  + 添加步骤
                </el-button>
              </section>
            </template>

            <section class="side-section">
              <div class="side-title">摘要</div>
              <el-form-item prop="summary" class="compact-form-item">
                <el-input
                  v-model="form.summary"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入摘要"
                />
              </el-form-item>
            </section>

            <section class="side-section">
              <div class="side-title">封面图片</div>
              <el-form-item prop="coverUrl" class="compact-form-item">
                <image-upload
                  v-model="form.coverUrl"
                  action="/common/upload/article-cover"
                  crop
                  :aspect-ratio="3"
                  :limit="1"
                />
                <div v-if="form.coverUrl" class="cover-preview">
                  <img :src="coverPreviewUrl" alt="封面预览" />
                  <el-button type="danger" size="small" plain @click="form.coverUrl = ''">
                    删除封面
                  </el-button>
                </div>
              </el-form-item>
            </section>

            <section class="side-section">
              <div class="side-title">推荐位</div>
              <div class="switch-row">
                <span>置顶</span>
                <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
              </div>
              <div class="switch-row">
                <span>推荐</span>
                <el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" />
              </div>
            </section>

            <section class="side-section">
              <div class="side-title">作者</div>
              <el-form-item prop="authorName" class="compact-form-item">
                <el-input
                  v-model="form.authorName"
                  :placeholder="form.id ? '文章原作者' : '自动填充为当前登录用户'"
                  readonly
                />
              </el-form-item>
            </section>

            <div class="side-actions">
              <span class="word-count">
                字数：{{ articleWordCount }} · 预计阅读：{{ articleReadMinutes }} 分钟
              </span>
              <div class="action-buttons">
                <el-button @click="cancel">取消</el-button>
                <el-button @click="submitForm(0)">存为草稿</el-button>
                <el-button type="primary" @click="submitForm(1)">发布文章</el-button>
              </div>
            </div>
          </aside>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Article">
import {
  ref,
  reactive,
  toRefs,
  getCurrentInstance,
  onMounted,
  nextTick,
  computed,
  watch
} from 'vue'
import { useUserStore } from '@/stores/user'
import {
  listArticle,
  getArticle,
  delArticle,
  restoreArticle,
  purgeArticle,
  addArticle,
  updateArticle,
  updateArticleStatus,
  updateArticleTopStatus,
  updateArticleRecommendStatus
} from '@/api/admin/article'
import { listCategory } from '@/api/admin/category'
import { listTag } from '@/api/admin/tag'
import ImageUpload from '@/components/ImageUpload'
import TagCategorySelector from '@/components/TagCategorySelector.vue'
import { parseTime } from '@/utils/zhi'
import { sanitizeArticleContent } from '@/utils/sanitize'
import { renderMarkdown } from '@/utils/markdown'
import { ElMessage, ElMessageBox } from '@/plugins/element-plus-service'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
// 核心业务/客户案例/解决方案管理菜单复用本组件，按路由固定 articleType
const fixedArticleType = route.path.endsWith('/business')
  ? 'business'
  : route.path.endsWith('/solution')
    ? 'solution'
    : route.path.endsWith('/case')
      ? 'case'
      : ''
/** 业务详情结构化（business/case）：编辑态数组，提交时序列化为 JSON */
const sellingPointsList = ref<{ title: string; desc: string }[]>([])
const specTableList = ref<{ name: string; value: string }[]>([])
const galleryList = ref<string[]>([])
const processStepsList = ref<{ title: string; desc: string }[]>([])
const isBizDetailType = computed(() =>
  ['business', 'case'].includes(String(form.value.articleType || ''))
)
// 行业预设与建表注释同源（制造业/金融/零售/医疗/教育/政务），可创建新行业
const industryOptions = ['制造业', '金融', '零售', '医疗', '教育', '政务']

function syncDetailListsFromForm() {
  function parseList<T>(raw: unknown): T[] {
    try {
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  sellingPointsList.value = parseList(form.value.sellingPoints)
  specTableList.value = parseList(form.value.specTable)
  galleryList.value = parseList(form.value.gallery)
  processStepsList.value = parseList(form.value.processSteps)
}
const articleTypeLabel = (type?: string) =>
  ({ news: '资讯动态', business: '核心业务', case: '客户案例', solution: '解决方案' })[
    type || 'news'
  ] || '资讯动态'
const articleTypeTag = (type?: string) =>
  ({ news: 'info', business: 'primary', case: 'warning', solution: 'success' })[type || 'news'] ??
  'info'
const userStore = useUserStore()
const articleList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const recycleMode = ref(false)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const categoryOptions = ref([])
const tagOptions = ref([])
// 编辑器组件的渲染计数器，用于强制重新渲染
const editorKey = ref(0)
const richEditorRef = ref<any>()
const editorViewMode = ref<'split' | 'edit' | 'preview'>('split')
const editorSaveState = ref<'idle' | 'dirty' | 'saving' | 'saved' | 'error'>('idle')

const data = reactive<Record<string, any>>({
  form: {} as Record<string, any>,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    categoryId: null,
    status: null,
    articleType: null
  },
  rules: {
    title: [
      { required: true, message: '文章标题不能为空', trigger: 'blur' },
      { min: 2, max: 200, message: '标题长度应为2-200个字符', trigger: 'blur' }
    ],
    content: [
      { required: true, message: '文章内容不能为空', trigger: 'blur' },
      { min: 10, message: '内容至少需要10个字符', trigger: 'blur' }
    ],
    summary: [{ max: 500, message: '摘要长度不能超过500个字符', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择文章分类', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)
const articlePlainText = computed(() => (form.value.content || '').replace(/<[^>]+>/g, '').trim())
const articleWordCount = computed(() => articlePlainText.value.length)
const articleReadMinutes = computed(() => Math.max(1, Math.ceil(articleWordCount.value / 500)))
const coverPreviewUrl = computed(() => getCoverUrl(form.value.coverUrl))
const editorSaveStatusText = computed(() => {
  const statusMap = {
    idle: '尚未保存',
    dirty: '有未保存修改',
    saving: '正在保存...',
    saved: form.value.status === 1 ? '已发布' : '草稿已保存',
    error: '保存失败'
  }
  return statusMap[editorSaveState.value]
})

watch(
  () => [form.value.title, form.value.summary, form.value.content, form.value.categoryId],
  () => {
    if (open.value && editorSaveState.value !== 'saving') {
      editorSaveState.value = 'dirty'
    }
  }
)

// 新增文章时，作者自动跟随当前登录用户（避免用户信息晚加载时留空）
watch(
  () => [userStore.nickName, userStore.name, userStore.userId] as const,
  ([nickName, name, userId]) => {
    if (!form.value.id) {
      if (!form.value.authorName) {
        form.value.authorName = nickName || name || ''
      }
      if (!form.value.authorId && userId) {
        form.value.authorId = userId
      }
    }
  },
  { immediate: true }
)

function setEditorViewMode(mode: 'split' | 'edit' | 'preview') {
  editorViewMode.value = mode
}

function runEditorCommand(command: string, value: string | number | boolean = true) {
  richEditorRef.value?.format?.(command, value)
}

function setEditorHeader(level: number) {
  runEditorCommand('header', level)
}

async function insertEditorLink() {
  try {
    const hrefResult = await ElMessageBox.prompt('请输入链接地址', '插入链接', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^https?:\/\/.+|^\/.+|^#.+/,
      inputErrorMessage: '请输入 http(s)、站内路径或锚点链接'
    })
    const href = hrefResult.value?.trim()
    if (!href) return

    const textResult = await ElMessageBox.prompt('请输入链接文本', '插入链接', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: href
    })
    richEditorRef.value?.insertLink?.(href, textResult.value?.trim() || href)
  } catch {
    // 用户取消输入时不做处理
  }
}

function insertEditorImage() {
  richEditorRef.value?.triggerUpload?.()
}

function insertEditorTable() {
  richEditorRef.value?.insertTable?.()
}

/** 查询文章列表 */
async function getList() {
  loading.value = true
  try {
    const response = await listArticle({
      ...queryParams.value,
      articleType: fixedArticleType || queryParams.value.articleType,
      delFlag: recycleMode.value ? 2 : 0
    })
    articleList.value = response.rows || []
    total.value = response.total || 0

    // 如果搜索结果为空，显示提示信息
    if (articleList.value.length === 0) {
      ElMessage.info(recycleMode.value ? '回收站是空的' : '未找到符合条件的文章')
    }
  } catch (error: any) {
    console.error('获取文章列表失败:', error)
    ElMessage.error('获取文章列表失败: ' + (error.message || '未知错误'))
    // 错误时也设置空数组，确保表格不会显示错误数据
    articleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 查询分类列表 */
async function getCategoryList() {
  try {
    const response = await listCategory()
    categoryOptions.value = response.rows || []
  } catch (error: any) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败: ' + (error.message || '未知错误'))
    categoryOptions.value = []
  }
}

/** 查询标签列表 */
async function getTagList() {
  try {
    const response = await listTag()
    tagOptions.value = response.rows || []
  } catch (error: any) {
    console.error('获取标签列表失败:', error)
    ElMessage.error('获取标签列表失败: ' + (error.message || '未知错误'))
    tagOptions.value = []
  }
}

// 取消按钮
function cancel() {
  reset()
  open.value = false
}

// 表单重置
function reset() {
  // 增加编辑器组件的渲染计数器，强制重新渲染
  editorKey.value++
  editorSaveState.value = 'idle'
  editorViewMode.value = 'split'

  form.value = {
    id: null,
    title: '',
    summary: '',
    content: '', // 直接使用空字符串
    contentMd: '', // Markdown 源码（format=markdown 时）
    format: 'html', // 内容格式 html|markdown
    articleType: fixedArticleType || 'news', // 内容类型：news 资讯 business 核心业务 case 案例 solution 解决方案
    industry: '', // 方案筛选维度：所属行业（solution）
    scenario: '', // 方案筛选维度：能力场景，逗号分隔（solution）
    seoTitle: '',
    seoKeywords: '',
    seoDescription: '',
    slug: '',
    coverUrl: '',
    categoryId: null,
    authorId: userStore.userId || null,
    authorName: userStore.nickName || userStore.name || '',
    status: 0, // 默认草稿状态
    publishTime: '', // 定时发布时间
    isTop: 0,
    isRecommend: 0,
    tagIds: [],
    viewCount: 0,
    likeCount: 0,
    commentCount: 0,
    createTime: null,
    updateTime: null,
    delFlag: 0
  }

  // 业务详情结构化列表同步清空
  syncDetailListsFromForm()

  // 清空表单验证状态
  if (articleRef.value) {
    articleRef.value.clearValidate()
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  // 直接重置查询参数
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    title: null,
    categoryId: null,
    status: null,
    articleType: null
  }
  getList()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  // 如果对话框已经打开，先关闭它
  if (open.value) {
    open.value = false
    // 等待对话框完全关闭后再重新打开
    setTimeout(() => {
      openDialog()
    }, 300)
  } else {
    openDialog()
  }
}

/** 打开对话框的辅助函数 */
function openDialog() {
  reset()
  // reset 函数已经增加了 editorKey，编辑器会重新渲染
  open.value = true
  editorSaveState.value = 'idle'
  title.value = '添加文章'
}

/** 修改按钮操作 */
async function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  try {
    loading.value = true
    const response = await getArticle(id)

    if (response.data) {
      // 深拷贝并确保数据类型正确
      const articleData = JSON.parse(JSON.stringify(response.data))

      // 确保数值字段是正确的类型
      if (articleData.categoryId !== null && articleData.categoryId !== undefined) {
        articleData.categoryId = Number(articleData.categoryId)
      }
      if (articleData.status !== null && articleData.status !== undefined) {
        articleData.status = Number(articleData.status)
      }
      if (articleData.isTop !== null && articleData.isTop !== undefined) {
        articleData.isTop = Number(articleData.isTop)
      }
      if (articleData.isRecommend !== null && articleData.isRecommend !== undefined) {
        articleData.isRecommend = Number(articleData.isRecommend)
      }

      // 确保tagIds是数组类型
      if (articleData.tagIds && !Array.isArray(articleData.tagIds)) {
        articleData.tagIds = [articleData.tagIds]
      } else if (!articleData.tagIds) {
        articleData.tagIds = []
      }

      // 确保字符串字段不为null
      articleData.title = articleData.title || ''
      articleData.summary = articleData.summary || ''
      articleData.content = articleData.content || ''
      articleData.contentMd = articleData.contentMd || ''
      articleData.format = articleData.format === 'markdown' ? 'markdown' : 'html'
      articleData.coverUrl = articleData.coverUrl || ''
      articleData.authorName = articleData.authorName || ''
      articleData.publishTime = articleData.publishTime
        ? String(articleData.publishTime).replace('T', ' ').slice(0, 19)
        : ''

      form.value = articleData
      syncDetailListsFromForm()
    } else {
      form.value = {}
      syncDetailListsFromForm()
    }

    open.value = true
    editorSaveState.value = 'saved'
    editorViewMode.value = 'split'
    title.value = '修改文章'
  } catch (error: any) {
    console.error('获取文章详情失败:', error)
    ElMessage.error('获取文章详情失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/** 格式化标签列表 */
function formatTagList(tags) {
  try {
    // 处理null或undefined情况
    if (!tags) return []

    // 如果是字符串，尝试解析JSON
    if (typeof tags === 'string') {
      try {
        const parsedTags = JSON.parse(tags)
        if (Array.isArray(parsedTags)) {
          return parsedTags.map(tag => {
            // 处理对象类型的标签
            if (typeof tag === 'object' && tag !== null) {
              return tag.name || tag.title || String(tag)
            }
            return String(tag)
          })
        }
        // 如果解析结果不是数组，返回字符串本身的数组
        return [tags]
      } catch (e) {
        // 如果JSON解析失败，检查是否为逗号分隔的字符串
        if (tags.includes(',')) {
          return tags.split(',').map(tag => tag.trim())
        }
        // 否则返回原字符串作为单个标签
        return [tags]
      }
    }

    // 如果已经是数组
    if (Array.isArray(tags)) {
      return tags.map(tag => {
        // 处理对象类型的标签
        if (typeof tag === 'object' && tag !== null) {
          return tag.name || tag.title || String(tag)
        }
        return String(tag)
      })
    }

    // 其他类型转为字符串数组
    return [String(tags)]
  } catch (error: any) {
    console.error('格式化标签列表失败:', error)
    return []
  }
}

// 提交按钮
const articleRef = ref<any>()

// Markdown/富文本相关
const isMarkdownMode = computed(() => form.value.format === 'markdown')
const previewHtml = computed(() => {
  // 后台预览与前台详情页用同一个消毒器：编辑器来源的内容同样可能带 onclick/javascript: 等
  if (isMarkdownMode.value) {
    return sanitizeArticleContent(renderMarkdown(form.value.contentMd))
  }
  return sanitizeArticleContent(form.value.content || '')
})

const handleFormatChange = (value: string) => {
  if (value === 'markdown' && !form.value.contentMd && form.value.content) {
    // 富文本已有内容时作为 Markdown 初始源码（可再编辑）
    form.value.contentMd = form.value.content
  }
  if (value === 'html' && !form.value.content && form.value.contentMd) {
    // 切回富文本时用当前 Markdown 生成 HTML
    form.value.content = renderMarkdown(form.value.contentMd)
  }
}
/** 站点预览：在官网真实布局中查看已保存内容（未发布也可查看），新标签打开不打断编辑 */
async function openSitePreview() {
  if (!form.value.id) {
    ElMessage.warning('请先保存草稿后再预览')
    return
  }
  if (editorSaveState.value === 'dirty') {
    try {
      await ElMessageBox.confirm(
        '当前有未保存的修改，预览将展示上次保存的内容。是否继续？',
        '站点预览',
        {
          confirmButtonText: '继续预览',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  const articleType = String(form.value.articleType || fixedArticleType)
  const routeName =
    articleType === 'business' || articleType === 'case'
      ? 'AdminBusinessPreview'
      : articleType === 'solution'
        ? 'AdminSolutionPreview'
        : 'AdminArticlePreview'
  const { href } = router.resolve({ name: routeName, params: { id: form.value.id } })
  window.open(href, '_blank')
}

const submitForm = async (targetStatus?: 0 | 1 | 2) => {
  if (!articleRef.value) return

  articleRef.value.validate(async valid => {
    if (valid) {
      // 定时发布前置校验
      const desiredStatus = targetStatus !== undefined ? targetStatus : Number(form.value.status)
      const scheduleTime = form.value.publishTime ? String(form.value.publishTime) : ''
      if (desiredStatus === 2) {
        if (!scheduleTime) {
          ElMessage.error('请选择定时发布时间')
          return
        }
        const pickTime = new Date(scheduleTime.replace(/-/g, '/')).getTime()
        if (!Number.isFinite(pickTime) || pickTime <= Date.now()) {
          ElMessage.error('定时发布时间必须晚于当前时间')
          return
        }
      } else {
        // 非定时发布时清除历史定时时间
        form.value.publishTime = ''
      }
      loading.value = true
      editorSaveState.value = 'saving'
      try {
        // 创建一个完整的数据对象，包含所有必需的字段
        const apiData = { ...form.value }
        if (targetStatus !== undefined) {
          apiData.status = targetStatus
          form.value.status = targetStatus
        }

        // 确保所有必需的字段都有值
        apiData.title = apiData.title?.trim() || ''
        apiData.summary = apiData.summary?.trim() || ''
        apiData.content = apiData.content || ''

        // Markdown：同时提交源码并由前端/服务端渲染 HTML 双写
        const submitFormat = apiData.format === 'markdown' ? 'markdown' : 'html'
        if (submitFormat === 'markdown') {
          apiData.contentMd = apiData.contentMd || ''
          if (!apiData.contentMd.trim()) {
            throw new Error('请填写 Markdown 内容')
          }
          apiData.content = renderMarkdown(apiData.contentMd)
        } else {
          apiData.contentMd = ''
        }
        apiData.format = submitFormat
        apiData.coverUrl = apiData.coverUrl || ''

        // 设置默认值，确保数据类型正确
        apiData.isTop = apiData.isTop ? 1 : 0
        apiData.isRecommend = apiData.isRecommend ? 1 : 0

        // 业务详情结构化（数组 → JSON 字符串；空数组存 '[]' 表示管理员已清空）
        const stringifyList = (list: unknown[]) => JSON.stringify(list ?? [])
        apiData.sellingPoints = stringifyList(sellingPointsList.value)
        apiData.specTable = stringifyList(specTableList.value)
        apiData.gallery = stringifyList(galleryList.value)
        apiData.processSteps = stringifyList(processStepsList.value)

        // 确保所有数值字段都是Number类型
        apiData.isTop = Number(apiData.isTop)
        apiData.isRecommend = Number(apiData.isRecommend)
        apiData.status = Number(apiData.status)

        // 确保分类ID类型正确
        if (apiData.categoryId !== null && apiData.categoryId !== undefined) {
          apiData.categoryId = Number(apiData.categoryId)
        }

        // 处理标签数据，确保格式正确
        if (apiData.tagIds && Array.isArray(apiData.tagIds)) {
          // 保留标签ID数组格式
        } else if (typeof apiData.tagIds === 'string') {
          // 如果是字符串，尝试分割为数组
          apiData.tagIds = apiData.tagIds
            .split(',')
            .map(id => Number(id.trim()))
            .filter(id => !isNaN(id))
        } else {
          apiData.tagIds = []
        }

        // 移除可能引起JSON解析问题的字段
        // 判断是否为新增操作：id 为 null 或空字符串
        const isNew = !form.value.id || form.value.id === ''

        if (isNew) {
          delete apiData.id // 新增时不需要id
        }

        delete apiData.createTime
        delete apiData.updateTime
        delete apiData.delFlag

        // 使用现有的API函数
        if (isNew) {
          await addArticle(apiData)
          ElMessage.success(
            apiData.status === 2
              ? '定时发布已保存'
              : apiData.status === 1
                ? '发布成功'
                : '草稿保存成功'
          )
        } else {
          await updateArticle(apiData)
          ElMessage.success(
            apiData.status === 2
              ? '定时发布已保存'
              : apiData.status === 1
                ? '发布成功'
                : '草稿保存成功'
          )
        }

        editorSaveState.value = 'saved'
        // 重置表单，清空所有字段
        reset()
        open.value = false
        await getList()
      } catch (error: any) {
        console.error('操作失败:', error)
        editorSaveState.value = 'error'
        // 显示友好的错误提示
        const errorMsg = error.response?.data?.msg || error.message || '操作失败，请重试'
        ElMessage.error(errorMsg)

        // 如果是401错误，处理登录过期
        if (error.response?.status === 401) {
          setTimeout(() => {
            window.location.href = '/index'
          }, 1500)
        }
      } finally {
        loading.value = false
      }
    }
  })
}

/** 删除按钮操作（逻辑删除，移入回收站） */
async function handleDelete(row) {
  const articleIds = row.id || ids.value

  try {
    await ElMessageBox.confirm(
      '是否确认将文章编号为"' + articleIds + '"的数据项移入回收站？可在回收站中恢复。',
      '删除确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    await delArticle(articleIds)
    ElMessage.success('已移入回收站')
    await getList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

/** 切换列表 / 回收站视图 */
function toggleRecycleMode() {
  recycleMode.value = !recycleMode.value
  queryParams.value.pageNum = 1
  ids.value = []
  single.value = true
  multiple.value = true
  getList()
}

/** 从回收站恢复 */
async function handleRestore(row) {
  const articleIds = row?.id ? [row.id] : ids.value
  if (!articleIds.length) {
    ElMessage.warning('请选择要恢复的文章')
    return
  }

  try {
    await ElMessageBox.confirm(
      `是否确认恢复选中的 ${articleIds.length} 篇文章？恢复后重新出现在列表中。`,
      '恢复确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true
    await restoreArticle(articleIds)
    ElMessage.success('恢复成功')
    ids.value = []
    await getList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('恢复失败:', error)
      // 标题被在线文章占用时后端会返回明确原因
      const errorMsg = error.response?.data?.msg || error.message || '恢复失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

/** 彻底删除（不可恢复） */
async function handlePurge(row) {
  const articleIds = row?.id ? [row.id] : ids.value
  if (!articleIds.length) {
    ElMessage.warning('请选择要彻底删除的文章')
    return
  }

  try {
    await ElMessageBox.confirm(
      `是否确认彻底删除选中的 ${articleIds.length} 篇文章？此操作不可恢复，标签关联也会一并清除。`,
      '彻底删除确认',
      {
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    await purgeArticle(articleIds)
    ElMessage.success('已彻底删除')
    ids.value = []
    await getList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('彻底删除失败:', error)
      ElMessage.error('彻底删除失败: ' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
  }
}

/** 状态切换按钮操作 */
async function handleStatusChange(row) {
  try {
    const newStatus = row.status === '1' || row.status === 1 ? 0 : 1
    const statusText = newStatus === 1 ? '发布' : '草稿'

    // 安全处理标题显示，防止title为null或undefined
    const articleTitle = row.title || '(无标题文章)'

    await ElMessageBox.confirm(
      `是否确认将文章《${articleTitle}》的状态切换为${statusText}？`,
      '状态切换确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true
    // 走专用状态接口：只改 status，不会触碰 format/contentMd/publishTime 等字段
    await updateArticleStatus([Number(row.id)], Number(newStatus))
    ElMessage.success('状态切换成功')
    await getList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('状态切换失败:', error)
      // 显示更友好的错误提示，优先使用后端返回的错误信息
      const errorMsg = error.response?.data?.msg || error.message || '状态切换失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

/** 置顶/取消置顶 */
async function handleTop(row) {
  try {
    const newIsTop = row.isTop === 1 || row.isTop === '1' ? 0 : 1
    const actionText = newIsTop === 1 ? '置顶' : '取消置顶'
    const articleTitle = row.title || '(无标题文章)'

    await ElMessageBox.confirm(`是否确认将文章《${articleTitle}》${actionText}？`, '置顶确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })

    loading.value = true
    await updateArticle({
      id: row.id,
      isTop: Number(newIsTop),
      title: row.title || '(无标题文章)',
      categoryId: row.categoryId,
      content: row.content,
      summary: row.summary,
      coverUrl: row.coverUrl,
      authorId: row.authorId,
      authorName: row.authorName,
      status: row.status,
      isRecommend: row.isRecommend,
      tagIds: row.tagIds
    })
    ElMessage.success(`${actionText}成功`)
    await getList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('置顶操作失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '置顶操作失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

/** 推荐/取消推荐 */
async function handleRecommend(row) {
  try {
    const newIsRecommend = row.isRecommend === 1 || row.isRecommend === '1' ? 0 : 1
    const actionText = newIsRecommend === 1 ? '推荐' : '取消推荐'
    const articleTitle = row.title || '(无标题文章)'

    await ElMessageBox.confirm(`是否确认将文章《${articleTitle}》${actionText}？`, '推荐确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })

    loading.value = true
    await updateArticle({
      id: row.id,
      isRecommend: Number(newIsRecommend),
      title: row.title || '(无标题文章)',
      categoryId: row.categoryId,
      content: row.content,
      summary: row.summary,
      coverUrl: row.coverUrl,
      authorId: row.authorId,
      authorName: row.authorName,
      status: row.status,
      isTop: row.isTop,
      tagIds: row.tagIds
    })
    ElMessage.success(`${actionText}成功`)
    await getList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('推荐操作失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '推荐操作失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

/** 批量置顶 */
async function handleBatchTop() {
  if (ids.value.length === 0) {
    ElMessage.warning('请选择要置顶的文章')
    return
  }

  try {
    await ElMessageBox.confirm(
      `是否确认将选中的 ${ids.value.length} 篇文章置顶？`,
      '批量置顶确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true
    // 使用批量更新接口
    await updateArticleTopStatus({
      ids: ids.value,
      isTop: 1
    })
    ElMessage.success('批量置顶成功')
    await getList()
    // 清空选择
    ids.value = []
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量置顶失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '批量置顶失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

/** 批量推荐 */
async function handleBatchRecommend() {
  if (ids.value.length === 0) {
    ElMessage.warning('请选择要推荐的文章')
    return
  }

  try {
    await ElMessageBox.confirm(
      `是否确认将选中的 ${ids.value.length} 篇文章推荐？`,
      '批量推荐确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true
    // 使用批量更新接口
    await updateArticleRecommendStatus({
      ids: ids.value,
      isRecommend: 1
    })
    ElMessage.success('批量推荐成功')
    await getList()
    // 清空选择
    ids.value = []
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量推荐失败:', error)
      const errorMsg = error.response?.data?.msg || error.message || '批量推荐失败'
      ElMessage.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

/** 获取封面图片URL */
function getCoverUrl(coverUrl) {
  if (!coverUrl) return ''
  // 检查是否为完整URL
  if (coverUrl.startsWith('http://') || coverUrl.startsWith('https://')) {
    return coverUrl
  }
  // 如果是相对路径，添加基础URL
  const baseUrl = import.meta.env?.VITE_APP_BASE_API || ''
  return baseUrl + coverUrl
}

// 页面加载时初始化数据
onMounted(async () => {
  // 并行加载所有初始化数据
  try {
    await Promise.all([getList(), getCategoryList(), getTagList()])
  } catch (error: any) {
    console.error('初始化数据加载失败:', error)
    // 单个请求失败不会阻止其他请求继续执行，这里只是记录错误
  }
})
</script>

<style scoped>
.mo-editor-form {
  overflow: hidden;
  margin: -20px;
  color: #44403c;
  background: #fff;
}

:global(.mo-editor-dialog .el-dialog__header) {
  display: none;
}

:global(.mo-editor-dialog .el-dialog__body) {
  padding: 0;
}

.editor-topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e7e5e4;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  color: #57534e;
  font-size: 14px;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.title-field {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}

.title-field :deep(.el-form-item__content) {
  line-height: 1;
}

.title-input :deep(.el-input__wrapper) {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.title-input :deep(.el-input__inner) {
  height: 38px;
  color: #1c1917;
  font-size: 18px;
  font-weight: 600;
}

.save-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #a8a29e;
  font-size: 12px;
  white-space: nowrap;
}

.save-status .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #16a34a;
}

.save-status.is-idle .dot {
  background: #a8a29e;
}

.save-status.is-dirty .dot {
  background: #f59e0b;
}

.save-status.is-saving .dot {
  background: var(--el-color-primary);
}

.save-status.is-error .dot {
  background: #dc2626;
}

.editor-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: min(760px, calc(92vh - 92px));
}

.editor-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  min-height: 42px;
  padding: 0 16px;
  overflow-x: auto;
  background: #fafaf9;
  border-bottom: 1px solid #e7e5e4;
}

.tool {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  color: #57534e;
  font: inherit;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 4px;
  transition: all 0.1s;
}

.tool:hover,
.tool.active {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 12%, transparent);
}

.sep {
  flex-shrink: 0;
  width: 1px;
  height: 20px;
  margin: 0 6px;
  background: #d6d3d1;
}

.view-modes {
  display: flex;
  flex-shrink: 0;
  gap: 2px;
  margin-left: auto;
}

.view-modes .tool {
  font-size: 12px;
  font-weight: 500;
}

.editor-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr);
  flex: 1;
  min-height: 0;
}

.editor-body.is-edit,
.editor-body.is-preview {
  grid-template-columns: 1fr;
}

.editor-pane {
  min-width: 0;
  padding: 16px 20px;
  overflow-y: auto;
}

.edit-pane {
  background: #fff;
  border-right: 1px solid #e7e5e4;
}

.preview-pane {
  background: #fafaf9;
}

.content-field {
  margin-bottom: 0;
}

.content-field :deep(.el-form-item__content) {
  display: block;
}

.editor-preview {
  color: #57534e;
  font-size: 14px;
  line-height: 1.8;
}

.editor-preview :deep(h2) {
  margin: 18px 0 10px;
  padding-bottom: 6px;
  color: #1c1917;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid #e7e5e4;
}

.editor-preview :deep(h3) {
  margin: 14px 0 8px;
  color: #292524;
  font-size: 16px;
  font-weight: 600;
}

.editor-preview :deep(p) {
  margin-bottom: 12px;
}

.editor-preview :deep(code) {
  padding: 2px 6px;
  color: var(--el-color-primary);
  font-size: 13px;
  background: #f5f5f4;
  border-radius: 4px;
}

.editor-preview :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 16px;
  color: #57534e;
  font-style: italic;
  background: #eef2ff;
  border-left: 3px solid #818cf8;
  border-radius: 0 8px 8px 0;
}

.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  color: #a8a29e;
  font-size: 14px;
}

.editor-side-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  max-height: min(760px, calc(92vh - 92px));
  padding: 16px;
  overflow-y: auto;
  background: #fcfcfb;
  border-left: 1px solid #e7e5e4;
}

.side-section {
  padding-bottom: 14px;
  border-bottom: 1px solid #e7e5e4;
}

.side-section:last-of-type {
  border-bottom: 0;
}

.side-title {
  display: block;
  margin-bottom: 8px;
  color: #57534e;
  font-size: 12px;
  font-weight: 600;
}

.compact-form-item {
  margin-bottom: 0;
}

.compact-form-item :deep(.el-select),
.compact-form-item :deep(.el-input),
.compact-form-item :deep(.el-textarea) {
  width: 100%;
}

.fixed-type-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.status-radio {
  width: 100%;
}

.status-radio :deep(.el-radio-button) {
  flex: 1;
}

.status-radio :deep(.el-radio-button__inner) {
  width: 100%;
}

.cover-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.cover-preview img {
  width: 104px;
  height: 64px;
  object-fit: cover;
  border: 1px solid #e7e5e4;
  border-radius: 8px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 30px;
  color: #57534e;
  font-size: 13px;
}

.side-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 0 0;
  margin-top: auto;
  background: linear-gradient(180deg, rgb(252 252 251 / 70%), #fcfcfb 18%);
  border-top: 1px solid #e7e5e4;
}

.word-count {
  color: #a8a29e;
  font-size: 12px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.side-actions .action-buttons :deep(.el-button) {
  margin-left: 0;
}

/* 文章状态标签样式优化 */
.article-status-tag {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 4px;
  font-weight: 500;
}

/* 分类标签样式优化 */
.category-tag {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 3px;
  font-weight: 500;
}

/* 置顶标签样式优化 */
.article-top-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 600;
}

/* 推荐标签样式优化 */
.article-recommend-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 600;
}

/* 未分类文本样式 */
.text-muted {
  color: var(--el-text-color-placeholder, #999);
  font-size: 12px;
}

/* 标签样式优化 */
.mr-1 {
  margin-right: 4px;
}

.ml-1 {
  margin-left: 4px;
}

.ml-2 {
  margin-left: 8px;
}

/* 文章标题包装器样式 */
.article-title-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  overflow: hidden;
}

.article-title-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  line-height: 1.4;
}

/* 响应式优化 */
@media (max-width: 1400px) {
  .article-title-text {
    font-size: 13px;
  }
}

@media (max-width: 1200px) {
  .article-title-text {
    font-size: 12px;
  }
}

@media (max-width: 992px) {
  .editor-workspace {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .editor-side-panel {
    max-height: none;
    border-top: 1px solid #e7e5e4;
    border-left: 0;
  }

  .side-actions {
    position: static;
  }

  .article-title-wrapper {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .article-title-text {
    font-size: 12px;
  }
}

/* 分类下拉选择框样式优化 */
.category-select {
  width: 150px;
}

:deep(.category-select .el-input__wrapper) {
  height: 32px;
  font-size: 13px;
}

:deep(.category-select .el-select__placeholder) {
  font-size: 13px;
}

:deep(.category-select .el-input__inner) {
  font-size: 13px;
}

/* 状态下拉选择框样式优化 */
.status-select {
  width: 120px;
}

:deep(.status-select .el-input__wrapper) {
  height: 32px;
  font-size: 13px;
}

:deep(.status-select .el-select__placeholder) {
  font-size: 13px;
}

:deep(.status-select .el-input__inner) {
  font-size: 13px;
}

/* 下拉选项样式优化 */
:deep(.el-select-dropdown__item) {
  font-size: 13px;
  padding: 8px 12px;
  line-height: 1.4;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .editor-topbar {
    gap: 8px;
    padding: 0 16px;
  }

  .save-status {
    display: none;
  }

  .editor-toolbar {
    padding: 0 12px;
  }

  .editor-body {
    grid-template-columns: 1fr;
  }

  .edit-pane {
    border-right: 0;
    border-bottom: 1px solid #e7e5e4;
  }

  .action-buttons {
    justify-content: flex-end;
  }

  .category-select {
    width: 120px;
  }

  .status-select {
    width: 100px;
  }

  :deep(.category-select .el-input__wrapper),
  :deep(.status-select .el-input__wrapper) {
    height: 30px;
    font-size: 12px;
  }

  :deep(.category-select .el-select__placeholder),
  :deep(.status-select .el-select__placeholder) {
    font-size: 12px;
  }

  :deep(.category-select .el-input__inner),
  :deep(.status-select .el-input__inner) {
    font-size: 12px;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 12px;
    padding: 6px 10px;
  }

  :deep(.el-tag) {
    font-size: 12px;
    padding: 3px 8px;
  }

  .article-status-tag {
    font-size: 12px;
    padding: 3px 8px;
  }

  .category-tag {
    font-size: 11px;
    padding: 2px 6px;
  }

  .article-top-tag,
  .article-recommend-tag {
    font-size: 10px;
    padding: 1px 4px;
  }

  .text-muted {
    font-size: 11px;
  }

  .ml-1,
  .ml-2 {
    margin-left: 2px;
  }
}

@media (max-width: 480px) {
  .category-select {
    width: 100px;
  }

  .status-select {
    width: 90px;
  }

  :deep(.category-select .el-input__wrapper),
  :deep(.status-select .el-input__wrapper) {
    height: 28px;
    font-size: 11px;
  }

  :deep(.category-select .el-select__placeholder),
  :deep(.status-select .el-select__placeholder) {
    font-size: 11px;
  }

  :deep(.category-select .el-input__inner),
  :deep(.status-select .el-input__inner) {
    font-size: 11px;
  }

  :deep(.el-select-dropdown__item) {
    font-size: 11px;
    padding: 5px 8px;
  }

  :deep(.el-tag) {
    font-size: 11px;
    padding: 2px 6px;
  }

  .article-status-tag {
    font-size: 11px;
    padding: 2px 6px;
  }

  .category-tag {
    font-size: 10px;
    padding: 2px 5px;
  }

  .article-top-tag,
  .article-recommend-tag {
    font-size: 9px;
    padding: 1px 3px;
  }

  .text-muted {
    font-size: 10px;
  }

  .ml-1,
  .ml-2 {
    margin-left: 1px;
  }
}

/* ===== 深色模式：编辑器作用域内映射 mo 中性色阶 =====
   为与各主题深色配色一致：
   - 默认主题（Tech Blue 深蓝）：将 --mo-n* 映射到 --el-* 变量
   - Mo-Blog 主题（中性深棕）：用棕色色阶反转（特异性更高，覆盖默认） */

html.dark .mo-editor-form {
  --mo-n0: var(--el-bg-color-overlay);
  --mo-n50: var(--el-bg-color);
  --mo-n100: var(--el-bg-color-overlay);
  --mo-n200: var(--el-border-color);
  --mo-n300: var(--el-text-color-regular);
  --mo-n400: var(--el-text-color-regular);
  --mo-n500: var(--el-text-color-regular);
  --mo-n600: var(--el-text-color-primary);
  --mo-n700: var(--el-text-color-primary);
  --mo-n800: var(--el-text-color-primary);
  --mo-n900: var(--el-text-color-primary);
  --mo-p50: color-mix(in srgb, var(--theme-primary) 16%, #141414);
  --mo-p100: color-mix(in srgb, var(--theme-primary) 24%, #141414);
  --mo-p200: color-mix(in srgb, var(--theme-primary) 34%, #141414);
  /* 强调色文字：p600 不参与深色重映射（浅色主题的深靛蓝在深色底上仅 ≈2:1），
     改为按主题主色即时混出的浅色强调（默认主题=浅青，Mo-Blog=浅靛） */
  --mo-p300: color-mix(in srgb, var(--el-color-primary) 45%, #ffffff);
  color: var(--mo-n700);
  background: var(--mo-n50);
}

html.dark.theme-mo-blog .mo-editor-form {
  --mo-n0: #292524;
  --mo-n50: #1c1917;
  --mo-n100: #292524;
  --mo-n200: #44403c;
  --mo-n300: #57534e;
  --mo-n400: #78716c;
  --mo-n500: #a8a29e;
  --mo-n600: #d6d3d1;
  --mo-n700: #e7e5e4;
  --mo-n800: #f5f5f4;
  --mo-n900: #fafaf9;
  --mo-p50: color-mix(in srgb, var(--theme-primary) 16%, #292524);
  --mo-p100: color-mix(in srgb, var(--theme-primary) 24%, #292524);
  --mo-p200: color-mix(in srgb, var(--theme-primary) 34%, #292524);
  --mo-p300: color-mix(in srgb, var(--el-color-primary) 45%, #ffffff);
}

/* 顶栏 / 工具栏 / 编辑区 / 预览区 / 侧面板背景 */
html.dark .mo-editor-form .editor-topbar,
html.dark .mo-editor-form .editor-main,
html.dark .mo-editor-form .edit-pane {
  background: var(--mo-n0);
  border-color: var(--mo-n200);
}

html.dark .mo-editor-form .editor-topbar {
  border-bottom-color: var(--mo-n200);
}

html.dark .mo-editor-form .editor-toolbar {
  background: var(--mo-n50);
  border-bottom-color: var(--mo-n200);
}

/* 编辑主体（分屏/纯编辑/纯预览）容器：旧 mo-blog 深色块用 n800（浅色）
   当背景，会在这里露出一块浅底，这里统一覆盖为深色。 */
html.dark .mo-editor-form .editor-body {
  background: var(--mo-n50);
}

html.dark .mo-editor-form .edit-pane {
  border-right-color: var(--mo-n200);
}

html.dark .mo-editor-form .preview-pane {
  background: var(--mo-n50);
}

html.dark .mo-editor-form .editor-side-panel {
  background: var(--mo-n0);
  border-left-color: var(--mo-n200);
}

html.dark .mo-editor-form .side-section {
  border-bottom-color: var(--mo-n200);
}

/* 工具栏按钮 */
html.dark .mo-editor-form .tool {
  color: var(--mo-n500);
}

html.dark .mo-editor-form .tool:hover,
html.dark .mo-editor-form .tool.active {
  color: var(--mo-p300);
  background: var(--mo-p50);
}

/* 分隔线 */
html.dark .mo-editor-form .sep {
  background: var(--mo-n200);
}

/* 标题输入 */
html.dark .mo-editor-form .title-input :deep(.el-input__inner) {
  color: var(--mo-n900);
}

/* 返回按钮 / 侧边标题 / 开关行 */
html.dark .mo-editor-form .back-btn,
html.dark .mo-editor-form .side-title,
html.dark .mo-editor-form .switch-row {
  color: var(--mo-n500);
}

/* 预览区文字与标题 */
html.dark .mo-editor-form .editor-preview {
  color: var(--mo-n500);
}

html.dark .mo-editor-form .editor-preview :deep(h2) {
  color: var(--mo-n900);
  border-bottom-color: var(--mo-n200);
}

html.dark .mo-editor-form .editor-preview :deep(h3) {
  color: var(--mo-n800);
}

/* 行内代码 */
html.dark .mo-editor-form .editor-preview :deep(code) {
  color: var(--mo-p300);
  background: var(--mo-n100);
}

/* 引用块 */
html.dark .mo-editor-form .editor-preview :deep(blockquote) {
  color: var(--mo-n500);
  background: var(--mo-p50);
  border-left-color: var(--mo-p400);
}

/* 空预览提示 */
html.dark .mo-editor-form .preview-empty {
  /* n400 在 Mo-Blog 深色作用域里是深灰 #78716c（3.65:1），改用 n500 */
  color: var(--mo-n500);
}

/* 封面预览边框 */
html.dark .mo-editor-form .cover-preview img {
  border-color: var(--mo-n200);
}

/* 底部操作栏渐变（用纯色替代浅色渐变） */
html.dark .mo-editor-form .side-actions {
  background: var(--mo-n0);
  border-top-color: var(--mo-n200);
}

/* 字数统计 / 保存状态 */
html.dark .mo-editor-form .word-count,
html.dark .mo-editor-form .save-status {
  color: var(--mo-n500);
}

/* ===== Element Plus 表单控件深色模式适配 ===== */

/* 输入框、文本域、选择器 */
html.dark .mo-editor-form :deep(.el-input__wrapper),
html.dark .mo-editor-form :deep(.el-textarea__inner) {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
  box-shadow: 0 0 0 1px var(--mo-n200) inset;
}

html.dark .mo-editor-form :deep(.el-input__wrapper.is-focus),
html.dark .mo-editor-form :deep(.el-textarea__inner:focus) {
  border-color: var(--mo-p600);
  box-shadow: 0 0 0 1px var(--mo-p600) inset;
}

html.dark .mo-editor-form :deep(.el-input__inner),
html.dark .mo-editor-form :deep(.el-textarea__inner) {
  color: var(--mo-n700);
}

html.dark .mo-editor-form :deep(.el-input__inner::placeholder),
html.dark .mo-editor-form :deep(.el-textarea__inner::placeholder) {
  color: var(--mo-n400);
}

/* 选择器下拉项 */
html.dark .mo-editor-form :deep(.el-select-dropdown__item) {
  color: var(--mo-n600);
}

html.dark .mo-editor-form :deep(.el-select-dropdown__item:hover) {
  background: var(--mo-p50);
}

html.dark .mo-editor-form :deep(.el-select-dropdown__item.selected) {
  color: var(--mo-p300);
  background: var(--mo-p50);
}

/* 单选按钮组 */
html.dark .mo-editor-form :deep(.el-radio-button__inner) {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
  color: var(--mo-n600);
}

html.dark
  .mo-editor-form
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--mo-p600);
  border-color: var(--mo-p600);
  /* 固定靛蓝填充（p600 不参与深色重映射），前景只能是白色，不能用 --mo-on-primary */
  color: #fff;
}

html.dark
  .mo-editor-form
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner:hover) {
  background: var(--mo-p700);
  border-color: var(--mo-p700);
}

html.dark .mo-editor-form :deep(.el-radio-button__inner:hover) {
  color: var(--mo-p300);
  border-color: var(--mo-n300);
}

/* 开关 */
html.dark .mo-editor-form :deep(.el-switch__core) {
  background: var(--mo-n400);
  border-color: var(--mo-n400);
}

html.dark .mo-editor-form :deep(.el-switch.is-checked .el-switch__core) {
  background: var(--mo-p600);
  border-color: var(--mo-p600);
}

/* 按钮悬停 */
html.dark .mo-editor-form :deep(.el-button:hover) {
  border-color: var(--mo-p600);
}

html.dark .mo-editor-form :deep(.el-button--primary:hover) {
  background: var(--mo-p700);
  border-color: var(--mo-p700);
}

/* 富文本编辑器区域 */
html.dark .mo-editor-form :deep(.editor) {
  background: var(--mo-n50);
}

html.dark .mo-editor-form :deep(.ql-toolbar) {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark .mo-editor-form :deep(.ql-container) {
  background: var(--mo-n50);
  border-color: var(--mo-n200);
}

html.dark .mo-editor-form :deep(.ql-editor) {
  color: var(--mo-n600);
  background: var(--mo-n50);
}

html.dark .mo-editor-form :deep(.ql-editor.ql-blank::before) {
  color: var(--mo-n400);
}

html.dark .mo-editor-form :deep(.ql-stroke) {
  stroke: var(--mo-n500);
}

html.dark .mo-editor-form :deep(.ql-fill) {
  fill: var(--mo-n500);
}

html.dark .mo-editor-form :deep(.ql-picker-item:hover) {
  color: var(--mo-p300);
}

html.dark .mo-editor-form :deep(.ql-picker-label:hover) {
  color: var(--mo-p300);
}

html.dark .mo-editor-form :deep(.ql-active) {
  color: var(--mo-p300) !important;
}

html.dark .mo-editor-form :deep(.ql-selected) {
  color: var(--mo-p300) !important;
}
/* 定时发布时间选择 */
.publish-time-picker {
  margin-top: 8px;
}
.schedule-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--mo-n500);
}
</style>
