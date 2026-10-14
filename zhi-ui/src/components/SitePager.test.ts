import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SitePager from './SitePager.vue'

const stubs = {
  'el-pagination': {
    name: 'ElPagination',
    props: ['total', 'pageSize', 'currentPage'],
    template: '<nav class="stub-pager" :data-total="total" :data-page-size="pageSize" />'
  }
}

const mountPager = (props: { total: number; pageSize: number; pageNum: number }) =>
  mount(SitePager, { props, global: { stubs } })

describe('SitePager 组件', () => {
  it('总数超过单页容量时才渲染分页器', () => {
    expect(mountPager({ total: 30, pageSize: 10, pageNum: 1 }).find('.stub-pager').exists()).toBe(
      true
    )
    expect(mountPager({ total: 10, pageSize: 10, pageNum: 1 }).find('.stub-pager').exists()).toBe(
      false
    )
  })

  it('应该把总数与单页容量透传给分页器', () => {
    const pager = mountPager({ total: 30, pageSize: 10, pageNum: 2 }).find('.stub-pager')

    expect(pager.attributes('data-total')).toBe('30')
    expect(pager.attributes('data-page-size')).toBe('10')
  })

  it('翻页时应该向外抛出 page-change 事件', async () => {
    const wrapper = mountPager({ total: 30, pageSize: 10, pageNum: 1 })

    await wrapper.findComponent({ name: 'ElPagination' }).vm.$emit('current-change', 3)

    expect(wrapper.emitted('page-change')).toEqual([[3]])
  })
})
