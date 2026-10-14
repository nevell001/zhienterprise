import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ArticleCard from './ArticleCard.vue'

const stubs = {
  RouterLink: {
    props: ['to', 'title'],
    template: '<a class="stub-link" :data-to="to"><slot /></a>'
  },
  'el-icon': { template: '<span class="stub-icon"><slot /></span>' }
}

const mountCard = (article: Record<string, unknown>, index = 0) =>
  mount(ArticleCard, { props: { article: article as never, index }, global: { stubs } })

describe('ArticleCard 组件', () => {
  it('应该渲染标题、元信息、摘要与文章标签', () => {
    const wrapper = mountCard({
      id: 7,
      title: '深色模式令牌契约',
      coverUrl: '/cover.png',
      categoryName: '前端工程',
      createTime: '2026-10-01 10:00:00',
      viewCount: 12,
      likeCount: 3,
      commentCount: 1,
      summary: '一段摘要',
      tags: [
        { id: 1, name: 'Vue' },
        { id: 2, name: 'CSS' },
        { id: 3, name: '主题' },
        { id: 4, name: '不应出现' }
      ]
    })

    expect(wrapper.text()).toContain('深色模式令牌契约')
    expect(wrapper.text()).toContain('12 阅读')
    expect(wrapper.text()).toContain('一段摘要')
    expect(wrapper.text()).toContain('前端工程')
    expect(wrapper.find('img').attributes('src')).toBe('/cover.png')

    const chips = wrapper.findAll('.article-card-tag')
    expect(chips).toHaveLength(3)
    expect(chips.map(c => c.text())).not.toContain('不应出现')
  })

  it('缺封面时不渲染封面与分类角标', () => {
    const wrapper = mountCard({ id: 1, title: '无封面', summary: 's' })

    expect(wrapper.find('.article-card-cover').exists()).toBe(false)
    expect(wrapper.find('.article-card-badge').exists()).toBe(false)
  })

  it('置顶/推荐文章渲染对应徽标，普通文章不渲染', () => {
    const flagged = mountCard({ id: 2, title: '旗标文章', isTop: 1, isRecommend: 1 })
    expect(flagged.find('.article-card-flags').exists()).toBe(true)
    expect(flagged.find('.card-flag.flag-top').text()).toBe('置顶')
    expect(flagged.find('.card-flag.flag-rec').text()).toBe('推荐')

    const plain = mountCard({ id: 3, title: '普通文章', isTop: 0, isRecommend: 0 })
    expect(plain.find('.article-card-flags').exists()).toBe(false)
  })

  it('没有摘要时应该用去掉 HTML 标签的正文兜底', () => {
    const wrapper = mountCard({
      id: 2,
      title: '兜底摘要',
      content: '<p>正文<strong>加粗</strong></p>'
    })

    const summary = wrapper.find('.article-card-summary').text()
    expect(summary).toContain('正文加粗')
    expect(summary).not.toContain('<p>')
    expect(summary).not.toContain('<strong>')
  })

  it('文章详情链接应该指向文章 id', () => {
    const wrapper = mountCard({ id: 42, title: 't', summary: 's' })

    expect(wrapper.find('.article-card-more').attributes('data-to')).toBe('/article/42')
  })

  it('入场动画延迟应该跟随索引', () => {
    const wrapper = mountCard({ id: 1, title: 't', summary: 's' }, 5)

    expect(wrapper.find('.article-card').attributes('style')).toContain('animation-delay: 0.5s')
  })

  /**
   * 跨文件 CSS 契约：封面展示容器必须与上传裁剪比例（3:1）一致。
   * jsdom 无布局引擎无法行为验证；若容器比例偏离裁剪比例，center/cover 会对
   * 裁剪结果二次裁切，用户「裁什么」与「首页显示什么」不一致（参考 2026-10-04 首页横条问题）。
   */
  it('封面容器保持 3:1，与上传裁剪比例一致（跨文件 CSS 契约）', () => {
    const vue = readFileSync(resolve(process.cwd(), 'src/components/ArticleCard.vue'), 'utf-8')
    const coverRule = vue.match(/\.article-card-cover\s*\{[^}]*\}/)?.[0] ?? ''
    expect(coverRule).toMatch(/aspect-ratio:\s*3\s*\/\s*1/)
    expect(coverRule).not.toMatch(/height:\s*\d+px/)
    // 响应式断点里不得残留其他比例（回归过 480px 断点残留 16:9 的问题）
    const vueRatios = vue.match(/aspect-ratio:\s*[^;]+/g) ?? []
    expect(vueRatios.every(r => /3\s*\/\s*1/.test(r))).toBe(true)

    const home = readFileSync(resolve(process.cwd(), 'src/views/blog/index.vue'), 'utf-8')
    const thumbRule = home.match(/\.article-card \.thumb\s*\{[^}]*\}/)?.[0] ?? ''
    expect(thumbRule).toMatch(/aspect-ratio:\s*3\s*\/\s*1/)
    expect(thumbRule).not.toMatch(/height:\s*\d+px/)

    const archive = readFileSync(
      resolve(process.cwd(), 'src/views/blog/archive/index.vue'),
      'utf-8'
    )
    const archiveRule = archive.match(/\.article-cover\s*\{[^}]*\}/)?.[0] ?? ''
    expect(archiveRule).toMatch(/aspect-ratio:\s*3\s*\/\s*1/)
  })
})
