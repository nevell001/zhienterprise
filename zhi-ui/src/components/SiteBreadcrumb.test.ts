import { describe, expect, it, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import SiteBreadcrumb from './SiteBreadcrumb.vue'

const stubs = {
  RouterLink: {
    props: ['to'],
    template: '<a class="stub-link" :data-to="to"><slot /></a>'
  }
}

const mountCrumb = (items: Array<{ label: string; to?: string }>) =>
  mount(SiteBreadcrumb, { props: { items }, global: { stubs } })

const readJsonLd = () => {
  const el = document.getElementById('zh-jsonld-breadcrumb')
  return el?.textContent ? JSON.parse(el.textContent) : null
}

describe('SiteBreadcrumb 组件', () => {
  beforeEach(() => {
    document.getElementById('zh-jsonld-breadcrumb')?.remove()
  })

  it('渲染完整链路：父级为链接，当前页无链接', () => {
    const wrapper = mountCrumb([
      { label: '首页', to: '/' },
      { label: '公司动态', to: '/category/3' },
      { label: '年度发布会' }
    ])

    const links = wrapper.findAll('.crumb-list .stub-link')
    expect(links.map(l => l.text())).toEqual(['首页', '公司动态'])
    expect(links.map(l => l.attributes('data-to'))).toEqual(['/', '/category/3'])
    expect(wrapper.find('.crumb-list .crumb-current').text()).toBe('年度发布会')
    expect(wrapper.find('.crumb-list a[aria-current]').exists()).toBe(false)
    // 移动端返回链接指向父级（桌面端由 CSS 隐藏）
    expect(wrapper.find('.crumb-back').attributes('data-to')).toBe('/category/3')
  })

  it('同步注入 BreadcrumbList：位置递增、父级绝对 URL、当前页取当前地址', () => {
    mountCrumb([
      { label: '首页', to: '/' },
      { label: '公司动态', to: '/category/3' },
      { label: '年度发布会' }
    ])

    const ld = readJsonLd()
    expect(ld['@type']).toBe('BreadcrumbList')
    expect(ld.itemListElement).toHaveLength(3)
    expect(ld.itemListElement[0]).toMatchObject({
      '@type': 'ListItem',
      position: 1,
      name: '首页',
      item: `${window.location.origin}/`
    })
    expect(ld.itemListElement[1].item).toBe(`${window.location.origin}/category/3`)
    expect(ld.itemListElement[2].item).toBe(window.location.href)
  })

  it('items 变化时 JSON-LD 跟随更新，卸载时移除', async () => {
    const wrapper = mountCrumb([{ label: '首页', to: '/' }, { label: '旧标题' }])
    expect(readJsonLd().itemListElement[1].name).toBe('旧标题')

    await wrapper.setProps({
      items: [{ label: '首页', to: '/' }, { label: '新标题' }]
    })
    expect(readJsonLd().itemListElement[1].name).toBe('新标题')

    wrapper.unmount()
    expect(readJsonLd()).toBeNull()
  })

  it('只有当前页一项时不渲染返回链接（移动端「返回」需要有父级）', () => {
    const wrapper = mountCrumb([{ label: '独立页' }])

    expect(wrapper.find('.crumb-back').exists()).toBe(false)
    expect(wrapper.find('.crumb-inline').text()).toBe('独立页')
    expect(readJsonLd().itemListElement).toHaveLength(1)
  })
})
